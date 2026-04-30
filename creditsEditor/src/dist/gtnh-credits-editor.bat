@echo off
setlocal
set "APP_HOME=%~dp0"
start "" javaw -jar "%APP_HOME%gtnh-credits-editor.jar" %*
endlocal