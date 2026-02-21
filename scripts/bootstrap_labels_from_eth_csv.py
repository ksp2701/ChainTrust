"""
Bootstrap labeled outcomes from eth_addresses.csv via backend APIs.

Maps:
- Label=Legit -> outcome=REPAID
- Label=Dodgy -> outcome=DEFAULTED

To reduce noise:
- Drops invalid addresses
- Drops duplicate addresses with conflicting labels
- Supports account-type filtering
- Samples balanced classes
"""

from __future__ import annotations

import argparse
import csv
import json
import random
import re
import time
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


ADDRESS_RE = re.compile(r"^0x[a-fA-F0-9]{40}$")


@dataclass(frozen=True)
class CsvRow:
    address: str
    label: str
    account_type: str


def http_json(method: str, url: str, payload: dict[str, Any] | None, timeout: int = 60) -> Any:
    body = None
    headers = {"Content-Type": "application/json"}
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
    req = Request(url=url, method=method, data=body, headers=headers)
    with urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def load_rows(csv_path: Path, account_type_filter: str) -> list[CsvRow]:
    rows: dict[str, set[str]] = defaultdict(set)
    account_by_addr: dict[str, str] = {}

    with csv_path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for raw in reader:
            address = (raw.get("Address") or "").strip()
            label = (raw.get("Label") or "").strip()
            account_type = (raw.get("Account Type") or "").strip()

            if not ADDRESS_RE.match(address):
                continue
            if label not in {"Legit", "Dodgy"}:
                continue
            if account_type_filter != "all" and account_type != account_type_filter:
                continue

            addr_lower = address.lower()
            rows[addr_lower].add(label)
            account_by_addr[addr_lower] = account_type

    out: list[CsvRow] = []
    for addr, labels in rows.items():
        if len(labels) != 1:
            # conflicting label for same address; skip to avoid noisy supervision
            continue
        label = next(iter(labels))
        out.append(CsvRow(address=addr, label=label, account_type=account_by_addr.get(addr, "")))
    return out


def get_dataset_state(backend_url: str, timeout: int = 60) -> tuple[Counter, set[str]]:
    rows = http_json("GET", backend_url.rstrip("/") + "/loan/training-data", None, timeout)
    counts = Counter()
    seen_addresses: set[str] = set()
    for r in rows:
        address = str(r.get("wallet_address") or "").strip().lower()
        if ADDRESS_RE.match(address):
            seen_addresses.add(address)
        lbl = r.get("label")
        if lbl == 1:
            counts["REPAID"] += 1
        elif lbl == 0:
            counts["DEFAULTED"] += 1
    return counts, seen_addresses


def post_evaluate(backend_url: str, address: str, amount: float, timeout: int = 60) -> dict[str, Any]:
    payload = {
        "walletAddress": address,
        "amount": amount,
        "collateralToken": "ETH",
        "collateralAmount": 3000,
        "termDays": 30,
        "purpose": "csv_bootstrap",
    }
    return http_json("POST", backend_url.rstrip("/") + "/loan/evaluate", payload, timeout)


def post_outcome(backend_url: str, decision_hash: str, outcome: str, timeout: int = 60) -> dict[str, Any]:
    payload = {"decisionHash": decision_hash, "outcome": outcome}
    return http_json("POST", backend_url.rstrip("/") + "/loan/outcome", payload, timeout)


def run(args: argparse.Namespace) -> int:
    rng = random.Random(args.seed)
    counts, seen_addresses = get_dataset_state(args.backend_url, args.timeout)
    print(f"Current counts: REPAID={counts['REPAID']} DEFAULTED={counts['DEFAULTED']}")

    repaid_needed = max(0, args.target_per_class - counts["REPAID"])
    default_needed = max(0, args.target_per_class - counts["DEFAULTED"])
    print(f"Need: REPAID={repaid_needed} DEFAULTED={default_needed}")
    if repaid_needed == 0 and default_needed == 0:
        print("Target already satisfied; nothing to do.")
        return 0

    rows = load_rows(Path(args.csv), args.account_type)
    if not args.include_seen_addresses:
        before = len(rows)
        rows = [r for r in rows if r.address not in seen_addresses]
        print(f"Filtered {before - len(rows)} addresses already labeled in backend history")

    legit = [r for r in rows if r.label == "Legit"]
    dodgy = [r for r in rows if r.label == "Dodgy"]
    rng.shuffle(legit)
    rng.shuffle(dodgy)

    if len(legit) < repaid_needed or len(dodgy) < default_needed:
        print(
            f"Not enough rows after filtering. legit={len(legit)} dodgy={len(dodgy)} "
            f"need_repaid={repaid_needed} need_defaulted={default_needed}"
        )
        return 1

    success = 0
    failures = 0

    for i in range(repaid_needed):
        row = legit[i]
        amount = float(args.repaid_amount_base + i)
        try:
            eval_resp = post_evaluate(args.backend_url, row.address, amount, args.timeout)
            decision_hash = eval_resp["decisionHash"]
            post_outcome(args.backend_url, decision_hash, "REPAID", args.timeout)
            success += 1
            print(f"REPAID {i+1}/{repaid_needed} addr={row.address} hash={decision_hash}")
        except (HTTPError, URLError, TimeoutError, KeyError, ValueError) as exc:
            failures += 1
            print(f"REPAID failed addr={row.address}: {exc}")
        if args.sleep_ms > 0:
            time.sleep(args.sleep_ms / 1000.0)

    for i in range(default_needed):
        row = dodgy[i]
        amount = float(args.default_amount_base + i)
        try:
            eval_resp = post_evaluate(args.backend_url, row.address, amount, args.timeout)
            decision_hash = eval_resp["decisionHash"]
            post_outcome(args.backend_url, decision_hash, "DEFAULTED", args.timeout)
            success += 1
            print(f"DEFAULTED {i+1}/{default_needed} addr={row.address} hash={decision_hash}")
        except (HTTPError, URLError, TimeoutError, KeyError, ValueError) as exc:
            failures += 1
            print(f"DEFAULTED failed addr={row.address}: {exc}")
        if args.sleep_ms > 0:
            time.sleep(args.sleep_ms / 1000.0)

    counts, _ = get_dataset_state(args.backend_url, args.timeout)
    print(
        f"Done. success={success} failures={failures} "
        f"final_counts: REPAID={counts['REPAID']} DEFAULTED={counts['DEFAULTED']}"
    )
    return 0


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Bootstrap backend outcomes from eth_addresses.csv")
    p.add_argument("--csv", default="eth_addresses.csv")
    p.add_argument("--backend-url", default="http://localhost:8080")
    p.add_argument("--target-per-class", type=int, default=120)
    p.add_argument("--account-type", choices=["all", "Wallet", "Smart Contract"], default="all")
    p.add_argument(
        "--include-seen-addresses",
        action="store_true",
        help="Allow addresses already present in /loan/training-data (disabled by default).",
    )
    p.add_argument("--repaid-amount-base", type=int, default=10000)
    p.add_argument("--default-amount-base", type=int, default=30000)
    p.add_argument("--sleep-ms", type=int, default=50)
    p.add_argument("--timeout", type=int, default=60)
    p.add_argument("--seed", type=int, default=42)
    return p.parse_args()


if __name__ == "__main__":
    raise SystemExit(run(parse_args()))
