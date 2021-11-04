rem ---------------------------------------------------------------------------
rem Append to CLASSPATH
rem
rem $Id: cpappend.bat 3160 2006-08-03 14:38:55Z liefeld $
rem ---------------------------------------------------------------------------

rem Process the first argument
if ""%1"" == """" goto end
set CLASSPATH=%CLASSPATH%;%1
shift

rem Process the remaining arguments
:setArgs
if ""%1"" == """" goto doneSetArgs
set CLASSPATH=%CLASSPATH% %1
shift
goto setArgs
:doneSetArgs
:end
