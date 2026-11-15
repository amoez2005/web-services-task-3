@echo off
setlocal
call "%~dp0use-java.cmd"
if errorlevel 1 exit /b 1
if not exist "%~dp0..\target\student-rest-service.jar" (
    call "%~dp0build.cmd"
    if errorlevel 1 exit /b 1
)
if "%~1"=="" (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0..\target\student-rest-service.jar"
) else (
    "%JAVA_HOME%\bin\java.exe" -jar "%~dp0..\target\student-rest-service.jar" "%~1"
)
exit /b %ERRORLEVEL%
