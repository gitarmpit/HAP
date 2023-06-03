@echo off 

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_77


set PATH=%PATH%;%JAVA_HOME%\bin
dir /s /B *.java > sources.txt
javac.exe -cp jars\netty-all-4.0.32.Final.jar;.  @sources.txt  -d bin

