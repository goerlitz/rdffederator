@ECHO OFF

set MAIN=de.uni_koblenz.west.federation.test.OptimizerTest

REM collect all jars
set JARS=.
for %%i in (lib\*.jar) do CALL :SETTER %%i

java -cp %JARS% %MAIN% %*
goto :eof

:SETTER
set JARS=%JARS%;%1
goto :eof
