#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRACTS_DIR="${ROOT_DIR}/contracts"
ENV_FILE="${ROOT_DIR}/.env"
DEFAULT_DEV_PK="ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"

set_env() {
  local key="$1"
  local value="$2"
  if grep -qE "^${key}=" "${ENV_FILE}"; then
    sed -i.bak -E "s|^${key}=.*|${key}=${value}|g" "${ENV_FILE}"
  else
    echo "${key}=${value}" >>"${ENV_FILE}"
  fi
}

pushd "${CONTRACTS_DIR}" >/dev/null
npm install
npm run compile

if ! lsof -i :8545 >/dev/null 2>&1; then
  nohup npx hardhat node --hostname 0.0.0.0 --port 8545 > hardhat-node.log 2>&1 &
  sleep 4
fi

npm run deploy:local
popd >/dev/null

DEPLOY_FILE="${CONTRACTS_DIR}/deployments/localhost.json"
if [[ ! -f "${DEPLOY_FILE}" ]]; then
  echo "Deployment file not found: ${DEPLOY_FILE}"
  exit 1
fi

CONTRACT_ADDRESS="$(node -e "const fs=require('fs');const d=JSON.parse(fs.readFileSync('${DEPLOY_FILE}','utf8'));process.stdout.write(d.address||'');")"
if [[ -z "${CONTRACT_ADDRESS}" ]]; then
  echo "Could not read deployed contract address"
  exit 1
fi

set_env "ETH_RPC_URL" "http://host.docker.internal:8545"
set_env "CONTRACT_ADDRESS" "${CONTRACT_ADDRESS}"
set_env "BLOCKCHAIN_ENABLED" "true"
set_env "BLOCKCHAIN_CHAIN_ID" "31337"
set_env "BLOCKCHAIN_PRIVATE_KEY" "${DEFAULT_DEV_PK}"

pushd "${ROOT_DIR}" >/dev/null
docker compose up -d backend
popd >/dev/null

echo "Hardhat local chain active."
echo "Contract address: ${CONTRACT_ADDRESS}"
