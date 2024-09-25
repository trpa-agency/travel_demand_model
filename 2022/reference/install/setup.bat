@echo off

::This version has the TC_PATH hard-coded.
:: use the command below to manually set the path to the folder containing tcw.exe

SET TC_PATH=C:\Program Files\TransCAD 9.0_32930

SETLOCAL ENABLEEXTENSIONS
SETLOCAL ENABLEDELAYEDEXPANSION

::set the batch file directory 
SET BD=%~dp0
set MD=%BD%..\..\

::compile listfile name 
SET LSTFILE=%BD%..\code\TahoeModel.lst

::kill transcad, if it is running
CALL:kill_process tcw.exe

::pause for a few seconds
ping 127.0.0.1 -n 3

::build ui
IF EXIST "%BD%..\code\ui\tahoemodelui.dbd" DEL /Q "%BD%..\code\ui\*.*"
"%TC_PATH%\rscc.exe" -c -u "%BD%..\code\ui\ui.dbd" "@%LSTFILE%"

SET MODEL_NAME=TRPA Travel Demand Model
SET NEW_AI_DESC=%MODEL_NAME%
SET NEW_AI_FILE=%BD%..\code\ui\ui.dbd
SET NEW_AI_MACRO=TahoeABModel

::copy dll dependencies, as needed
::CALL:copy_dll libgfortran-3.dll
::CALL:copy_dll libgcc_s_sjlj-1.dll

::create shortcut
echo "create shortcut"
SET TARGET='%TC_PATH%\tcw.exe'
SET ARGUMENTS=-q -a '%NEW_AI_FILE%' -ai '%NEW_AI_MACRO%' -n '%MODEL_NAME%'
SET DESCRIPTION=%NEW_AI_DESC%

::arguments are shortcut name, shortcut target, shortcut description, shortcut working directory, shortcut icon path
::cscript shortcut.vbs "%MD%" "%MODEL_NAME%" "%TARGET%" "%ARGUMENTS%" "%DESCRIPTION%" "%TC_PATH%" %ICON_PATH%
cscript shortcut.vbs "%MD%" "%MODEL_NAME%" "%TARGET%" "%ARGUMENTS%" "%DESCRIPTION%" "%TC_PATH%"

GOTO END

:kill_process
for /f "tokens=1,2" %%i in ('tasklist') do if %%i==%~1 TASKKILL /F /T /PID %%j
GOTO:EOF

:copy_dll
SET found=
FOR %%X in (%~1) do (set found=%%~$PATH:X)
IF NOT DEFINED found COPY %BD%..\lib\%~1 c:\Windows\System32\%~1
GOTO:EOF

:END
