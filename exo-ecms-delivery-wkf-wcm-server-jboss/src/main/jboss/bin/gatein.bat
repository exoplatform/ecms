@echo off

rem Computes the absolute path of eXo
setlocal ENABLEDELAYEDEXPANSION
for %%i in ( !%~f0! ) do set BIN_DIR=%%~dpi
cd %BIN_DIR%

rem Sets some variables
set BONITA_OPTS="-Dorg.ow2.bonita.environment=..\server\default\conf\bonita.environnement.xml"
set LOGGING_OPTS="-Djava.util.logging.config.file=..\server\default\conf\logging.properties"
#set EXO_CONFIG_OPTS="-Dorg.exoplatform.container.configuration.debug"
#set JPDA_TRANSPORT=dt_socket
#set JPDA_ADDRESS=8000

rem Launches the server
call run.bat %*
