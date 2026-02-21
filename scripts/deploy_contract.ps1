param(
    [ValidateSet("localhost", "sepolia")]
    [string]$Network = "localhost"
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Contracts = Join-Path $Root "contracts"

if (-not (Test-Path (Join-Path $Contracts "package.json"))) {
    throw "Hardhat workspace not found at $Contracts"
}

Push-Location $Contracts
try {
    npm install
    npm run compile

    if ($Network -eq "localhost") {
        npx hardhat run scripts/deploy.js --network localhost
    } elseif ($Network -eq "sepolia") {
        npx hardhat run scripts/deploy.js --network sepolia
    } else {
        throw "Unsupported network: $Network"
    }
} finally {
    Pop-Location
}
