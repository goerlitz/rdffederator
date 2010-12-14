@ECHO OFF

REM collect all jars
set JARS=.
for %%i in (..\lib\*.jar) do CALL :SETTER %%i

PATH=%PATH%;binlib
java -cp %JARS% de.uni_koblenz.west.federation.test.OptimizerTest %*
goto :eof

:SETTER
set JARS=%JARS%;%1
goto :eof 
