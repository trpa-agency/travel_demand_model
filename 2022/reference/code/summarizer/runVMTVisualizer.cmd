call ..\..\..\python_path.bat
SET codePath="."
SET run_log="./vmtRunSummary.log"
set scen_name=%1
ECHO %PYTHON_PATH% > %run_log% 2>&1
echo %cd% >> %run_log% 2>&1
%PYTHON_PATH%\python.exe %codePath%/vmt_summarizer.py %scen_name% >> %run_log% 2>&1
