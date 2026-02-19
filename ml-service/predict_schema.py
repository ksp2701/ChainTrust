from pydantic import BaseModel, Field


class FeatureSchema(BaseModel):
    wallet_age_days: float = Field(ge=0)
    tx_count: int = Field(ge=0)
    avg_tx_value: float = Field(ge=0)
    unique_contracts: int = Field(ge=0)
    incoming_outgoing_ratio: float = Field(ge=0)
    tx_variance: float = Field(ge=0)


class RiskResult(BaseModel):
    risk_score: float
    risk_level: str
