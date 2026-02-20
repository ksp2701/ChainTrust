<<<<<<< HEAD
"""
ChainTrust — Advanced ML Training Pipeline
GradientBoosting + RandomForest soft-voting ensemble, isotonic calibration.
Target: ≥90% accuracy on DeFi wallet credit risk classification.
"""

from pathlib import Path
import joblib
import numpy as np
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import GradientBoostingClassifier, VotingClassifier, RandomForestClassifier
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
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

LOG_IDX = [1, 3, 6, 7, 8, 9, 10, 13]   # columns to log1p-transform


def build_X(df):
    X = df[FEATURES].values.astype(np.float64)
    X[:, LOG_IDX] = np.log1p(X[:, LOG_IDX])
    return X


def build_base_pipeline():
    gb = GradientBoostingClassifier(
        n_estimators=300,
        max_depth=5,
        learning_rate=0.08,
        min_samples_leaf=20,
        subsample=0.85,
        max_features="sqrt",
        random_state=42,
    )
    rf = RandomForestClassifier(
        n_estimators=200,
        max_depth=12,
        min_samples_leaf=10,
        max_features="sqrt",
        random_state=42,
        n_jobs=1,           # single-threaded to avoid Windows memory spikes
    )
    voting = VotingClassifier(
        estimators=[("gb", gb), ("rf", rf)],
        voting="soft",
        weights=[2, 1],
        n_jobs=1,
    )
    return Pipeline([("scaler", StandardScaler()), ("model", voting)])


def train_and_save() -> None:
    print("Generating dataset (50,000 samples, 15 features)…")
    df = make_synthetic(n=50_000, seed=42)
    X = build_X(df)
    y = df["label"].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.15, random_state=42, stratify=y
    )

    # ── Cross-validate the BASE pipeline (not the calibrated wrapper) ──
    print("Running 5-fold cross-validation on base pipeline…")
    base = build_base_pipeline()
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=0)
    # n_jobs=1 avoids Windows fork + OOM issues
    cv_scores = cross_val_score(base, X_train, y_train, cv=cv, scoring="accuracy", n_jobs=1)
    cv_auc    = cross_val_score(base, X_train, y_train, cv=cv, scoring="roc_auc",  n_jobs=1)
    print(f"  CV accuracy : {cv_scores.mean():.4f} ± {cv_scores.std():.4f}")
    print(f"  CV ROC-AUC  : {cv_auc.mean():.4f} ± {cv_auc.std():.4f}")

    # ── Re-fit base pipeline on a calibration split, then calibrate ────
    print("Fitting base pipeline on 80% of training data…")
    X_fit, X_cal, y_fit, y_cal = train_test_split(
        X_train, y_train, test_size=0.20, random_state=1, stratify=y_train
    )
    base_fitted = build_base_pipeline()
    base_fitted.fit(X_fit, y_fit)

    print("Calibrating probabilities (isotonic, prefit)…")
    calibrated = CalibratedClassifierCV(base_fitted, cv="prefit", method="isotonic")
    calibrated.fit(X_cal, y_cal)

    # ── Evaluation ─────────────────────────────────────────────────────
    acc   = calibrated.score(X_test, y_test)
    proba = calibrated.predict_proba(X_test)[:, 1]
    auc   = roc_auc_score(y_test, proba)
    print(f"\nHeld-out accuracy : {acc:.4f}")
    print(f"Held-out ROC-AUC  : {auc:.4f}")
    print("\nClassification report:")
    print(classification_report(y_test, calibrated.predict(X_test),
                                target_names=["Denied", "Approved"]))

    # ── Save ────────────────────────────────────────────────────────────
    root      = Path(__file__).resolve().parent.parent
    model_dir = root / "ml-service" / "model"
    model_dir.mkdir(parents=True, exist_ok=True)
    model_path = model_dir / "model.pkl"
    joblib.dump({"model": calibrated, "features": FEATURES}, model_path)
    print(f"\nModel saved → {model_path}")

    if acc < 0.88:
        print("⚠️  Accuracy below 88% — check data generation.")
    elif acc >= 0.90:
        print(f"✅  Validation accuracy: {acc:.4f}  (target ≥0.90 — PASSED)")
    else:
        print(f"🟡  Validation accuracy: {acc:.4f}  (good, just below 90%)")
=======
from pathlib import Path

import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split

from make_dataset import make_synthetic


def train_and_save() -> None:
    df = make_synthetic(n=5000, seed=42)
    x = df.drop(columns=["label"]).values
    y = df["label"].values

    x_train, x_test, y_train, y_test = train_test_split(
        x, y, test_size=0.2, random_state=42, stratify=y
    )

    model = RandomForestClassifier(
        n_estimators=150,
        max_depth=10,
        random_state=42,
    )
    model.fit(x_train, y_train)

    acc = model.score(x_test, y_test)

    root = Path(__file__).resolve().parent.parent
    model_dir = root / "ml-service" / "model"
    model_dir.mkdir(parents=True, exist_ok=True)
    model_path = model_dir / "model.pkl"

    joblib.dump(model, model_path)
    print(f"Model saved to {model_path}")
    print(f"Validation accuracy: {acc:.4f}")
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1


if __name__ == "__main__":
    train_and_save()
