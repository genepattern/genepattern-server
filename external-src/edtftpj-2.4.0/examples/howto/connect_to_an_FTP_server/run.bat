@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% ConnectToServer.java
  "%JAVA_HOME%\bin\java" -cp %CP% ConnectToServer %1 %2 %3
)