@echo off 

SET VS=C:\Program Files (x86)\Microsoft Visual Studio
SET BASE=%VS%\2019\Community
SET VS14=%BASE%\VC\Tools\MSVC\14.21.27702
SET KIT=C:\Program Files (x86)\Windows Kits

SET INCLUDE=%VS14%\ATLMFC\include;%VS14%\include;^
%KIT%\NETFXSDK\4.6.1\include\um;^
%KIT%\10\include\10.0.17763.0\ucrt;^
%KIT%\10\include\10.0.17763.0\shared;^
%KIT%\10\include\10.0.17763.0\um;^
%KIT%\10\include\10.0.17763.0\winrt;^
%KIT%\10\include\10.0.17763.0\cppwinrt

SET LIB=%VS14%\ATLMFC\lib\x64;%VS14%\lib\x64;^
%KIT%\NETFXSDK\4.6.1\lib\um\x64;^
%KIT%\10\lib\10.0.17763.0\ucrt\x64;^
%KIT%\10\lib\10.0.17763.0\um\x64;

SET Path=%VS14%\bin\HostX64\x64;%KIT%\10\bin\10.0.17763.0\x64;%KIT%\10\bin\x64;^
%BASE%\MSBuild\Current\Bin;%BASE%\Common7\IDE\;%BASE%\Common7\Tools\;%PATH%

::==================
:: /TP  treat all files as CPP
:: /JMC -- just my code debugging, skips system calls 
:: /permissive-   standard conforming behavior
:: /GS buffer security check
:: /analyze-   disable code analysys
:: /W3 warning level=production quality
:: /Zc:wchar_t  wchar is a native type
:: /ZI  -- but it produces a PDB file in a format that supports the Edit and Continue feature
:: /Gm-  disable minimal rebuild 
:: /Od -- Turns off all optimizations in the program and speeds compilation. 
:: /sdl-  additional security checks (on top of /GS)
:: /Zc:inline (remove unreferenced COMDAT, size reduction and build speedup) 
:: /fp:precise floating point behavior
:: /WX-  treat warnings as errors
:: /Zc:forScope  let a for loop's initializer go out of scope after the for loop.
:: /RTC1  runtime checks  (debug only) 
:: /Gd  -- calling convention _cdecl which is the default 
:: /Oy-  suppress frame pointer omission (debug is /Oy- which means do not suppress) 
:: /FC   -- full path of the source code in debug version diagnostics
:: /EHsc  -- exception handling model s: enables standard stack unwinding,  c: assumes extern "C" never throws C++ ex
:: /Fp"Debug\MyDll.pch"   path for precompiled header

:: Release: 
:: /GL  whole program optimization
:: /Gy function level linking
:: /Oi  generate intrinsic function
:: /Oy frame pointer omission

:: RT lib version:  /MD /MDd, static: /MT /MTd, /LD: dll   

:: Macro
:: NDEBUG : disables assert: #define assert(ignore) ((void)0) 

SET DEFS=/D _UNICODE /D UNICODE /D _WINDOWS
SET OPTS=/EHsc /fp:precise /Gm- /GS /nologo /permissive- /Zc:forScope /Zc:inline /Zc:wchar_t /W3 /WX- 

::debug
SET DEFS_DEBUG=%DEFS%  /D _DEBUG    
SET DEFS_RELEASE=%DEFS%  /D NDEBUG

:: debug 
SET OPTS_DEBUG=%OPTS% /MDd /Od /RTC1 /sdl /Zi 
:: release
SET OPTS_RELEASE=%OPTS% /GL /Gy /O2 /Oi /MD /sdl- /Zi 

set LINK_FLAGS=/INCREMENTAL:NO /NOLOGO /MANIFEST /MANIFESTUAC:NO^
 /manifest:embed /MACHINE:X64 /DYNAMICBASE /NXCOMPAT /SUBSYSTEM:CONSOLE
::/SUBSYSTEM:WINDOWS

set LINK_FLAGS_DEBUG=%LINK_FLAGS% /DEBUG:FULL
set LINK_FLAGS_RELEASE=%LINK_FLAGS%
