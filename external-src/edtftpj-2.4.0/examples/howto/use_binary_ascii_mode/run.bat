@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% UseTransferModes.java
  "%JAVA_HOME%\bin\java" -cp %CP% UseTransferModes %1 %2 %3
)