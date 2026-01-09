@echo off
echo Agent de Surveillance - Démarrage...
echo.

REM Vérifier si Java est installé
where java >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Java non trouvé.
    echo Installez Java 17 depuis: https://adoptium.net
    pause
    exit
)

REM Démarrer l'agent
java -cp "target/classes" edu.ds.monitoring.agent.AgentMain

pause