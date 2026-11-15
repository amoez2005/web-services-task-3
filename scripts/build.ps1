$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'use-java.ps1')
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\mvnw.cmd -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verification failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
