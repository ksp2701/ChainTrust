const fs = require("fs");
const path = require("path");

const rootContract = path.join(__dirname, "..", "ChainTrust.sol");
const hardhatContractDir = path.join(__dirname, "..", "contracts");
const hardhatContract = path.join(hardhatContractDir, "ChainTrust.sol");

if (!fs.existsSync(rootContract)) {
  throw new Error(`Source contract not found: ${rootContract}`);
}

fs.mkdirSync(hardhatContractDir, { recursive: true });
fs.copyFileSync(rootContract, hardhatContract);
console.log(`Synced contract source to ${hardhatContract}`);
