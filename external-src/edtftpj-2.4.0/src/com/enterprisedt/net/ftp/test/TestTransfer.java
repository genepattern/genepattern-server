/**
 *
 *  Java FTP client library.
 *
 *  Copyright (C) 2000  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be should posted on 
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *        $Log: TestTransfer.java,v $
 *        Revision 1.17  2012/11/06 04:12:13  bruceb
 *        stream changes - extract TestProgressMonitor out
 *
 *        Revision 1.16  2009-07-30 03:49:52  bruceb
 *        add new test
 *
 *        Revision 1.15  2008-05-02 07:42:12  bruceb
 *        force quit in finally
 *
 *        Revision 1.14  2007-12-18 07:55:50  bruceb
 *        add finally
 *
 *        Revision 1.13  2007/04/21 04:14:12  bruceb
 *        test unix text files transferred
 *
 *        Revision 1.12  2007/02/26 07:14:34  bruceb
 *        make progress monitor public
 *
 *        Revision 1.11  2007/02/06 07:23:40  bruceb
 *        testNoLocalFilename()
 *
 *        Revision 1.10  2007/01/10 02:39:11  bruceb
 *        added testTransferUnique
 *
 *        Revision 1.9  2006/10/11 08:59:54  hans
 *        Organised imports.
 *
 *        Revision 1.8  2005/10/10 20:43:39  bruceb
 *        append now in FTPClientInterface
 *
 *        Revision 1.7  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.6  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.5  2004/08/31 10:44:49  bruceb
 *        minor tweaks re compile warnings
 *
 *        Revision 1.4  2004/05/01 17:05:43  bruceb
 *        Logger stuff added
 *
 *        Revision 1.3  2003/11/03 21:18:51  bruceb
 *        added test of progress callback
 *
 *        Revision 1.2  2003/05/31 14:54:05  bruceb
 *        cleaned up unused imports
 *
 *        Revision 1.1  2002/11/19 22:00:15  bruceb
 *        New JUnit test cases
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Test get'ing and put'ing of remote files in various ways
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.17 $
 */
public class TestTransfer extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestTransfer.java,v 1.17 2012/11/06 04:12:13 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestTransfer.log";
    }

    /**
     *  Test transferring a binary file
     */
    public void testTransferBinary() throws Exception {

        log.info("testTransferBinary()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
    
            // get it back        
            ftp.get(localDataDir + filename, filename);
    
            // delete remote file
            ftp.delete(filename);
            try {
                ftp.modtime(filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    
    /**
     *  Test transferring using the server to generate a unique
     *  file name
     */
    public void testTransferUnique() throws Exception {

        log.info("testTransferUnique()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = ftp.put(localDataDir + localBinaryFile, null);
            log.info("Put file to '" + filename + "'");
    
            // get it back        
            ftp.get(localDataDir + filename, filename);
    
            // delete remote file
            ftp.delete(filename);
            try {
                ftp.modtime(filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    /**
     *  Test transfering by only supplying a local dir, not a
     *  full pathname
     */
    public void testNoLocalFilename() throws Exception {

        log.info("testNoLocalFilename()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            log.info("Put file to '" + filename + "'");
    
            // get it back        
            ftp.get(localDataDir, filename);
    
            // delete remote file
            ftp.delete(filename);
            try {
                ftp.modtime(filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }

    /**
     *  Test transfering a text file
     */
    public void testTransferText() throws Exception {

        log.info("testTransferText()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.ASCII);
    
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localTextFile, filename);
    
            // get it back        
            ftp.get(localDataDir + filename, filename);
    
            // delete remote file
            ftp.delete(filename);
            try {
                ftp.modtime(filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
    
            // check equality of local files
            assertIdentical(localDataDir + localTextFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    
    /**
     *  Test transfering a text file
     */
    public void testTransferUnixText() throws Exception {

        log.info("testTransferUnixText()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.ASCII);
    
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localUnixTextFile, filename);
    
            // get it back        
            ftp.get(localDataDir + filename, filename);
            
    
            // check equality of local files - against the equivalent local text file
            // not the transferred unix one
            assertIdentical(localDataDir + localTextFile, localDataDir + filename);
    
            // delete remote file
            ftp.delete(filename);
            try {
                ftp.modtime(filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    
    /**
     *  Test getting a byte array
     */
    public void testGetBytes() throws Exception {

        log.info("testGetBytes()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
    
            // get the file and work out its size
            String filename1 = generateRandomFilename();
            ftp.get(localDataDir + filename1, remoteBinaryFile);
            File file1 = new File(localDataDir + filename1);
            long len = file1.length();
    
            // now get to a buffer and check the length
            byte[] result = ftp.get(remoteBinaryFile);
            assertTrue(result.length == len);
            
            // put the buffer         
            String filename2 = generateRandomFilename();
            ftp.put(result, filename2);
            
            // get it back as a file
            ftp.get(localDataDir + filename2, filename2);
            
            // remove it remotely
            ftp.delete(filename2);
            
            // and now check files are identical
            File file2 = new File(localDataDir + filename2);
            assertIdentical(file1, file2);
    
            // and finally delete them
            file1.delete();
            file2.delete();
            
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }

    /**
     *  Test the stream functionality
     */
    public void testTransferStream() throws Exception {

        log.info("testTransferStream()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
    
            // get file as output stream        
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ftp.get(out, remoteBinaryFile);
    
            // convert to byte array
            byte[] result1 = out.toByteArray();
    
            // put this 
            String filename = generateRandomFilename();
            ftp.put(new ByteArrayInputStream(result1), filename);
    
            // get it back
            byte[] result2 = ftp.get(filename);
    
            // delete remote file
            ftp.delete(filename);
    
            // and compare the buffers
            assertIdentical(result1, result2);
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }

    /**
     *  Test the append functionality in put()
     */
    public void testPutAppend() throws Exception {

        log.info("testPutAppend()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
    
            // second time, append if possible
            int count = 1;
            ftp.put(localDataDir + localBinaryFile, filename, true);
            count++;
    
            // get it back & delete remotely
            ftp.get(localDataDir + filename, filename);
            ftp.delete(filename);
    
            // check it is the right size
            
            File file1 = new File(localDataDir + localBinaryFile);
            File file2 = new File(localDataDir + filename);
            assertTrue(file1.length()*count == file2.length());
            log.info(localBinaryFile + " length=" + file1.length() + ", " + filename + " length=" + file2.length());
    
            // and finally delete it
            file2.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    /**
     *  Test overwriting a remote file with a smaller file
     */
    public void testOverwrite() throws Exception {

        log.info("testOverwrite()");
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);        
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
    
            // second time, append
            ftp.put(localDataDir + localBinaryFile, filename, true);
             
            // check it is twice the size
            File file1 = new File(localDataDir + localBinaryFile);
            assertTrue(file1.length()*2 == ftp.size(filename));
            
            // now put it again without appending
            ftp.put(localDataDir + localBinaryFile, filename);
    
            // get it back & delete remotely
            ftp.get(localDataDir + filename, filename);
            ftp.delete(filename);
    
            // check it is the right size          
            File file2 = new File(localDataDir + filename);
            assertTrue(file1.length() == file2.length());
            log.info(localBinaryFile + " length=" + file1.length() + ", " + filename + " length=" + file2.length());
    
            // and finally delete it
            file2.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }

    /**
     *  Test transferring empty files
     */
    public void testTransferEmpty() throws Exception {

        log.info("testTransferEmpty()");
        
        try {

            connect();
     
            // move to test directory
            ftp.chdir(testdir);      
    
            // get an empty file
            ftp.get(localDataDir + remoteEmptyFile, remoteEmptyFile);
            File empty = new File(localDataDir + remoteEmptyFile);
            assertTrue(empty.exists());
            assertTrue(empty.length() == 0);
    
            // delete it
            empty.delete();
    
            // put an empty file
            ftp.put(localDataDir + localEmptyFile, localEmptyFile);        
            
            // get it back as a different filename
            String filename = generateRandomFilename();
            ftp.get(localDataDir + filename, localEmptyFile);
            empty = new File(localDataDir + filename);
            assertTrue(empty.exists());
            assertTrue(empty.length() == 0);
    
            // delete file we got back (copy of our local empty file)
            empty.delete();
    
            // and delete the remote empty file we
            // put there
            ftp.delete(localEmptyFile);
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }

    /**
     *  Test transferring non-existent files
     */
    public void testTransferNonExistent() throws Exception {

        log.info("testTransferNonExistent()");
        
        try {

            connect();
    
            // move to test directory
            ftp.chdir(testdir);  
    
            // generate a name & try to get it
            String filename = generateRandomFilename();
            log.info("Getting non-existent file: " + filename);
            try {
                ftp.get(localDataDir + filename, filename);
                fail(filename + " should not be found");
            }
            catch (IOException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }
            catch (FTPException ex) {
                log.info("Expected exception: " + ex.getMessage());
    
            }
            
            // ensure we don't have a local file of that name produced
            File file = new File(localDataDir + filename);
            assertFalse(file.exists());
    
            // generate name & try to put
            filename = generateRandomFilename();
            try {
                ftp.put(localDataDir + filename, filename);
                fail(filename + " should not be found");
            }
            catch (FileNotFoundException ex) {
                log.info("Expected exception: " + ex.getMessage());
            }        
    
            ftp.quit(); 
        }
        finally {
            ftp.quitImmediately();
        }
    }
    
    
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestTransfer.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

