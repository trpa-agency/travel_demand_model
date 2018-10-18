@echo off
::trpa_setup.bat [installation_name=TRPA Model]

::get argument count
SET argc=0
FOR %%x in (%*) DO SET /A argc+=1

SETLOCAL ENABLEEXTENSIONS
SETLOCAL ENABLEDELAYEDEXPANSION

::set the batch file directory
SET BD=%~dp0

::put the current directory, plus extra directories, in front of the files in the resource list
SET LSTFILE=%BD%..\TahoeModel.lst
IF EXIST %LSTFILE% DEL %LSTFILE%
TYPE nul >%LSTFILE%
FOR /f "tokens=* delims= eol=/" %%a in (%BD%..\TahoeModel_.lst) do (
ECHO %BD%..\%%a >>%LSTFILE%
)

::set the model directory; it is the third directory behind this one
:: first save the current directory, change directories to get absolute path, then go back
:: save current directory
pushd .
cd %BD%..\..\..
set MD=%CD%
:: restore current directory
popd

SET TF=%BD%..\gisdk\EntryTemplate.rsc
SET TFN=%BD%..\gisdk\Entry.rsc
SET REPLACE_TOKEN=%MD:\=\\%
SET TOKEN=__model_dir__
CALL detemplify.bat %TOKEN% %REPLACE_TOKEN% %TF% > %TFN%

::now find TransCAD 4.8 installation directory
SET TC_PATH=C:\Program Files\TransCAD
IF NOT EXIST "%TC_PATH%\rscc.exe" SET TC_PATH=C:\Program Files (x86)\TransCAD
:TA
IF EXIST "%TC_PATH%\rscc.exe" GOTO TAD
SET /P TC_PATH=Enter TransCAD 4.8 directory:
for /f "useback tokens=*" %%a in ('%TC_PATH%') do set TC_PATH=%%~a
IF NOT EXIST "%TC_PATH%\rscc.exe" GOTO TA
:TAD
FOR /F %%A IN ('cscript //nologo "%BD%filever.vbs" "%TC_PATH%\tcw.exe"') DO (
SET TC_VER=%%A
GOTO TADONE
)
:TADONE
SET TC_VER=%TC_VER:~0,3%
IF %TC_VER%==4.8 GOTO TAFINE
ECHO Model requires TransCAD version 4.8, not %TC_VER%
SET TC_PATH=C:\
GOTO TA

:TAFINE


::kill transcad, if it is running
CALL:kill_process tcw.exe

::pause for a few seconds
ping 127.0.0.1 -n 3

::build ui
IF EXIST "%BD%..\ui\tahoemodelui.dbd" DEL /Q "%BD%..\ui\*.*"
"%TC_PATH%\rscc.exe" -c -u "%BD%..\ui\tahoemodelui.dbd" "@%LSTFILE%"

SET MODEL_NAME=Tahoe Activity Based Travel Demand Model

::add to add-ins
SET AI_FILENAME=Add-Ins.txt
SET AI_FILENAME_BAK=Add-Ins.txt.backup
SET AI_FILEPATH=%TC_PATH%\%AI_FILENAME%
SET AI_FILEPATH_BAK=%TC_PATH%\%AI_FILENAME_BAK%
IF EXIST "%AI_FILEPATH_BAK%" DEL "%AI_FILEPATH_BAK%"
RENAME "%AI_FILEPATH%" %AI_FILENAME_BAK%
SET NEW_AI_DESC=Tahoe Activity Based Travel Demand Model (Ver. 2.0)
SET NEW_AI_FILE=%BD%..\ui\tahoemodelui.dbd
SET NEW_AI_MACRO=TahoeABModel

TYPE nul >"%AI_FILEPATH%"
FOR /F "tokens=1-4 delims=," %%A IN ('TYPE "%TC_PATH%\%AI_FILENAME_BAK%"') DO (
    SET AI_TYPE=%%A
    SET AI_FILE=%%B
    SET AI_MACRO=%%C
    SET AI_DESC=%%D
REM     IF DEFINED AI_DESC IF NOT "!AI_DESC!"=="%NEW_AI_DESC% " ECHO !AI_TYPE!,!AI_FILE!,!AI_MACRO!,!AI_DESC!>>"%AI_FILEPATH%"
    IF DEFINED AI_MACRO IF NOT "!AI_DESC!"=="%NEW_AI_DESC%" ECHO !AI_TYPE!,!AI_FILE!,!AI_MACRO!,!AI_DESC!>>"%AI_FILEPATH%"
REM    IF DEFINED AI_MACRO IF "!AI_FILE!"=="%NEW_AI_FILE%" IF NOT "!AI_MACRO!"=="%NEW_AI_MACRO%" ECHO !AI_TYPE!,!AI_FILE!,!AI_MACRO!,!AI_DESC!>>"%AI_FILEPATH%"
)
ECHO M,%NEW_AI_FILE%,%NEW_AI_MACRO%,%NEW_AI_DESC%>>"%AI_FILEPATH%"

SET VM_SIZE=1400
if defined ValueName64 set VM_SIZE=1200
SET CONFIG_FILE=TahoeModelRunnerBackup_V2.txt
SET CONFIG_FILEPATH=%TC_PATH%\%CONFIG_FILE%
IF EXIST "%CONFIG_FILEPATH%" DEL "%CONFIG_FILEPATH%"
TYPE nul >"%CONFIG_FILEPATH%"
ECHO.>>"%CONFIG_FILEPATH%"
ECHO 1 >>"%CONFIG_FILEPATH%"
ECHO %BD%..\..\..\>>"%CONFIG_FILEPATH%"
ECHO 50 >>"%CONFIG_FILEPATH%"
ECHO 3 >>"%CONFIG_FILEPATH%"
ECHO.>>"%CONFIG_FILEPATH%"
ECHO %VM_SIZE%>>"%CONFIG_FILEPATH%"

::write summarizer file
SET SUM_FILE=%BD%..\summarizer\backup_file.txt
TYPE nul >"%SUM_FILE%
ECHO %CONFIG_FILEPATH% >>"%SUM_FILE%"

::create shortcut
SET TARGET='%TC_PATH%\tcw.exe'
SET ARGUMENTS=-q -a '%NEW_AI_FILE%' -ai '%NEW_AI_MACRO%' -n '%MODEL_NAME%'
SET DESCRIPTION=%MODEL_NAME%
::SET ICON_PATH=TODO
::arguments are shortcut name, shortcut target, shortcut description, shortcut working directory, shortcut icon path
::cscript shortcut.vbs "%MD%" "%MODEL_NAME%" "%TARGET%" "%ARGUMENTS%" "%DESCRIPTION%" "%TC_PATH%" %ICON_PATH%
cscript %BD%shortcut.vbs "%MD%" "%MODEL_NAME%" "%TARGET%" "%ARGUMENTS%" "%DESCRIPTION%" "%TC_PATH%"

::ask to move other scenarios
SET MVSCEN=N
SET OLD_CONFIG_FILEPATH=%TC_PATH%\TahoeModelRunnerBackup.txt
IF EXIST "%OLD_CONFIG_FILEPATH%" SET /P MVSCEN=Do you wish to move your scenarios from your previous model installation? (y/n):
IF %MVSCEN%==y SET MVSCEN=Y
IF NOT %MVSCEN%==Y GOTO END
ECHO %MVSCEN%
ECHO %OLD_CONFIG_FILEPATH%

FOR /F "skip=2 tokens=*" %%a in ('TYPE "%OLD_CONFIG_FILEPATH%"') DO (
SET OLD_INSTALL_PATH=%%~a
GOTO move_scenarios
)

:move_scenarios
@echo on
SET OSCEN_LIST=%OLD_INSTALL_PATH%reference\code\scenario_list.txt
SET SCEN_LIST=%BD%..\..\..\reference\code\scenario_list.txt
COPY /Y "%OSCEN_LIST%" "%OLD_INSTALL_PATH%reference\code\scenario_list_backup.txt"
COPY /Y "%SCEN_LIST%" "%BD%..\..\..\reference\code\scenario_list_backup.txt"
TYPE nul >"%OSCEN_LIST%"
FOR /d %%i in ("%OLD_INSTALL_PATH%scenarios\*") DO (
IF EXIST "%BD%..\..\..\scenarios\%%~ni" ECHO %%~ni>>"%OSCEN_LIST%"
IF NOT EXIST "%BD%..\..\..\scenarios\%%~ni" ECHO %%~ni>>"%SCEN_LIST%"
IF NOT EXIST "%BD%..\..\..\scenarios\%%~ni" MOVE "%%~i" "%BD%..\..\..\scenarios\%%~ni"
)

GOTO END

:kill_process
for /f "tokens=1,2" %%i in ('tasklist') do if %%i==%~1 TASKKILL /F /T /PID %%j
GOTO:EOF

:END
