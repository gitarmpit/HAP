@echo off 

:: 64 bit
set PATH="C:\Program Files\Java\jdk1.8.0_251\bin";%PATH%

:: 32 bit
:: set PATH="C:\Program Files\Java\java-se-8u40-ri\bin";%PATH%

javac.exe Test.java -cp .;jna-5.5.0.jar
 


