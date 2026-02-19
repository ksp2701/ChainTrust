from fastapi import FastAPI, HTTPException
from predict_schema import FeatureSchema, RiskResult
import joblib
import numpy as np
import os

app = FastAPI(title="ChainTrust ML Service")

MODEL_PATH = "/app/model/model.pkl"
model = None


@app.on_event("startup")
def load_model() -> None:
    global model
    if not os.path.exists(MODEL_PATH):
        model = None
        return
    model = joblib.load(MODEL_PATH)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "model_loaded": model is not None,
    }


@app.post("/predict", response_model=RiskResult)
def predict(feat: FeatureSchema) -> RiskResult:
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
