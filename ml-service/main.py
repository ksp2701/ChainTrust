e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1


@app.on_event("startup")
def load_model() -> None:
<<<<<<< HEAD
    global _bundle
    path = os.environ.get("MODEL_PATH", MODEL_PATH)
    if not os.path.exists(path):
        _bundle = None
        return
    loaded = joblib.load(path)
    if isinstance(loaded, dict) and "model" in loaded:
        _bundle = loaded
    else:
        # backward-compat — old bare model
        _bundle = {"model": loaded, "features": [
            "wallet_age_days", "tx_count", "avg_tx_value_eth",
            "unique_contracts", "incoming_outgoing_ratio", "tx_variance",
            "defi_protocol_count", "flash_loan_count", "liquidation_events",
            "nft_transaction_count", "max_single_tx_eth", "dormant_period_days",
            "collateral_ratio", "cross_chain_count", "rugpull_exposure_score",
        ]}
=======
    global model
    if not os.path.exists(MODEL_PATH):
        model = None
        return
    model = joblib.load(MODEL_PATH)
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
<<<<<<< HEAD
        "model_loaded": _bundle is not None,
        "features": _bundle["features"] if _bundle else [],
=======
        "model_loaded": model is not None,
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
    }


@app.post("/predict", response_model=RiskResult)
def predict(feat: FeatureSchema) -> RiskResult:
<<<<<<< HEAD
    if _bundle is None:
        raise HTTPException(status_code=503, detail="model.pkl not found — run train_model.py first")

    model = _bundle["model"]
    feature_names = _bundle["features"]

    # Build feature dict — handle legacy avg_tx_value alias
    feat_dict = feat.dict()
    if feat_dict.get("avg_tx_value_eth") is None and feat_dict.get("avg_tx_value") is not None:
        feat_dict["avg_tx_value_eth"] = feat_dict["avg_tx_value"]

    raw = np.array([[feat_dict.get(f, 0.0) for f in feature_names]], dtype=np.float64)

    # Log-transform skewed features (same indices as training)
    log_idx = [1, 3, 6, 7, 8, 9, 10, 13]
    valid_idx = [i for i in log_idx if i < raw.shape[1]]
    raw[:, valid_idx] = np.log1p(raw[:, valid_idx])

    try:
        if hasattr(model, "predict_proba"):
            prob = float(model.predict_proba(raw)[0][1])
        else:
            prob = float(model.predict(raw)[0])
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))

    prob = max(0.0, min(1.0, prob))

    # Risk is inverse of trust — prob is P(approved)
    risk_score = 1.0 - prob
    level = "LOW" if risk_score < 0.35 else ("MEDIUM" if risk_score < 0.65 else "HIGH")

    # Feature contribution — simple sensitivity analysis
    contributions = {}
    for i, fname in enumerate(feature_names):
        perturbed = raw.copy()
        perturbed[0, i] = perturbed[0, i] * 1.1 + 0.01
        try:
            p2 = float(model.predict_proba(perturbed)[0][1])
            contributions[fname] = round(p2 - prob, 4)
        except Exception:
            contributions[fname] = 0.0

    denial_reasons = _build_denial_reasons(feat, prob)

    return RiskResult(
        risk_score=risk_score,
        risk_level=level,
        feature_contributions=contributions,
        denial_reasons=denial_reasons,
    )


def _build_denial_reasons(feat: FeatureSchema, trust_prob: float) -> list:
    reasons = []
    if feat.wallet_age_days < 60:
        reasons.append("Wallet age below 60 days — insufficient history")
    if feat.tx_count < 10:
        reasons.append("Very low transaction count — limited activity")
    if feat.liquidation_events > 0:
        reasons.append(f"{feat.liquidation_events} liquidation event(s) — high-risk history")
    if feat.flash_loan_count > 2:
        reasons.append(f"{feat.flash_loan_count} flash loans detected — elevated manipulation risk")
    if feat.rugpull_exposure_score > 0.3:
        reasons.append("High exposure to rugpull contracts")
    if feat.collateral_ratio < 1.2:
        reasons.append("Insufficient collateral ratio for requested loan")
    if feat.dormant_period_days > 180:
        reasons.append("Long dormant period suggests inactive wallet")
    if feat.incoming_outgoing_ratio < 0.2:
        reasons.append("Very few incoming transactions relative to outgoing")
    if trust_prob > 0.5 and not reasons:
        reasons.append("Wallet meets all credit criteria")
    return reasons
=======
    if model is None:
        raise HTTPException(status_code=503, detail="model.pkl not found; train model first")

    try:
        x = np.array(
            [[
                feat.wallet_age_days,
                feat.tx_count,
                feat.avg_tx_value,
                feat.unique_contracts,
                feat.incoming_outgoing_ratio,
                feat.tx_variance,
            ]]
        )
        if hasattr(model, "predict_proba"):
            prob = float(model.predict_proba(x)[0][1])
        else:
            prob = float(model.predict(x)[0])

        prob = max(0.0, min(1.0, prob))
        level = "LOW" if prob < 0.4 else ("MEDIUM" if prob < 0.7 else "HIGH")
        return RiskResult(risk_score=prob, risk_level=level)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))
>>>>>>> e6bab9ff3e4c81f53c66b24db7e96dd1d61d97c1
