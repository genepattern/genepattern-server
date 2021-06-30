@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% MonitorTransfersCommands.java
  "%JAVA_HOME%\bin\java" -cp %CP% MonitorTransfersCommands %1 %2 %3
)