@ECHO OFF

set MAIN=de.uni_koblenz.west.splendid.statistics.void2.VoidGenerator

REM set classpath
set CP=..\bin

REM include jar files in classpath
for %%i in (..\lib\*.jar) do CALL :SETTER %%i

java -cp %CP% %MAIN% %*
goto :eof

:SETTER
set CP=%CP%;%1
goto :eof
