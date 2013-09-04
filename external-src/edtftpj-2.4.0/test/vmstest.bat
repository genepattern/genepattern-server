echo Setting path
copy /y %COMPUTERNAME%.bat setenv.bat
call setenv
path
del setenv.bat
rem
set CLASSPATH=..\lib\junit.jar;..\lib\edtftpj.jar

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.vms.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.VMSTests  

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.vms.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.VMSTests  
