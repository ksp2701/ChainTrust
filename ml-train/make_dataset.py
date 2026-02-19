import numpy as np
import pandas as pd


def make_synthetic(n: int = 5000, seed: int = 42) -> pd.DataFrame:
    rng = np.random.RandomState(seed)

    wallet_age_days = rng.exponential(scale=120, size=n)
    tx_count = rng.poisson(lam=35, size=n)
    avg_tx_value = rng.exponential(scale=0.3, size=n)
    unique_contracts = rng.poisson(lam=6, size=n)
    incoming_outgoing_ratio = rng.beta(2, 2, size=n)
    tx_variance = rng.exponential(scale=0.25, size=n)

    score = (
        (wallet_age_days / (wallet_age_days + 80))
        + (tx_count / 120)
        + (unique_contracts / 30)
        - (avg_tx_value * 0.45)
        - (tx_variance * 0.2)
    )
    label = (score + rng.normal(0, 0.35, n)) > 0.9

    return pd.DataFrame(
        {
            "wallet_age_days": wallet_age_days,
            "tx_count": tx_count,
            "avg_tx_value": avg_tx_value,
            "unique_contracts": unique_contracts,
            "incoming_outgoing_ratio": incoming_outgoing_ratio,
            "tx_variance": tx_variance,
            "label": label.astype(int),
        }
    )
