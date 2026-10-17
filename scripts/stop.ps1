$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    docker compose --profile ai stop
    docker compose -p agnostic-espo -f compose.espocrm.yml stop
    Write-Host 'Contenedores detenidos; volumenes intactos.'
}
finally { Pop-Location }
