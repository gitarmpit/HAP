@echo off 

set PATH=C:\Program Files\Java\jdk1.8.0_144\bin;C:\Program Files\Java\jdk1.8.0_77\bin;%PATH%

:loop
echo %TIME%
java.exe -cp bin  cli.Cli
goto loop
