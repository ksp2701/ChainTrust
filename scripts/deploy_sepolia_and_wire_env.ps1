param(
    [string]$RootDir = "."
)

$ErrorActionPreference = "Stop"

function Is-Placeholder([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $true
    }
    return $value.Contains("<") -or $value.Contains("YOUR_") -or $value.Contains("PROJECT_ID")
}

function Read-EnvMap([string]$path) {
    $map = [ordered]@{}
    if (!(Test-Path $path)) {
        throw "Env file not found: $path"
    }
    foreach ($line in Get-Content $path) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($line.TrimStart().StartsWith("#")) { continue }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1)
        $map[$key] = $val
    }
    return $map
}

function Upsert-Env([string]$path, [hashtable]$updates) {
    $lines = Get-Content $path
    foreach ($k in $updates.Keys) {
        $found = $false
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match "^$k=") {
                $lines[$i] = "$k=$($updates[$k])"
                $found = $true
                break
            }
        }
        if (-not $found) {
            $lines += "$k=$($updates[$k])"
        }
    }
    Set-Content -Path $path -Value $lines -Encoding UTF8
}

$root = (Resolve-Path $RootDir).Path
$envPath = Join-Path $root ".env"
$contractsDir = Join-Path $root "contracts"
$sepoliaDeployPath = Join-Path $contractsDir "deployments\\sepolia.json"

$envMap = Read-EnvMap $envPath

$rpc = $envMap["HARDHAT_SEPOLIA_RPC_URL"]
if (Is-Placeholder $rpc) {
    throw "HARDHAT_SEPOLIA_RPC_URL is missing/placeholder in .env"
}
$pk = $envMap["HARDHAT_DEPLOYER_PRIVATE_KEY"]
if (Is-Placeholder $pk) {
    throw "HARDHAT_DEPLOYER_PRIVATE_KEY is missing/placeholder in .env"
}

Write-Output "Deploying ChainTrust to Sepolia..."
$env:HARDHAT_SEPOLIA_RPC_URL = $rpc
$env:HARDHAT_DEPLOYER_PRIVATE_KEY = $pk

Push-Location $contractsDir
try {
    npm run deploy:sepolia
} finally {
    Pop-Location
}

if (!(Test-Path $sepoliaDeployPath)) {
    throw "Sepolia deployment file not found: $sepoliaDeployPath"
}

$deployJson = Get-Content $sepoliaDeployPath -Raw | ConvertFrom-Json
$address = $deployJson.address
if (Is-Placeholder $address) {
    throw "Invalid deployed contract address in $sepoliaDeployPath"
}

$updates = @{
    ETH_RPC_URL = $rpc
    BLOCKCHAIN_ENABLED = "true"
    BLOCKCHAIN_CHAIN_ID = "11155111"
    CONTRACT_ADDRESS = $address
    BLOCKCHAIN_PRIVATE_KEY = $pk
    WALLET_SYNTHETIC_FALLBACK_ENABLED = "false"
}
Upsert-Env -path $envPath -updates $updates

Write-Output ""
Write-Output "Sepolia deployment and env wiring complete."
Write-Output "CONTRACT_ADDRESS=$address"
Write-Output "ETH_RPC_URL=$rpc"
