@echo off
rem Called by launchers inside SETLOCAL; no machine-wide settings are changed.
if defined JAVA_HOME (
    call :try_jdk "%JAVA_HOME%"
    if not errorlevel 1 exit /b 0
)
for /f "delims=" %%J in ('where javac.exe 2^>nul') do (
    for %%D in ("%%~dpJ..") do call :try_jdk "%%~fD"
    if not errorlevel 1 exit /b 0
)
echo JDK 17 or newer is required. Set JAVA_HOME or put its bin directory on PATH. 1>&2
exit /b 1

:try_jdk
if not exist "%~1\bin\javac.exe" exit /b 1
"%~1\bin\javac.exe" --release 17 -version >nul 2>&1
if errorlevel 1 exit /b 1
set "JAVA_HOME=%~1"
exit /b 0
