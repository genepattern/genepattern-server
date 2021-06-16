@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% ChangeDirectory.java
  "%JAVA_HOME%\bin\java" -cp %CP% ChangeDirectory %1 %2 %3 %4
)