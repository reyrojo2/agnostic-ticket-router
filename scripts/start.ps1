param([switch]$WithAI)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    if (-not (Test-Path '.env')) { throw 'Copia .env.example a .env y completa las credenciales.' }
    docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop no esta iniciado.' }
    docker compose -p agnostic-espo -f compose.espocrm.yml up -d
    if ($LASTEXITCODE -ne 0) { throw 'Fallo al iniciar EspoCRM.' }
    $options = @('compose')
    if ($WithAI) { $options += @('--profile', 'ai') }
    $options += @('up', '-d', '--build')
    & docker @options
    if ($LASTEXITCODE -ne 0) { throw 'Fallo al iniciar la plataforma.' }
    Write-Host 'Angular: http://localhost:4200'
    Write-Host 'EspoCRM: http://localhost:8080'
    Write-Host 'RabbitMQ: http://localhost:15672'
    Write-Host 'Mailpit: http://localhost:8025'
}
finally { Pop-Location }
