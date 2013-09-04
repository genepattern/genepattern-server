@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  %JAVA_HOME%\bin\javac -classpath %CP% SetupLogging.java
  %JAVA_HOME%\bin\java -cp %CP% SetupLogging
)