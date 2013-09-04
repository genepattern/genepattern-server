/**
 *
 *  Java FTP client library.
 *
 *  Copyright (C) 2012  Enterprise Distributed Technologies Ltd
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
 *        $Log: TestStream.java,v $
 *        Revision 1.3  2012/11/15 06:09:27  bruceb
 *        *** empty log message ***
 *
 *        Revision 1.2  2012/11/07 02:41:50  bruceb
 *        extra test
 *
 *        Revision 1.1  2012/11/06 04:12:13  bruceb
 *        stream changes - extract TestProgressMonitor out
 *
 *        Revision 1.4  2008-04-24 05:37:48  bruceb
 *        tweaked for SFTC changes
 *
 *        Revision 1.3  2007-08-09 00:11:00  hans
 *        Removed unused imports.
 *
 *        Revision 1.2  2007/03/22 03:05:17  bruceb
 *        FTPOutputStream testing
 *
 *        Revision 1.1  2007/02/26 07:25:16  bruceb
 *        test the streams code
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPInputStream;
import com.enterprisedt.net.ftp.FTPOutputStream;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Test get'ing and put'ing of remote files in various ways
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.3 $
 */
public class TestStream extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestStream.java,v 1.3 2012/11/15 06:09:27 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestStream.log";
    }
    
    
    /**
     *  Test transferring a binary file
     */
    public void testTransferBinaryStreamDownload() throws Exception {

        log.debug("testTransferBinaryStreamDownload()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
    
            // get it back 
            InputStream str = getInputStream(ftp, filename);
            BufferedOutputStream out = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                
                out = new BufferedOutputStream(new FileOutputStream(localDataDir + filename));
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
            
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    /**
     *  Test skipping at start
     */
    public void testTransferBinaryStreamDownloadSkip() throws Exception {

        log.debug("testTransferBinaryStreamDownloadSkip()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            
            long length = ftp.size(filename);
            long skip = length/10;
            log.debug("Length=" + length + ", skip=" + skip);
    
            // get it back 
            InputStream str = getInputStream(ftp, filename);
            str.skip(skip);
            BufferedOutputStream out = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                
                out = new BufferedOutputStream(new FileOutputStream(localDataDir + filename));
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
            
            String filename2 = filename + "2";
            
            ftp.resumeNextDownload(skip);
            ftp.get(localDataDir + filename2, filename);
            
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + filename2, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
            
            local = new File(localDataDir + filename2);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    /**
     *  Test skipping part way through
     */
    public void testTransferBinaryStreamDownloadSkip2() throws Exception {

        log.debug("testTransferBinaryStreamDownloadSkip2()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            
            long length = ftp.size(filename);
            long skip = length/10;
            log.debug("Length=" + length + ", skip=" + skip);
    
            // get it back 
            InputStream str = getInputStream(ftp, filename);
            long size = 0;
            BufferedOutputStream out = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                
                
                // read some and mark
                while ((count = str.read(chunk)) >= 0) {
                    size += count;
                    if (size > skip)
                        break;
                }
                
                // now skip 
                str.skip(skip);
                
                out = new BufferedOutputStream(new FileOutputStream(localDataDir + filename));
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
                
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
            
            String filename2 = filename + "2";
            
            // resume past the read bit and the skipped bit
            ftp.resumeNextDownload(size+skip);
            ftp.get(localDataDir + filename2, filename);
            
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + filename2, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
            
            local = new File(localDataDir + filename2);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    /**
     *  Test skipping part way through
     */
    public void testTransferBinaryStreamDownloadSkipReset() throws Exception {

        log.debug("testTransferBinaryStreamDownloadSkipReset()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            
            long length = ftp.size(filename);
            
            long skip = length - 132072;
            log.debug("Length=" + length + ", skip=" + skip);
    
            // get it back 
            InputStream str = getInputStream(ftp, filename);
            str.skip(skip);
            str.mark(0);
            
            int shortSkip = 60000;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] first = null;
            byte[] second = null;
            try {
                byte[] chunk = new byte[8192];
                int count = 0;              
                
                // read some and mark
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                first = out.toByteArray();
                out.reset();
                
                // now reset stream and skip
                str.reset();
                str.skip(shortSkip);
                
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                second = out.toByteArray();
                
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
            
    
            // delete remote file
            ftp.delete(filename);
            
            // we only want part of first
            byte[] firstPart = new byte[(int)(first.length-shortSkip)];
            System.arraycopy(first, shortSkip, firstPart, 0, first.length-shortSkip);
    
            // check equality of local files
            assertIdentical(firstPart, second);
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    /**
     *  Test transferring a binary file
     */
    public void testTransferBinaryStreamDownloadReset() throws Exception {

        log.debug("testTransferBinaryStreamDownloadReset()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localBinaryFile, filename);
            
            long length = ftp.size(filename);
            long markpos = length/10;
            log.debug("Length=" + length + ", markpos=" + markpos);
            
            byte[] first = null;
            byte[] second = null;
                
            // get it back 
            InputStream str = getInputStream(ftp, filename);          
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                long size = 0;
                
                // read some and mark
                while ((count = str.read(chunk)) >= 0) {
                    size += count;
                    if (size > markpos)
                        break;
                }
                str.mark(0);
                markpos = size;
                
                // read some more
                while ((count = str.read(chunk)) >= 0) {
                    size += count;
                    out.write(chunk, 0, count);
                    if (size > markpos*2)
                        break;
                }
                first = out.toByteArray();
                
                // now go back to mark and read again
                str.reset();
                size = markpos;
                out.close();
                out = new ByteArrayOutputStream();
                while ((count = str.read(chunk)) >= 0) {
                    size += count;
                    out.write(chunk, 0, count);
                    if (size > markpos*2)
                        break;
                }
                second = out.toByteArray();
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
            
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(first, second);
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    /**
     *  Test transferring a binary file
     */
    public void testTransferBinaryStreamUpload() throws Exception {

        log.debug("testTransferBinaryStreamDownload()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put to a random filename
            String filename = generateRandomFilename();  
            
            // get it back  
            OutputStream out = getOutputStream(ftp, filename);
            BufferedInputStream in = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                in = new BufferedInputStream(new FileInputStream(localDataDir + localBinaryFile));
                while ((count = in.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
            }
            finally {
                if (in != null)
                    in.close();
                out.close();
            }        
            
            ftp.get(localDataDir + filename, filename);

            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    
    /**
     *  Test transferring a ASCII file
     */
    public void testTransferASCIIStreamDownload() throws Exception {

        log.debug("testTransferASCIIStreamDownload()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.ASCII);
            
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localTextFile, filename);
    
            // get it back        
            InputStream str = getInputStream(ftp, filename);
            BufferedOutputStream out = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                
                out = new BufferedOutputStream(new FileOutputStream(localDataDir + filename));
                while ((count = str.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
            }
            finally {
                if (out != null)
                    out.close();
                str.close();
            }
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + localTextFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    
    /**
     *  Test transferring a ASCII file
     */
    public void testTransferASCIIStreamUpload() throws Exception {

        log.debug("testTransferASCIIStreamUpload()");
        
        try {
            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 8192);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.ASCII);
            
            // put to a random filename
            String filename = generateRandomFilename();
            
    
            // get it back        
            OutputStream out = getOutputStream(ftp, filename);
            BufferedInputStream in = null;
            try {
                byte[] chunk = new byte[4096];
                int count = 0;
                
                in = new BufferedInputStream(new FileInputStream(localDataDir + localTextFile));
                while ((count = in.read(chunk)) >= 0) {
                    out.write(chunk, 0, count);
                }
                out.flush();
            }
            finally {
                if (in != null)
                    in.close();
                out.close();
            }
            
            ftp.get(localDataDir + filename, filename);
    
            // delete remote file
            ftp.delete(filename);
    
            // check equality of local files
            assertIdentical(localDataDir + localTextFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
    
            ftp.quit();
        }
        catch (ClassCastException ex) {
            log.info("Only FTPClient (and subclasses) supports streaming");
            ftp.quit();
        }
        catch (Exception ex) {
            log.error("Unexpected error", ex);
            ftp.quitImmediately();
            throw ex;
        }
    }
    
    protected OutputStream getOutputStream(FTPClientInterface ftp, String filename) 
        throws IOException, FTPException {
        return new FTPOutputStream((FTPClient)ftp, filename);
    }

    protected InputStream getInputStream(FTPClientInterface ftp, String filename) 
        throws IOException, FTPException {
        return new FTPInputStream((FTPClient)ftp, filename);
    }
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestStream.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

