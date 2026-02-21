"""
ChainTrust model training pipeline.

Default mode trains from REAL labeled outcomes captured by backend:
- source=api -> GET {backend}/loan/training-data
- source=csv -> read a labeled CSV file

Synthetic mode is available only when --allow-synthetic is set.

Outputs:
- ml-service/model/model.pkl
- ml-service/model/policy_thresholds.json
- ml-service/model/metrics.json
"""

from __future__ import annotations

import argparse
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import urlopen

import joblib
import numpy as np
import pandas as pd
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier, VotingClassifier
from sklearn.frozen import FrozenEstimator
from sklearn.metrics import accuracy_score, classification_report, f1_score, roc_auc_score
from sklearn.model_selection import GroupShuffleSplit, StratifiedGroupKFold, StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from make_dataset import make_synthetic

FEATURES = [
    "wallet_age_days",
    "tx_count",
    "avg_tx_value_eth",
    "unique_contracts",
    "incoming_outgoing_ratio",
    "tx_variance",
    "defi_protocol_count",
    "flash_loan_count",
    "liquidation_events",
    "nft_transaction_count",
    "max_single_tx_eth",
    "dormant_period_days",
    "collateral_ratio",
    "cross_chain_count",
    "rugpull_exposure_score",
]

LOG_IDX = [1, 3, 6, 7, 8, 9, 10, 13]


def build_X(df: pd.DataFrame) -> np.ndarray:
    X = df[FEATURES].astype(np.float64).values
    valid_idx = [i for i in LOG_IDX if i < X.shape[1]]
    X[:, valid_idx] = np.log1p(X[:, valid_idx])
    return X


def build_base_pipeline() -> Pipeline:
    gb = GradientBoostingClassifier(
        n_estimators=220,
        max_depth=4,
        learning_rate=0.07,
        min_samples_leaf=20,
        subsample=0.85,
        max_features="sqrt",
        random_state=42,
    )
    rf = RandomForestClassifier(
        n_estimators=240,
        max_depth=12,
        min_samples_leaf=8,
        max_features="sqrt",
        random_state=42,
        n_jobs=1,
    )
    voting = VotingClassifier(
        estimators=[("gb", gb), ("rf", rf)],
        voting="soft",
        weights=[2, 1],
        n_jobs=1,
    )
    return Pipeline([("scaler", StandardScaler()), ("model", voting)])


def load_dataset_from_api(backend_url: str) -> pd.DataFrame:
    url = backend_url.rstrip("/") + "/loan/training-data"
    with urlopen(url, timeout=30) as response:
        payload = response.read().decode("utf-8")
    rows = json.loads(payload)
    if not isinstance(rows, list):
        raise ValueError("Backend training-data endpoint returned non-list payload")
    return pd.DataFrame(rows)


def load_dataset_from_csv(csv_path: Path) -> pd.DataFrame:
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV dataset not found: {csv_path}")
    return pd.read_csv(csv_path)


def normalize_dataset(df: pd.DataFrame) -> pd.DataFrame:
    if "label" not in df.columns:
        raise ValueError("Dataset must include 'label' column")

    missing = [c for c in FEATURES if c not in df.columns]
    if missing:
        raise ValueError(f"Dataset missing required features: {', '.join(missing)}")

    out = df.copy()
    for col in FEATURES:
        out[col] = pd.to_numeric(out[col], errors="coerce")
    out["label"] = pd.to_numeric(out["label"], errors="coerce")
    out = out.dropna(subset=FEATURES + ["label"])
    out["label"] = out["label"].astype(int)
    out = out[out["label"].isin([0, 1])]
    return out


def choose_threshold(y_true: np.ndarray, probs: np.ndarray) -> float:
    candidates = np.unique(np.round(np.linspace(0.05, 0.95, 181), 3))
    best_t = 0.5
    best_f1 = -1.0
    for t in candidates:
        pred = (probs >= t).astype(int)
        score = f1_score(y_true, pred)
        if score > best_f1:
            best_f1 = score
            best_t = float(t)
    return best_t


def calibrate_policy_thresholds(y_true: np.ndarray, probs: np.ndarray) -> dict:
    approve = choose_threshold(y_true, probs)
    approved_probs = probs[y_true == 1]
    if approved_probs.size < 10:
        approved_probs = probs

    q35 = float(np.quantile(approved_probs, 0.35))
    q60 = float(np.quantile(approved_probs, 0.60))
    q85 = float(np.quantile(approved_probs, 0.85))

    bronze = float(np.clip(approve, 0.25, 0.90))
    silver = float(np.clip(max(bronze + 0.03, q35), bronze, 0.95))
    gold = float(np.clip(max(silver + 0.03, q60), silver, 0.97))
    platinum = float(np.clip(max(gold + 0.03, q85), gold, 0.99))

    return {
        "platinum_min_trust": round(platinum, 4),
        "gold_min_trust": round(gold, 4),
        "silver_min_trust": round(silver, 4),
        "bronze_min_trust": round(bronze, 4),
        "silver_max_amount": 5000,
        "bronze_max_amount": 1000,
    }


def split_train_test(
    X: np.ndarray,
    y: np.ndarray,
    groups: np.ndarray | None,
    test_size: float = 0.20,
) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    if groups is None:
        return train_test_split(X, y, test_size=test_size, random_state=42, stratify=y)

    splitter = GroupShuffleSplit(n_splits=1, test_size=test_size, random_state=42)
    train_idx, test_idx = next(splitter.split(X, y, groups=groups))
    return X[train_idx], X[test_idx], y[train_idx], y[test_idx]


def group_aware_cv_scores(base: Pipeline, X: np.ndarray, y: np.ndarray, groups: np.ndarray | None):
    if groups is None:
        cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=0)
        cv_acc = cross_val_score(base, X, y, cv=cv, scoring="accuracy", n_jobs=1)
        cv_auc = cross_val_score(base, X, y, cv=cv, scoring="roc_auc", n_jobs=1)
        return 5, cv_acc, cv_auc

    label_counts = np.bincount(y)
    min_class = int(label_counts.min()) if label_counts.size > 1 else 0
    n_splits = max(2, min(5, min_class))
    cv = StratifiedGroupKFold(n_splits=n_splits, shuffle=True, random_state=0)
    cv_acc = cross_val_score(base, X, y, cv=cv, scoring="accuracy", n_jobs=1, groups=groups)
    cv_auc = cross_val_score(base, X, y, cv=cv, scoring="roc_auc", n_jobs=1, groups=groups)
    return n_splits, cv_acc, cv_auc


def repeated_holdout_scores(
    X: np.ndarray,
    y: np.ndarray,
    groups: np.ndarray | None,
    seeds: list[int],
) -> dict[str, float]:
    acc_scores: list[float] = []
    auc_scores: list[float] = []
    f1_scores: list[float] = []

    for seed in seeds:
        if groups is None:
            X_train, X_test, y_train, y_test = train_test_split(
                X, y, test_size=0.20, random_state=seed, stratify=y
            )
            g_train = None
        else:
            split = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=seed)
            train_idx, test_idx = next(split.split(X, y, groups=groups))
            X_train, X_test = X[train_idx], X[test_idx]
            y_train, y_test = y[train_idx], y[test_idx]
            g_train = groups[train_idx]
            g_test = groups[test_idx]
            if set(g_train).intersection(set(g_test)):
                raise RuntimeError("Leakage detected during repeated hold-out evaluation")

        if g_train is None:
            X_fit, X_cal, y_fit, y_cal = train_test_split(
                X_train, y_train, test_size=0.20, random_state=seed + 101, stratify=y_train
            )
        else:
            fit_split = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=seed + 101)
            fit_idx, cal_idx = next(fit_split.split(X_train, y_train, groups=g_train))
            X_fit, X_cal = X_train[fit_idx], X_train[cal_idx]
            y_fit, y_cal = y_train[fit_idx], y_train[cal_idx]
            if len(np.unique(y_cal)) < 2:
                # Fallback keeps calibration valid if grouped split produced a single-class fold.
                X_fit, X_cal, y_fit, y_cal = train_test_split(
                    X_train, y_train, test_size=0.20, random_state=seed + 301, stratify=y_train
                )

        base = build_base_pipeline()
        base.fit(X_fit, y_fit)
        calibrated = CalibratedClassifierCV(FrozenEstimator(base), method="isotonic")
        calibrated.fit(X_cal, y_cal)

        proba = calibrated.predict_proba(X_test)[:, 1]
        pred = (proba >= 0.5).astype(int)

        acc_scores.append(float(accuracy_score(y_test, pred)))
        auc_scores.append(float(roc_auc_score(y_test, proba)))
        f1_scores.append(float(f1_score(y_test, pred)))

    return {
        "repeats": int(len(seeds)),
        "accuracy_mean": float(np.mean(acc_scores)),
        "accuracy_std": float(np.std(acc_scores)),
        "roc_auc_mean": float(np.mean(auc_scores)),
        "roc_auc_std": float(np.std(auc_scores)),
        "f1_mean": float(np.mean(f1_scores)),
        "f1_std": float(np.std(f1_scores)),
    }


def train_and_save(
    source: str,
    backend_url: str,
    csv_path: Path,
    allow_synthetic: bool,
    min_samples: int,
    model_path: Path,
    thresholds_path: Path,
    metrics_path: Path,
) -> None:
    if source == "api":
        print(f"Loading labeled dataset from backend API: {backend_url}/loan/training-data")
        try:
            df = load_dataset_from_api(backend_url)
        except (HTTPError, URLError, TimeoutError, ValueError) as exc:
            if not allow_synthetic:
                raise RuntimeError(
                    "Failed to load real labeled dataset from backend API. "
                    "Record outcomes first or use --allow-synthetic for development."
                ) from exc
            print(f"Real dataset unavailable ({exc}); falling back to synthetic dataset.")
            df = make_synthetic(n=50_000, seed=42)
    elif source == "csv":
        print(f"Loading labeled dataset from CSV: {csv_path}")
        df = load_dataset_from_csv(csv_path)
    elif source == "synthetic":
        if not allow_synthetic:
            raise RuntimeError("Synthetic training is blocked by default. Use --allow-synthetic to enable it.")
        print("Generating synthetic dataset (development mode).")
        df = make_synthetic(n=50_000, seed=42)
    else:
        raise ValueError(f"Unsupported source: {source}")

    df = normalize_dataset(df)
    if len(df) < min_samples:
        raise RuntimeError(
            f"Insufficient labeled samples: {len(df)} < {min_samples}. "
            "Capture more outcomes via /loan/outcome before training."
        )

    label_counts = df["label"].value_counts().to_dict()
    if len(label_counts) < 2:
        raise RuntimeError("Dataset must contain both classes (label 0 and 1)")
    min_class_count = min(label_counts.values())
    if min_class_count < 10:
        raise RuntimeError(
            f"Each class must have at least 10 samples for stable training; got min class count={min_class_count}. "
            "Record more labeled outcomes via /loan/outcome."
        )

    print(f"Training on {len(df)} labeled samples")
    print(f"Label distribution: {label_counts}")

    X = build_X(df)
    y = df["label"].values

    groups = None
    if "wallet_address" in df.columns:
        groups = df["wallet_address"].astype(str).str.lower().values
        if len(np.unique(groups)) < 10:
            groups = None

    X_train, X_test, y_train, y_test = split_train_test(X, y, groups, test_size=0.20)
    overlap_count = 0
    if groups is not None:
        # Recreate train/test groups by matching rows via index-like split using boolean masks is not safe;
        # generate directly from original split with indices.
        g_split = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=42)
        train_idx, test_idx = next(g_split.split(X, y, groups=groups))
        g_train = groups[train_idx]
        g_test = groups[test_idx]
        overlap = set(g_train).intersection(set(g_test))
        overlap_count = len(overlap)
        if overlap:
            raise RuntimeError("Leakage detected: wallet overlap between train/test groups")
    else:
        g_train = None

    base = build_base_pipeline()
    cv_splits, cv_acc, cv_auc = group_aware_cv_scores(base, X_train, y_train, g_train)
    print(f"Running {cv_splits}-fold cross-validation")
    print(f"  CV accuracy: {cv_acc.mean():.4f} +/- {cv_acc.std():.4f}")
    print(f"  CV ROC-AUC : {cv_auc.mean():.4f} +/- {cv_auc.std():.4f}")

    eval_seeds = [1, 2, 3, 4, 5, 11, 19, 29, 41, 59]
    repeated_metrics = repeated_holdout_scores(X, y, groups, eval_seeds)
    print(f"Repeated grouped hold-out ({repeated_metrics['repeats']} runs):")
    print(
        f"  Accuracy: {repeated_metrics['accuracy_mean']:.4f} "
        f"+/- {repeated_metrics['accuracy_std']:.4f}"
    )
    print(
        f"  ROC-AUC : {repeated_metrics['roc_auc_mean']:.4f} "
        f"+/- {repeated_metrics['roc_auc_std']:.4f}"
    )
    print(
        f"  F1      : {repeated_metrics['f1_mean']:.4f} "
        f"+/- {repeated_metrics['f1_std']:.4f}"
    )

    if g_train is None:
        X_fit, X_cal, y_fit, y_cal = train_test_split(
            X_train, y_train, test_size=0.20, random_state=1, stratify=y_train
        )
    else:
        g_fit_split = GroupShuffleSplit(n_splits=1, test_size=0.20, random_state=1)
        fit_idx, cal_idx = next(g_fit_split.split(X_train, y_train, groups=g_train))
        X_fit, X_cal = X_train[fit_idx], X_train[cal_idx]
        y_fit, y_cal = y_train[fit_idx], y_train[cal_idx]

    base_fitted = build_base_pipeline()
    base_fitted.fit(X_fit, y_fit)

    calibrated = CalibratedClassifierCV(FrozenEstimator(base_fitted), method="isotonic")
    calibrated.fit(X_cal, y_cal)

    test_acc = calibrated.score(X_test, y_test)
    test_proba = calibrated.predict_proba(X_test)[:, 1]
    test_auc = roc_auc_score(y_test, test_proba)

    print("Reference grouped hold-out (seed=42):")
    print(f"  Accuracy: {test_acc:.4f}")
    print(f"  ROC-AUC : {test_auc:.4f}")
    print("Classification report:")
    print(classification_report(y_test, calibrated.predict(X_test), target_names=["Defaulted", "Repaid"]))

    thresholds = calibrate_policy_thresholds(y_test, test_proba)
    print("Calibrated policy thresholds:")
    for k, v in thresholds.items():
        print(f"  {k}: {v}")

    model_path.parent.mkdir(parents=True, exist_ok=True)
    thresholds_path.parent.mkdir(parents=True, exist_ok=True)
    metrics_path.parent.mkdir(parents=True, exist_ok=True)

    bundle = {
        "model": calibrated,
        "features": FEATURES,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "source": source,
        "sample_count": int(len(df)),
    }
    joblib.dump(bundle, model_path)

    thresholds_payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": source,
        "sample_count": int(len(df)),
        "thresholds": thresholds,
    }
    thresholds_path.write_text(json.dumps(thresholds_payload, indent=2), encoding="utf-8")

    metrics_payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source": source,
        "sample_count": int(len(df)),
        "label_distribution": {str(k): int(v) for k, v in label_counts.items()},
        "grouping": {
            "group_feature": "wallet_address" if groups is not None else None,
            "groups_used": bool(groups is not None),
            "unique_group_count": int(len(np.unique(groups))) if groups is not None else None,
            "train_test_wallet_overlap_count": int(overlap_count),
        },
        "cross_validation": {
            "folds": int(cv_splits),
            "accuracy_mean": float(cv_acc.mean()),
            "accuracy_std": float(cv_acc.std()),
            "roc_auc_mean": float(cv_auc.mean()),
            "roc_auc_std": float(cv_auc.std()),
        },
        "repeated_group_holdout": repeated_metrics,
        "held_out": {
            "accuracy": float(test_acc),
            "roc_auc": float(test_auc),
            "support": int(len(y_test)),
        },
        "artifacts": {
            "model": str(model_path),
            "policy_thresholds": str(thresholds_path),
        },
    }
    metrics_path.write_text(json.dumps(metrics_payload, indent=2), encoding="utf-8")

    print(f"Model saved: {model_path}")
    print(f"Policy thresholds saved: {thresholds_path}")
    print(f"Metrics saved: {metrics_path}")


def parse_args() -> argparse.Namespace:
    root = Path(__file__).resolve().parent.parent
    default_model = root / "ml-service" / "model" / "model.pkl"
    default_thresholds = root / "ml-service" / "model" / "policy_thresholds.json"
    default_metrics = root / "ml-service" / "model" / "metrics.json"
    default_csv = Path(__file__).resolve().parent / "data" / "loan_outcomes.csv"

    parser = argparse.ArgumentParser(description="Train ChainTrust model with real labeled outcomes")
    parser.add_argument("--source", choices=["api", "csv", "synthetic"], default="api")
    parser.add_argument("--backend-url", default=os.environ.get("CHAINTRUST_BACKEND_URL", "http://localhost:8080"))
    parser.add_argument("--csv", type=Path, default=default_csv)
    parser.add_argument("--allow-synthetic", action="store_true")
    parser.add_argument("--min-samples", type=int, default=20)
    parser.add_argument("--model-out", type=Path, default=default_model)
    parser.add_argument("--thresholds-out", type=Path, default=default_thresholds)
    parser.add_argument("--metrics-out", type=Path, default=default_metrics)
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    try:
        train_and_save(
            source=args.source,
            backend_url=args.backend_url,
            csv_path=args.csv,
            allow_synthetic=args.allow_synthetic,
            min_samples=args.min_samples,
            model_path=args.model_out,
            thresholds_path=args.thresholds_out,
            metrics_path=args.metrics_out,
        )
    except RuntimeError as exc:
        print(f"Training aborted: {exc}")
        raise SystemExit(1)
