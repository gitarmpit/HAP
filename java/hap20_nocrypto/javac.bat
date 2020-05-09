@echo off 


set PATH=C:\Program Files\Java\jdk1.8.0_144\bin;C:\Program Files\Java\jdk1.8.0_77\bin;%PATH%

dir /s /B *.java > sources.txt
javac.exe  -cp .  @sources.txt  -d bin

