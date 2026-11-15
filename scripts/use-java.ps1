# Select a JDK for this process without changing Windows settings.
$jdkCandidates = @()
if ($env:JAVA_HOME) {
    $jdkCandidates += $env:JAVA_HOME
}
$javacCommand = Get-Command javac -ErrorAction SilentlyContinue
if ($javacCommand) {
    $jdkCandidates += Split-Path (Split-Path $javacCommand.Source -Parent) -Parent
}
foreach ($jdkCandidate in $jdkCandidates | Select-Object -Unique) {
    $compiler = Join-Path $jdkCandidate 'bin\javac.exe'
    if (Test-Path -LiteralPath $compiler) {
        $compilerVersion = (& $compiler -version 2>&1 | Out-String).Trim()
        if ($compilerVersion -match 'javac (\d+)' -and [int]$Matches[1] -ge 17) {
            $env:JAVA_HOME = $jdkCandidate
            return
        }
    }
}
throw 'JDK 17 or newer is required. Set JAVA_HOME to a JDK directory or add its bin directory to PATH.'
