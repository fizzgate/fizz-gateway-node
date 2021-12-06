@echo off

if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment. & EXIT /B 1

setlocal enabledelayedexpansion

set BASE_DIR=%~dp0
set APOLLO_META_SERVER=http://localhost:66
set APOLLO_ENV=prod
set SPRING_PROFILES_ACTIVE=prod
set APP_NAME=fizz-gateway-community
set APP_NAME_JAR=%APP_NAME%.jar
set APP_DEP_DIR=%BASE_DIR%
set APP_LOG_DIR=%APP_DEP_DIR%logs
set JAVA_CMD="%JAVA_HOME%\bin\java.exe"

IF NOT EXIST "%APP_LOG_DIR%" (
	MKDIR "%APP_LOG_DIR%"
)

if "%1" == "stop" goto stop
if "%1" == "status" goto status

if not "%1" == "start" if not "%1" == "restart" (
	echo "Usage: %0 {start|stop|restart|status}"
	goto end
)

rem JVM CONFIG
set MEM_OPTS=-Xms256m -Xmx4096m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m

set JAVA_OPTS=%MEM_OPTS% -XX:+AggressiveOpts -XX:+UseBiasedLocking -XX:+UseStringDeduplication -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:"%APP_LOG_DIR%\%START_DATE_TIME%.gc" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%APP_LOG_DIR%\dump.logs" -Dreactor.netty.pool.maxIdleTime=120000 -Dorg.jboss.netty.epollBugWorkaround=true

if "%1" == "start" goto start
if "%1" == "restart" goto stop

:status
for /f "tokens=1" %%i in ('jps -m ^| find /I "%APP_NAME%"') do (
	echo "%APP_NAME_JAR% is runing"
	goto end
)
echo "%APP_NAME_JAR% is not runing"
goto end

:stop
for /f "tokens=1" %%i in ('jps -m ^| find /I "%APP_NAME%"') do (
	goto doStop
)
echo "WARN:%APP_NAME_JAR% is not runing"
if "%1" == "restart" goto start
goto end

:doStop
echo "Stoping %APP_NAME_JAR%..."
for /f "tokens=1" %%i in ('jps -m ^| find "%APP_NAME%"') do ( taskkill /F /PID %%i )
if "%1" == "restart" goto start
goto end

:start
for /f "tokens=1" %%i in ('jps -m ^| find /I "%APP_NAME%"') do (
	echo "WARN:%APP_NAME_JAR% already started! Ignoring startup request."
	goto end
)

echo "Starting %APP_NAME_JAR% ..."
%JAVA_CMD% %JAVA_OPTS% -Dfile.encoding=UTF-8 -Dspring.profiles.active=%SPRING_PROFILES_ACTIVE% -Denv=%APOLLO_ENV% -Dapollo.meta=%APOLLO_META_SERVER% -Dlogging.config=log4j2-spring.xml -jar "%APP_DEP_DIR%%APP_NAME_JAR%"
goto end

:end
