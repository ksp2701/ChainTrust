const fs = require("fs");
const path = require("path");
const hre = require("hardhat");

function isPlaceholder(value) {
  if (!value) return true;
  return value.includes("<") || value.includes("YOUR_") || value.includes("PROJECT_ID");
}

async function main() {
  const networkName = hre.network.name;
  const [deployer] = await hre.ethers.getSigners();
  const network = await hre.ethers.provider.getNetwork();

  if (
    networkName === "sepolia" &&
    (isPlaceholder(process.env.HARDHAT_SEPOLIA_RPC_URL || process.env.ETH_RPC_URL) ||
      isPlaceholder(process.env.HARDHAT_DEPLOYER_PRIVATE_KEY || process.env.BLOCKCHAIN_PRIVATE_KEY))
  ) {
    throw new Error(
      "Sepolia deployment is not configured. Set HARDHAT_SEPOLIA_RPC_URL and HARDHAT_DEPLOYER_PRIVATE_KEY."
    );
  }

  console.log(`Deploying ChainTrust to network: ${networkName}`);
  console.log(`Chain ID: ${network.chainId}`);
  console.log(`Deployer: ${deployer.address}`);

  const factory = await hre.ethers.getContractFactory("ChainTrust");
  const contract = await factory.deploy();
  await contract.waitForDeployment();

  const address = await contract.getAddress();
  const deploymentTx = contract.deploymentTransaction();
  const owner = await contract.owner();

  const payload = {
    contractName: "ChainTrust",
    network: networkName,
    chainId: Number(network.chainId),
    address,
    owner,
    deployer: deployer.address,
    deploymentTxHash: deploymentTx ? deploymentTx.hash : null,
    deployedAt: new Date().toISOString()
  };

  const outDir = path.join(__dirname, "..", "deployments");
  fs.mkdirSync(outDir, { recursive: true });
  const outFile = path.join(outDir, `${networkName}.json`);
  fs.writeFileSync(outFile, JSON.stringify(payload, null, 2), "utf8");

  console.log("");
  console.log("Deployment complete.");
  console.log(`Contract address: ${address}`);
  console.log(`Deployment file : ${outFile}`);
  console.log("");
  console.log("Set backend .env values:");
  console.log(`CONTRACT_ADDRESS=${address}`);
  console.log(`BLOCKCHAIN_CHAIN_ID=${Number(network.chainId)}`);
  if (networkName === "localhost") {
    console.log("ETH_RPC_URL=http://host.docker.internal:8545");
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
