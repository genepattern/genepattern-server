echo Setting path
copy /y %COMPUTERNAME%.bat setenv.bat
call setenv
path
del setenv.bat
rem
set CLASSPATH=..\lib\junit.jar;..\lib\edtftpj.jar
rem
rem use -Dedtftp.log.log4j=true for log4j logging
rem

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestReconnect

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestExists

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestGeneral

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestListings

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestFileOperations

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestDirOperations

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestFeatures

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestResume

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestBigTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestAutoModeTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestBulkTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestStream

@echo off
rem
rem 240 second pause to allow earlier TIME_WAITs to expire
rem
ping -n 240 localhost>NUL
@echo on

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=ACTIVE com.enterprisedt.net.ftp.test.TestPortRange

@echo off
rem
rem 240 second pause to allow earlier TIME_WAITs to expire
rem
ping -n 240 localhost>NUL
@echo on

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestGeneral

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestListings

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestFileOperations

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestDirOperations

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestFeatures

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestResume

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestBigTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestAutoModeTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestBulkTransfer

"%JAVA_HOME%\bin\java" -Dftptest.properties.filename=conf/test.properties -Dftptest.connectmode=PASV com.enterprisedt.net.ftp.test.TestStream


