@echo off 


set PATH="C:\Program Files\Java\java-se-8u40-ri\bin";%PATH%

dir /s /B *.java > sources.txt
javac.exe  -cp .  @sources.txt  -d bin

