# Executa testes Maven no modulo glpi-integration
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
.\mvnw.cmd -B clean test
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Testes OK."
