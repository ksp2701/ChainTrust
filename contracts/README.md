# ChainTrust Hardhat

Hardhat workspace for deploying `ChainTrust.sol` to local Hardhat or Sepolia.
`contracts/ChainTrust.sol` is the source of truth; scripts auto-sync it into Hardhat's compile folder.

## 1) Install

```bash
cd contracts
npm install
```

## 2) Compile

```bash
npm run compile
```

## 3) Local real chain (Hardhat node)

Terminal 1:

```bash
npm run node
```

Terminal 2:

```bash
npm run deploy:local
```

This writes `contracts/deployments/localhost.json`.

Use these backend values in root `.env`:

```env
ETH_RPC_URL=http://host.docker.internal:8545
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_CHAIN_ID=31337
CONTRACT_ADDRESS=<from localhost.json>
BLOCKCHAIN_PRIVATE_KEY=ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80
```

## 4) Sepolia deploy

Set env (root `.env` or `contracts/.env`):

```env
HARDHAT_SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/<project_id>
HARDHAT_DEPLOYER_PRIVATE_KEY=<private_key_without_0x>
HARDHAT_ETHERSCAN_API_KEY=<etherscan_key>
```

Deploy:

```bash
npm run deploy:sepolia
```

Verify:

```bash
npm run verify:sepolia <deployed_contract_address>
```
