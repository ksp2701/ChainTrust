# ChainTrust

Decentralized credit intelligence prototype combining:
- Spring Boot backend (Java 17)
- FastAPI ML scoring microservice (Python)
- Solidity smart contract
- React frontend
- Docker Compose orchestration

## Quick Start
1. Copy `.env.example` to `.env` and fill values:
   ```bash
   cp .env.example .env
   ```
2. Train and export model:
   ```bash
   python ml-train/train_model.py
   ```
3. Start stack:
   ```bash
   docker compose up --build
   ```
4. Open:
- Frontend: <http://localhost:3000>
- Backend: <http://localhost:8080>
- ML service docs: <http://localhost:8000/docs>

## Team
- Aamer: ML and DevOps
- Vedant: Blockchain and smart contracts
- Ketan: Java backend and frontend

## API Flow
1. `GET /wallet/{address}` -> extract wallet features.
2. `POST /risk` -> backend calls ML `/predict`.
3. `POST /loan/evaluate` -> creates a conservative approve/deny response.

## Notes
- `ml-service/model/model.pkl` must exist for normal ML scoring.
- If ML service is unavailable, backend defaults to `HIGH` risk.
- On-chain storage should persist only hashes, not raw PII.
