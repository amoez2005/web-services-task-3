param([string]$BaseUri = 'http://localhost:8085/api/')

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'use-java.ps1')
$projectRoot = Split-Path $PSScriptRoot -Parent
$jarPath = Join-Path $projectRoot 'target\student-rest-service.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    & (Join-Path $PSScriptRoot 'build.ps1')
}
& (Join-Path $env:JAVA_HOME 'bin\java.exe') -jar $jarPath $BaseUri
if ($LASTEXITCODE -ne 0) {
    throw "The student API exited with code $LASTEXITCODE."
}
