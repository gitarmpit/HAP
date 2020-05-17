@echo off 

call clenv.bat

SET DEFS=%DEFS_DEBUG%  
SET DEFS=%DEFS% /D _USRDLL /D _WINDLL /D DLLBUILD
SET OPTS=%OPTS_DEBUG% 

set DLL_NAME=MyDll
set OUT=_debug
if not exist %OUT% mkdir  %OUT%

cl.exe /c %DEFS% %OPTS% /Fa"%OUT%\\" /Fo"%OUT%\\" /Fd"%OUT%\VC142.PDB" %1 %2 %3

set SYSTEMLIBS="kernel32.lib" "user32.lib" "gdi32.lib" "winspool.lib"^
               "comdlg32.lib" "advapi32.lib" "shell32.lib" "ole32.lib"^
               "oleaut32.lib" "uuid.lib" "odbc32.lib" "odbccp32.lib"

set IMPLIB="%OUT%\%DLL_NAME%.lib"

set LINK_FLAGS=%LINK_FLAGS_DEBUG% /DLL

link.exe /OUT:"%OUT%\%DLL_NAME%.dll"^
 /PDB:"%OUT%\%DLL_NAME%.pdb" %SYSTEMLIBS% %LINK_FLAGS%^
 /IMPLIB:"%IMPLIB%" %OUT%\*.obj

