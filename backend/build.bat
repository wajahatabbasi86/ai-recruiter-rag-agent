@echo off
setlocal

echo.
echo ============================================================
echo  AI Recruiter — Build Script (Windows)
echo ============================================================
echo.

echo [0/3] Stopping existing app container only...
docker-compose stop app
docker-compose rm -f app
echo Done. Infrastructure containers (postgres, qdrant, ollama) kept running.

echo.
echo [1/3] Compiling with Maven (using cached .m2 volume)...
docker-compose --profile build run --rm maven-build
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven build failed. Check logs above.
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Building Docker image...
docker-compose build app
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker image build failed. Check logs above.
    exit /b %ERRORLEVEL%
)

echo.
echo [3/3] Starting all services...
docker-compose up -d
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to start services. Check logs above.
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================================
echo  Done! App running at http://localhost:8080
echo  Logs: docker-compose logs -f app
echo ============================================================
endlocal