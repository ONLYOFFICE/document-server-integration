ECHO OFF

ECHO.
ECHO ----------------------------------------
ECHO Install node.js modules 
ECHO ----------------------------------------
call npm install

ECHO.
ECHO ----------------------------------------
ECHO Run server 
ECHO ----------------------------------------
SET NODE_CONFIG_DIR=%~dp0\config
start /min /b node bin/www