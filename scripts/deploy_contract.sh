#!/usr/bin/env bash
set -euo pipefail

NETWORK="${1:-localhost}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRACTS_DIR="${ROOT_DIR}/contracts"

if [[ ! -f "${CONTRACTS_DIR}/package.json" ]]; then
  echo "Hardhat workspace not found at ${CONTRACTS_DIR}"
  exit 1
fi

pushd "${CONTRACTS_DIR}" >/dev/null
npm install
npm run compile

if [[ "${NETWORK}" == "localhost" ]]; then
  npx hardhat run scripts/deploy.js --network localhost
elif [[ "${NETWORK}" == "sepolia" ]]; then
  npx hardhat run scripts/deploy.js --network sepolia
else
  echo "Unsupported network: ${NETWORK}. Use localhost or sepolia."
  exit 1
fi

popd >/dev/null
