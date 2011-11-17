@ECHO OFF
REM run SPLENDID federation test

set MAIN=de.uni_koblenz.west.federation.test.OptimizerTest

REM set classpath
set CP=..\bin

REM include jar files in classpath
for %%i in (..\lib\*.jar) do CALL :SETTER %%i

java -cp %CP% %MAIN% %*
goto :eof

:SETTER
set CP=%CP%;%1
goto :eof
