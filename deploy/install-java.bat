@echo off
echo Installation de Java 17 si nécessaire...
echo.
echo 1. Vérification de Java actuel...
java -version 2>nul
if %errorlevel% equ 0 (
    echo ✅ Java est déjà installé
    pause
    exit /b 0
)

echo.
echo 2. Téléchargement de Java 17...
echo    Lien: https://adoptium.net/temurin/releases/?version=17
echo.
echo 3. Instructions:
echo    a) Suivre le lien ci-dessus
echo    b) Télécharger "Windows x64 MSI Installer"
echo    c) Exécuter le fichier .msi
echo    d) Redémarrer l'ordinateur
echo.
echo 4. Après installation, relancer start-agent.bat
echo.
pause
