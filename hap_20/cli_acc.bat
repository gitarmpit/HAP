@echo off 

set PATH=C:\Program Files\Java\jdk1.8.0_144\bin;C:\Program Files\Java\jdk1.8.0_77\bin;%PATH%

:1
java.exe -cp bin  cli.CliAcc "192.168.1.141"
::goto 1