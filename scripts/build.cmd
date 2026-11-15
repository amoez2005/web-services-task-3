@echo off
setlocal
call "%~dp0use-java.cmd"
if errorlevel 1 exit /b 1
pushd "%~dp0.."
call mvnw.cmd -B -ntp clean verify
set "STUDENT_BUILD_EXIT=%ERRORLEVEL%"
popd
exit /b %STUDENT_BUILD_EXIT%
