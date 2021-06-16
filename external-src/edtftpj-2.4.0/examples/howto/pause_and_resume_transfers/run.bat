@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% PauseResumeTransfers.java
  "%JAVA_HOME%\bin\java" -cp %CP% PauseResumeTransfers %1 %2 %3
)