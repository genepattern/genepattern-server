@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% GetDirectoryListing.java
  "%JAVA_HOME%\bin\java" -cp %CP% GetDirectoryListing %1 %2 %3
)