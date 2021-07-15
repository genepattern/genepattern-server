@call ..\env.bat
IF NOT ERRORLEVEL 1 (
  "%JAVA_HOME%\bin\javac" -classpath %CP% UploadDownloadFiles.java
  "%JAVA_HOME%\bin\java" -cp %CP% UploadDownloadFiles %1 %2 %3
)