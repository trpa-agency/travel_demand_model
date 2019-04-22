SET rPath="../R-3.5.1/bin"
SET codePath="."
SET run_log="./runSummary.log"
set scen_name=%1

echo %cd% > %run_log% 2>&1
%rPath%\Rscript.exe --vanilla --verbose %codePath%/model_summarizer.R %scen_name% >> %run_log% 2>&1
