@echo off
setlocal enableextensions enabledelayedexpansion

set MAVEN_VERSION=3.9.6
set MAVEN_DIST=apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%MAVEN_DIST%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/%MAVEN_ZIP%
set MAVEN_CACHE=%USERPROFILE%\.m2\wrapper\dists\%MAVEN_DIST%
set MVN_CMD=%MAVEN_CACHE%\bin\mvn.cmd

if not exist "%MVN_CMD%" (
    echo [mvnw] Maven not cached, downloading %MAVEN_DIST%...
    if not exist "%MAVEN_CACHE%" mkdir "%MAVEN_CACHE%\.."
    powershell -NoProfile -Command ^
        "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '$env:TEMP\%MAVEN_ZIP%'; ^
         Expand-Archive -Path '$env:TEMP\%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force; ^
         Remove-Item '$env:TEMP\%MAVEN_ZIP%'"
    echo [mvnw] Maven downloaded.
)

if not exist "%MVN_CMD%" (
    echo [ERROR] Maven download failed. Please install Maven manually.
    exit /b 1
)

set JAVA_CMD=java
if defined JAVA_HOME set JAVA_CMD=%JAVA_HOME%\bin\java.exe

"%MVN_CMD%" %*
endlocal
