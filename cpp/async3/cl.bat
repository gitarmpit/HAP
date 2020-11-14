set VER=10.0
set VCPATH=C:\Program Files (x86)\Microsoft Visual Studio %VER%\VC
set PATH=%VCPATH%;%VCPATH%\bin;%PATH%

call vcvarsall.bat

::cl.exe /Fo.\obj\ %1 %2 %3 %4 %5 %6 %7 %8 -o win.exe

cl.exe /Fo.\obj\ /W4 -D _CRT_SECURE_NO_WARNINGS src\*.cpp -o win.exe