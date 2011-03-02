@echo off

rem Computes the absolute path of eXo
setlocal ENABLEDELAYEDEXPANSION
for %%i in ( !%~f0! ) do set BIN_DIR=%%~dpi
cd %BIN_DIR%

rem Sets some variables
set BONITA_OPTS="-Dorg.ow2.bonita.environment=..\server\default\conf\bonita.environnement.xml"
set LOG_OPTS="-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog"
set LOGGING_OPTS="-Djava.util.logging.config.file=..\server\default\conf\logging.properties"
#set EXO_CONFIG_OPTS="-Dorg.exoplatform.container.configuration.debug"
set EXO_OPTS="-Dexo.product.developing=false"
#set JPDA_TRANSPORT=dt_socket
#set JPDA_ADDRESS=8000
set JAVA_OPTS=-Xshare:auto -Xms128m -Xmx512m -XX:MaxPermSize=256m %LOG_OPTS% %EXO_OPTS% %EXO_CONFIG_OPTS% %BONITA_OPTS% %LOGGING_OPTS%
rem Launches the server
call run.bat %*
