from typing import Dict, Optional

from pydantic import BaseModel, Field


class FeatureSchema(BaseModel):
    wallet_age_days: float = Field(ge=0, description="Days since first transaction")
    tx_count: int = Field(ge=0, description="Total transaction count")
    avg_tx_value_eth: float = Field(ge=0, description="Average transaction value in ETH")
    unique_contracts: int = Field(ge=0, description="Number of unique contracts interacted")
    incoming_outgoing_ratio: float = Field(ge=0, description="Incoming vs outgoing tx ratio")
    tx_variance: float = Field(ge=0, description="Variance in transaction values")
    defi_protocol_count: int = Field(ge=0, description="Number of unique DeFi protocols used")
    flash_loan_count: int = Field(ge=0, description="Number of flash loan transactions")
    liquidation_events: int = Field(ge=0, description="Number of liquidation events")
    nft_transaction_count: int = Field(ge=0, description="Number of NFT-related transactions")
    max_single_tx_eth: float = Field(ge=0, description="Maximum single transaction value in ETH")
    dormant_period_days: float = Field(ge=0, description="Longest dormant period in days")
    collateral_ratio: float = Field(ge=0, description="Average collateral ratio across DeFi positions")
    cross_chain_count: int = Field(ge=0, description="Number of cross-chain transactions")
    rugpull_exposure_score: float = Field(
        ge=0, le=1, description="Exposure to known rugpull contracts"
    )

    # Backward-compatible alias
    avg_tx_value: Optional[float] = Field(default=None, ge=0)

    class Config:
        populate_by_name = True


class RiskResult(BaseModel):
    risk_score: float
    risk_level: str
    feature_contributions: Optional[Dict[str, float]] = None
    denial_reasons: Optional[list[str]] = None
