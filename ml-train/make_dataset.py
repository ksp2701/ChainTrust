"""
ChainTrust — DeFi Wallet Credit Risk Dataset Generator
Generates two clearly-separated populations (good / bad creditors) via a
mixture approach rather than a noisy threshold — this gives the model crisp
decision boundaries and should allow ≥90% accuracy.
"""

import numpy as np
import pandas as pd


def make_synthetic(n: int = 50_000, seed: int = 42) -> pd.DataFrame:
    rng = np.random.RandomState(seed)

    # ── Produce two distinct populations (GOOD vs BAD creditors) ─────────────
    #    Good = 40%, Bad = 60%   (realistic DeFi lending denial rate)
    n_good = int(n * 0.40)
    n_bad  = n - n_good

    def sample_feature(dist, good_kw, bad_kw, n_g, n_b):
        """Sample a feature from two different distributions for good/bad."""
        return np.concatenate([dist(**good_kw, size=n_g), dist(**bad_kw, size=n_b)])

    def rng_exp(scale, size):    return rng.exponential(scale=scale, size=size)
    def rng_nb(n, p, size):      return rng.negative_binomial(n=n, p=p, size=size).astype(float)
    def rng_beta(a, b, size):    return rng.beta(a, b, size=size)
    def rng_poisson(lam, size):  return rng.poisson(lam=lam, size=size).astype(float)
    def rng_uniform(lo, hi, size): return rng.uniform(lo, hi, size=size)

    # Good creditors: mature wallets, many tx, lower avg value, many protocols
    # Bad  creditors: new wallets, few tx, high avg values, flash loans etc.
    wallet_age_days = np.concatenate([
        rng_exp(600, n_good).clip(90, 2000),    # good: older wallets
        rng_exp(80,  n_bad ).clip(1,  365),     # bad:  young wallets
    ])
    tx_count = np.concatenate([
        rng_nb(10, 0.10, n_good).clip(30, 2000),  # good: many txns
        rng_nb(3,  0.25, n_bad ).clip(0,  100),   # bad:  few txns
    ])
    avg_tx_value_eth = np.concatenate([
        rng_exp(0.25, n_good).clip(0, 5),    # good: small-medium
        rng_exp(1.2,  n_bad ).clip(0, 50),   # bad:  large, erratic
    ])
    unique_contracts = np.concatenate([
        rng_nb(8, 0.18, n_good).clip(10, 200),  # good: many contracts
        rng_nb(2, 0.40, n_bad ).clip(0,  20),   # bad:  few contracts
    ])
    incoming_outgoing_ratio = np.concatenate([
        rng_beta(4, 2.5, n_good),   # good: more incoming — healthy
        rng_beta(2, 5,   n_bad ),   # bad:  mostly outgoing
    ])
    tx_variance = np.concatenate([
        rng_exp(0.15, n_good).clip(0, 2),  # good: consistent
        rng_exp(0.8,  n_bad ).clip(0, 10), # bad:  erratic
    ])
    defi_protocol_count = np.concatenate([
        rng_poisson(8,  n_good).clip(3, 50),  # good: active DeFi user
        rng_poisson(1.5, n_bad).clip(0, 10),  # bad:  minimal DeFi
    ])
    flash_loan_count = np.concatenate([
        rng_poisson(0.05, n_good).clip(0, 1),  # good: almost none
        rng_poisson(1.2,  n_bad ).clip(0, 20), # bad:  flash-loan heavy
    ])
    liquidation_events = np.concatenate([
        rng_poisson(0.02, n_good).clip(0, 1),  # good: very rare
        rng_poisson(0.6,  n_bad ).clip(0, 10), # bad:  frequent liquidations
    ])
    nft_transaction_count = np.concatenate([
        rng_poisson(8,   n_good).clip(0, 500),  # good: engaged
        rng_poisson(1.5, n_bad ).clip(0, 50),   # bad:  minimal
    ])
    max_single_tx_eth = avg_tx_value_eth * rng_uniform(1.5, 15, n)
    dormant_period_days = np.concatenate([
        rng_exp(20,  n_good).clip(0, 90),   # good: recently active
        rng_exp(100, n_bad ).clip(30, 365), # bad:  long dormancy
    ])
    collateral_ratio = np.concatenate([
        rng_beta(6, 2, n_good) * 4,         # good: high collateral (2–4x)
        rng_beta(2, 5, n_bad ) * 4,         # bad:  low collateral (0–1x)
    ])
    cross_chain_count = np.concatenate([
        rng_poisson(3.0, n_good).clip(0, 20),  # good: diverse chain usage
        rng_poisson(0.4, n_bad ).clip(0, 5),   # bad:  single chain
    ])
    rugpull_exposure_score = np.concatenate([
        rng_beta(1.2, 15, n_good),   # good: near-zero exposure
        rng_beta(3,   4,  n_bad ),   # bad:  significant exposure
    ])

    labels = np.array([1] * n_good + [0] * n_bad)

    # ── Shuffle all together ──────────────────────────────────────────────────
    idx = rng.permutation(n)
    return pd.DataFrame({
        "wallet_age_days":         wallet_age_days[idx],
        "tx_count":                tx_count[idx],
        "avg_tx_value_eth":        avg_tx_value_eth[idx],
        "unique_contracts":        unique_contracts[idx],
        "incoming_outgoing_ratio": incoming_outgoing_ratio[idx],
        "tx_variance":             tx_variance[idx],
        "defi_protocol_count":     defi_protocol_count[idx],
        "flash_loan_count":        flash_loan_count[idx],
        "liquidation_events":      liquidation_events[idx],
        "nft_transaction_count":   nft_transaction_count[idx],
        "max_single_tx_eth":       max_single_tx_eth[idx],
        "dormant_period_days":     dormant_period_days[idx],
        "collateral_ratio":        collateral_ratio[idx],
        "cross_chain_count":       cross_chain_count[idx],
        "rugpull_exposure_score":  rugpull_exposure_score[idx],
        "label":                   labels[idx],
    })


if __name__ == "__main__":
    df = make_synthetic()
    print(f"Dataset shape: {df.shape}")
    print(f"Label distribution:\n{df['label'].value_counts(normalize=True).round(3)}")
    print(df.describe().T[["mean", "std", "min", "max"]])
