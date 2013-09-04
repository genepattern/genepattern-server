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
 *        $Log: TestResume.java,v $
 *        Revision 1.14  2012-02-08 06:19:29  bruceb
 *        resumeNextDownload
 *
 *        Revision 1.13  2012-01-22 23:59:53  bruceb
 *        TestProgressMonitor added
 *
 *        Revision 1.12  2009-03-20 05:07:24  bruceb
 *        allow overriding of setup()
 *
 *        Revision 1.11  2009-01-28 03:47:18  bruceb
 *        set retry to 0
 *
 *        Revision 1.10  2007-12-18 07:55:50  bruceb
 *        add finally
 *
 *        Revision 1.9  2007-08-09 00:10:52  hans
 *        Removed unused imports.
 *
 *        Revision 1.8  2007-05-03 04:21:06  bruceb
 *        extra debug
 *
 *        Revision 1.7  2007/02/07 23:04:34  bruceb
 *        fix testing for cancellation
 *
 *        Revision 1.6  2005/11/15 21:07:30  bruceb
 *        disconnect when cancelled and reconnect
 *
 *        Revision 1.5  2005/11/10 19:46:45  bruceb
 *        uses FTPTransferCancelledException
 *
 *        Revision 1.4  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.3  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.2  2004/08/31 13:49:01  bruceb
 *        test cancelResume()
 *
 *        Revision 1.1  2004/08/31 10:44:17  bruceb
 *        test code
 * 
 */

package com.enterprisedt.net.ftp.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferCancelledException;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Test get'ing and put'ing of remote files in various ways
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.14 $
 */
public class TestResume extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestResume.java,v 1.14 2012-02-08 06:19:29 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestResume.log";
    }

    /**
     *  Test resuming when putting a binary file
     */
    public void testResumePut() throws Exception {

        log.debug("testResumePut()");
        
        try {

            setup();
            
            // put to a random filename
            String filename = generateRandomFilename();
            
            // use monitor to abort
            ftp.setProgressMonitor(new CancelProgressMonitor(ftp), 50000); 
            
            // initiate the put
            boolean cancelled = false;
            try {
                ftp.put(localDataDir + localBinaryFile, filename);
            }
            catch (FTPTransferCancelledException ex) {
                log.debug("Caught expected cancellation exception", ex);
                cancelled = true;
            }
            
            ftp.quit();
     
            assertTrue(cancelled);
            
            // reconnect
            setup();
            
            // should be cancelled - now resume
            ftp.resume();
            
            // put again - should append
            ftp.put(localDataDir + localBinaryFile, filename);
                    
            // get it back        
            ftp.get(localDataDir + filename, filename);
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
            
            // finally, just check cancelResume works
            ftp.cancelResume();
            
            // get it back        
            ftp.get(localDataDir + filename, filename);        
            
            // delete remote file
            ftp.delete(filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        finally {
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }
    
    /**
     *  Test resuming when putting a binary file
     */
    public void testResumeGet() throws Exception {

        log.debug("testResumeGet()");
        
        try {

            setup();
            
            // put to a random filename
            String filename = generateRandomFilename();          
            
            // use monitor to abort
            ftp.setProgressMonitor(new CancelProgressMonitor(ftp), 131072); 
                    
            // initiate the put
            boolean cancelled = false;
            try {
                ftp.get(localDataDir + filename, remoteBinaryFile);
            }
            catch (FTPTransferCancelledException ex) {
                log.debug("Caught expected cancellation exception", ex);
                cancelled = true;
            }
            
            ftp.quit();
    
            assertTrue(cancelled);
            
            // reconnect
            setup();
            
            // should be cancelled - now resume
            ftp.resume();
            
            // get again - should append
            ftp.get(localDataDir + filename, remoteBinaryFile);
            
            String filename2 = generateRandomFilename();;
                    
            // do another get - complete this time    
            ftp.get(localDataDir + filename2, remoteBinaryFile);
    
            // check equality of local files
            assertIdentical(localDataDir + filename, localDataDir + filename2);
            
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
            local = new File(localDataDir + filename2);
            local.delete();
            
            ftp.quit();
        }
        finally {
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }
    
    
    /**
     *  Test resuming a download at a fixed point
     */
    public void testResumeDownload() throws Exception {

        log.debug("testResumeDownload()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 2048);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            long size = ftp.size(filename);
    
            // get part of it back
            long resumePoint = (long)(Math.random() * (double)size);
            log.debug("Resume point=" + resumePoint);
            ftp.resumeNextDownload(resumePoint);
            
            FileInputStream in = new FileInputStream(localDataDir + localBinaryFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int count = 0;
            byte[] buf = new byte[4096];
            in.skip(resumePoint);
            while ((count = in.read(buf)) > 0) {
                out.write(buf, 0, count);
            }
            byte[] buf1 = out.toByteArray();
            
            byte[] buf2 = ftp.get(filename);
             
            // delete remote file
            ftp.delete(filename);
            
            assertEquals(buf2.length, size-resumePoint);
            assertIdentical(buf1, buf2);      
        
            ftp.quit();
        }
        finally {
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }


    
    /**
     * Connect and navigate to test dir, binary mode
     * 
     * @throws Exception
     */
    protected void setup() throws Exception {
        connect();
        
        if (ftp instanceof FTPClient) {
            
            ((FTPClient)ftp).setRetryCount(0);
            log.debug("Set retry count to 0");
        }
        
        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.BINARY);
    }
      
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestResume.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     *  Test of progress monitor functionality     
     */
    public class TestProgressMonitor implements FTPProgressMonitor {

        /* (non-Javadoc)
         * @see com.enterprisedt.net.ftp.FTPProgressMonitor#bytesTransferred(long)
         */
        public void bytesTransferred(long count) {
            log.info(count + " bytes transferred");         
        }            
    }
    
    /**
     *  As soon it receives notification of bytes transferred, it
     *  cancels the transfer    
     */
    class CancelProgressMonitor implements FTPProgressMonitor {
        
        /**
         * True if cancelled 
         */
        private boolean cancelled = false;
        
        /**
         * FTPClient reference
         */
        private FTPClientInterface ftpClient;
        
        /**
         * Constructor
         * 
         * @param ftp   FTP client reference
         */
        public CancelProgressMonitor(FTPClientInterface ftp) {
            this.ftpClient = ftp;
        }
        
        /* (non-Javadoc)
         * @see com.enterprisedt.net.ftp.FTPProgressMonitor#bytesTransferred(long)
         */
        public void bytesTransferred(long bytes) {
            log.debug("bytesTransferred(" + bytes + ") called");
            if (!cancelled) {
                log.debug("Cancelling transfer");
                ftpClient.cancelTransfer();
                cancelled = true;
            }
        }            
    }
}

