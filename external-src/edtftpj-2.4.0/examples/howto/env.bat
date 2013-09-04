@ECHO OFF
IF NOT DEFINED JAVA_HOME (
  REM ****************** 
  REM SET JAVA_HOME to the base directory of your JDK/JDE installation.
  SET JAVA_HOME=
  REM ******************
)
IF NOT EXIST "%JAVA_HOME%\bin\java.exe". (
  ECHO Could not find java.exe
  ECHO Please set the JAVA_HOME variable in the 'env.bat' batch file
  EXIT /B 1
)
IF NOT EXIST ..\..\..\lib\edtftpj.jar. (
  ECHO Error: Could not find 'edtftpj.jar'
  EXIT /B 1
)

SET CP=.;..\..\..\lib\edtftpj.jar
EXIT /B 0