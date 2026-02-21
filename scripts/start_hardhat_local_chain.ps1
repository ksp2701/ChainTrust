param(
    [switch]$RestartBackend = $true
)

$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..")
$Contracts = Join-Path $Root "contracts"
$EnvPath = Join-Path $Root ".env"
$HardhatLog = Join-Path $Contracts "hardhat-node.log"
$DefaultDevPk = "ac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"

function Set-EnvValue {
    param(
        [string]$FilePath,
        [string]$Key,
        [string]$Value
    )

    if (-not (Test-Path $FilePath)) {
        throw "Env file not found: $FilePath"
    }

    $content = Get-Content $FilePath
    $pattern = "^\s*$([Regex]::Escape($Key))="
    $line = "$Key=$Value"
    $found = $false
    $updated = foreach ($row in $content) {
        if ($row -match $pattern) {
            $found = $true
            $line
        } else {
            $row
        }
    }
    if (-not $found) {
        $updated += $line
    }
    Set-Content -Path $FilePath -Value $updated -Encoding utf8
}

Push-Location $Contracts
try {
    npm install
    npm run compile

    $listener = Get-NetTCPConnection -LocalPort 8545 -State Listen -ErrorAction SilentlyContinue
    if (-not $listener) {
        if (Test-Path $HardhatLog) {
            Remove-Item $HardhatLog -Force
        }
        Start-Process `
            -FilePath "C:\windows\System32\WindowsPowerShell\v1.0\powershell.exe" `
            -ArgumentList "-NoLogo", "-NoProfile", "-Command", "Set-Location '$Contracts'; npx hardhat node --hostname 0.0.0.0 --port 8545 *>&1 | Tee-Object -FilePath hardhat-node.log" `
            -WindowStyle Hidden

        Start-Sleep -Seconds 4
    }

    npm run deploy:local
} finally {
    Pop-Location
}

$deploymentFile = Join-Path $Contracts "deployments\localhost.json"
if (-not (Test-Path $deploymentFile)) {
    throw "Deployment output missing: $deploymentFile"
}

$deployment = Get-Content $deploymentFile | ConvertFrom-Json
$contractAddress = [string]$deployment.address
if (-not $contractAddress) {
    throw "Missing deployed contract address in $deploymentFile"
}

Set-EnvValue -FilePath $EnvPath -Key "ETH_RPC_URL" -Value "http://host.docker.internal:8545"
Set-EnvValue -FilePath $EnvPath -Key "CONTRACT_ADDRESS" -Value $contractAddress
Set-EnvValue -FilePath $EnvPath -Key "BLOCKCHAIN_ENABLED" -Value "true"
Set-EnvValue -FilePath $EnvPath -Key "BLOCKCHAIN_CHAIN_ID" -Value "31337"
Set-EnvValue -FilePath $EnvPath -Key "BLOCKCHAIN_PRIVATE_KEY" -Value $DefaultDevPk

if ($RestartBackend) {
    Push-Location $Root
    try {
        docker compose up -d backend
    } finally {
        Pop-Location
    }
}

Write-Host "Hardhat local chain active."
Write-Host "Contract address: $contractAddress"
Write-Host "Updated .env and backend blockchain settings."
