REM USAGE: SPLENDID.sh <config> <query>
REM -----------------------------------

@ECHO OFF

set MAIN=de.uni_koblenz.west.federation.test.OptimizerTest

REM set classpath
set CP=bin

REM include jar files in classpath
for %%i in (lib\*.jar) do CALL :SETTER %%i

REM run SPLENDID
java -cp %CP% %MAIN% %*
goto :eof

:SETTER
set CP=%CP%;%1
goto :eof
