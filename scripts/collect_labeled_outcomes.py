"""
Collects labeled loan outcomes via backend APIs until each class reaches a target.

This utility is intended for bootstrapping and repeatable local retraining workflow.
It drives:
1) POST /loan/evaluate
2) POST /loan/outcome
3) GET  /loan/training-data

Usage:
  python scripts/collect_labeled_outcomes.py --backend-url http://localhost:8080 --target-per-class 10
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from dataclasses import dataclass
from typing import Any, Dict, List
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


@dataclass
class LoanScenario:
    wallet: str
    amount: float
    collateral_token: str = "ETH"
    collateral_amount: float = 3000.0
    term_days: int = 30
    purpose: str = "ops"

    def to_eval_payload(self) -> Dict[str, Any]:
        return {
            "walletAddress": self.wallet,
            "amount": self.amount,
            "collateralToken": self.collateral_token,
            "collateralAmount": self.collateral_amount,
            "termDays": self.term_days,
            "purpose": self.purpose,
        }


def http_json(method: str, url: str, payload: Dict[str, Any] | None, timeout: int) -> Any:
    data = None
    headers = {"Content-Type": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
    req = Request(url=url, method=method, data=data, headers=headers)
    with urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def get_label_counts(backend_url: str, timeout: int) -> Counter:
    rows = http_json("GET", backend_url.rstrip("/") + "/loan/training-data", None, timeout)
    counts = Counter()
    for row in rows:
        label = row.get("label")
        if label == 1:
            counts["REPAID"] += 1
        elif label == 0:
            counts["DEFAULTED"] += 1
    return counts


def normalize_decision_hash(decision_hash: str) -> str:
    h = decision_hash.lower()
    if h.startswith("0x"):
        h = h[2:]
    return h


def label_outcome(backend_url: str, decision_hash: str, outcome: str, timeout: int) -> Dict[str, Any]:
    payload = {
        "decisionHash": normalize_decision_hash(decision_hash),
        "outcome": outcome,
    }
    return http_json("POST", backend_url.rstrip("/") + "/loan/outcome", payload, timeout)


def evaluate(backend_url: str, scenario: LoanScenario, timeout: int) -> Dict[str, Any]:
    return http_json("POST", backend_url.rstrip("/") + "/loan/evaluate", scenario.to_eval_payload(), timeout)


def collect(backend_url: str, target_per_class: int, timeout: int) -> Counter:
    counts = get_label_counts(backend_url, timeout)
    print(f"Current labeled counts: REPAID={counts['REPAID']} DEFAULTED={counts['DEFAULTED']}")

    # Known high-quality EOA scenario that currently passes policy.
    repaid_wallet = "0x267be1c1d684f78cb4f6a176c4911b741e4ffdc0"
    # Known hard-reject wallets for default class.
    default_wallets = [
        "0x000000000000000000000000000000000000dEaD",
        "0x7a250d5630B4cF539739df2C5dAcb4c659F2488D",
    ]

    repaid_amount_seed = 1000
    default_amount_seed = 300

    while counts["REPAID"] < target_per_class:
        amount = repaid_amount_seed + counts["REPAID"]
        scenario = LoanScenario(wallet=repaid_wallet, amount=float(amount), purpose="repaid_seed")
        result = evaluate(backend_url, scenario, timeout)
        decision_hash = result["decisionHash"]
        label_outcome(backend_url, decision_hash, "REPAID", timeout)
        counts["REPAID"] += 1
        print(
            f"Seeded REPAID {counts['REPAID']}/{target_per_class}: "
            f"wallet={scenario.wallet} amount={scenario.amount} hash={decision_hash}"
        )

    i = 0
    while counts["DEFAULTED"] < target_per_class:
        wallet = default_wallets[i % len(default_wallets)]
        amount = default_amount_seed + counts["DEFAULTED"]
        scenario = LoanScenario(wallet=wallet, amount=float(amount), purpose="default_seed")
        result = evaluate(backend_url, scenario, timeout)
        decision_hash = result["decisionHash"]
        label_outcome(backend_url, decision_hash, "DEFAULTED", timeout)
        counts["DEFAULTED"] += 1
        i += 1
        print(
            f"Seeded DEFAULTED {counts['DEFAULTED']}/{target_per_class}: "
            f"wallet={scenario.wallet} amount={scenario.amount} hash={decision_hash}"
        )

    return counts


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Collect labeled loan outcomes for retraining")
    parser.add_argument("--backend-url", default="http://localhost:8080")
    parser.add_argument("--target-per-class", type=int, default=10)
    parser.add_argument("--timeout", type=int, default=45)
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    try:
        final_counts = collect(args.backend_url, args.target_per_class, args.timeout)
    except (HTTPError, URLError, TimeoutError, KeyError, ValueError) as exc:
        print(f"Collection failed: {exc}")
        raise SystemExit(1)

    print(
        "Collection complete: "
        f"REPAID={final_counts['REPAID']} DEFAULTED={final_counts['DEFAULTED']}"
    )
    if final_counts["REPAID"] < args.target_per_class or final_counts["DEFAULTED"] < args.target_per_class:
        print("Targets not met.")
        raise SystemExit(1)

    raise SystemExit(0)

