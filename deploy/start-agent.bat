@echo off
title Agent de Surveillance - Déploiement Simple
echo ========================================
echo    AGENT DE SURVEILLANCE
echo    Machine: %COMPUTERNAME%
echo ========================================
echo.

REM Vérifier Java
where java >nul 2>&1
if errorlevel 1 (
    echo ❌ ERREUR: Java non trouvé
    echo.
    echo SOLUTION: Installez Java 17 depuis:
    echo https://adoptium.net/temurin/releases/?version=17
    echo.
    pause
    exit /b 1
)

echo ✅ Java trouvé
echo.

REM Configuration
set /p SERVER_IP="Adresse IP du serveur [localhost]: "
if "%SERVER_IP%"=="" set SERVER_IP=localhost

echo.
echo 📊 Configuration:
echo    Serveur: %SERVER_IP%
echo    Ports: 9999 (UDP), 9998 (TCP)
echo.

REM Démarrer
echo 🚀 Démarrage de l'agent...
echo    Ctrl+C pour arrêter
echo ========================================
echo.
java -cp "classes" edu.ds.monitoring.agent.AgentMain %SERVER_IP%
pause
