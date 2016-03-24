ECHO OFF

ECHO.
ECHO ----------------------------------------
ECHO Install node.js modules 
ECHO ----------------------------------------
call npm install


start /min /b node bin/www
pause