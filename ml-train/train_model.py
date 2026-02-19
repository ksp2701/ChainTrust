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


if __name__ == "__main__":
    train_and_save()
