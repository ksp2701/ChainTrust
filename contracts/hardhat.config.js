require("@nomicfoundation/hardhat-toolbox");
const dotenv = require("dotenv");
const path = require("path");

dotenv.config({ path: path.join(__dirname, ".env") });
dotenv.config({ path: path.join(__dirname, "..", ".env") });

function normalizePk(value) {
  if (!value) return "";
  const normalized = value.startsWith("0x") ? value.slice(2) : value;
  return /^[0-9a-fA-F]{64}$/.test(normalized) ? normalized : "";
}

function firstEtherscanKey(raw) {
  if (!raw) return "";
  const first = raw.split(",").map((v) => v.trim()).filter(Boolean)[0];
  return first || "";
}

const localRpc = process.env.HARDHAT_LOCAL_RPC_URL || "http://127.0.0.1:8545";
const sepoliaRpc = process.env.HARDHAT_SEPOLIA_RPC_URL || process.env.ETH_RPC_URL || "";
const deployerPk = normalizePk(
  process.env.HARDHAT_DEPLOYER_PRIVATE_KEY || process.env.BLOCKCHAIN_PRIVATE_KEY || ""
);
const etherscanKey =
  process.env.HARDHAT_ETHERSCAN_API_KEY || firstEtherscanKey(process.env.ETHERSCAN_API_KEYS);

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: {
    version: "0.8.17",
    settings: {
      optimizer: {
        enabled: true,
        runs: 200
      }
    }
  },
  networks: {
    hardhat: {
      chainId: 31337
    },
    localhost: {
      url: localRpc,
      chainId: 31337
    },
    sepolia: {
      url: sepoliaRpc,
      chainId: 11155111,
      accounts: deployerPk ? [`0x${deployerPk}`] : []
    }
  },
  etherscan: {
    apiKey: {
      sepolia: etherscanKey
    }
  }
};
