@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% UseConnectModes.java
  "%JAVA_HOME%\bin\java" -cp %CP% UseConnectModes %1 %2 %3
)