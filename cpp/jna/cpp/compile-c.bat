set PATH=c:\cygwin64\bin;%PATH%

g++ -c -m64 test.cpp
g++ -shared -m64 -o test.dll test.o
                                           
