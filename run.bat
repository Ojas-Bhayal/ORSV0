@echo off
setlocal enabledelayedexpansion

REM ============================================================================
REM ORSV0 Reproducible Build and Deployment Script (Phase 1)
REM Compiles current source, runs automated tests, produces target/ORSV0.war,
REM and deploys only when TOMCAT_HOME is explicitly configured.
REM ============================================================================

set "PROJECT_DIR=%~dp0"
pushd "%PROJECT_DIR%"

where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven ^(mvn^) was not found on PATH. Please install Maven or add it to PATH.
    popd
    exit /b 1
)

echo [INFO] Compiling current source and running tests...
call mvn clean test package
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed. Aborting.
    popd
    exit /b 1
)

if not exist "target\ORSV0.war" (
    echo [ERROR] target\ORSV0.war was not generated.
    popd
    exit /b 1
)

echo [INFO] Successfully built target\ORSV0.war ^(Build ID: ORS-2.1.1-RELEASE-20261015^).

if "%~1"=="--deploy" (
    if "%TOMCAT_HOME%"=="" (
        echo [ERROR] TOMCAT_HOME is not set. Please set TOMCAT_HOME before running with --deploy.
        popd
        exit /b 1
    )
    if not exist "%TOMCAT_HOME%\webapps" (
        echo [ERROR] "%TOMCAT_HOME%\webapps" does not exist.
        popd
        exit /b 1
    )
    echo [INFO] Deploying target\ORSV0.war to "%TOMCAT_HOME%\webapps\ORSV0.war" ...
    if exist "%TOMCAT_HOME%\webapps\ORSV0.war" del /f /q "%TOMCAT_HOME%\webapps\ORSV0.war"
    if exist "%TOMCAT_HOME%\webapps\ORSV0" rmdir /s /q "%TOMCAT_HOME%\webapps\ORSV0"
    copy /y "target\ORSV0.war" "%TOMCAT_HOME%\webapps\ORSV0.war" >nul
    echo [INFO] Deployment complete.
)

popd
endlocal
