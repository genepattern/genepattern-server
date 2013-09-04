/**
 *
 *  edtFTPj
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
 *        $Log: TestListings.java,v $
 *        Revision 1.15  2010-09-14 00:44:58  bruceb
 *        callback testing
 *
 *        Revision 1.14  2010-04-26 15:56:42  bruceb
 *        tesing for new dirDetails method with callback
 *
 *        Revision 1.13  2007-12-18 07:55:50  bruceb
 *        add finally
 *
 *        Revision 1.12  2007-08-09 00:10:52  hans
 *        Removed unused imports.
 *
 *        Revision 1.11  2005/09/21 11:20:20  bruceb
 *        reply code 2
 *
 *        Revision 1.10  2005/09/21 10:38:47  bruceb
 *        more debug
 *
 *        Revision 1.9  2005/09/21 08:39:11  bruceb
 *        allow 450 as well as 550 for empty dir
 *
 *        Revision 1.8  2005/07/22 10:20:09  bruceb
 *        cater for SSH tests
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
 *        Revision 1.3  2004/04/17 18:38:38  bruceb
 *        tweaks for ssl and new parsing functionality
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.DirectoryListArgument;
import com.enterprisedt.net.ftp.DirectoryListCallback;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;

/**
 *  Tests the various commands that list directories
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.15 $
 */
public class TestListings extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestListings.java,v 1.15 2010-09-14 00:44:58 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestListings.log";
    }

    /**
     *  Test directory listings
     */ 
    public void testDir() throws Exception {

        log.debug("testDir() - ENTRY");
        try {
        
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    
            // list current dir
            String[] list = ftp.dir();
            print(list);
    
            // list current dir by name
            list = ftp.dir(".");
            print(list);
    
            // list empty dir by name
            log.debug("Testing for empty dir: " + remoteEmptyDir);
            list = ftp.dir(remoteEmptyDir);
            print(list);
            log.debug("End testing for empty dir");
    
            // non-existent file
    		String randomName = generateRandomFilename();
            try {        
                list = ftp.dir(randomName);
                print(list);
    		}
    		catch (FTPException ex) { // reply code 2 is SFTP
    			if (ex.getReplyCode() != 550 && ex.getReplyCode() != 450 && ex.getReplyCode() != 2)
    				fail("dir(" + randomName + ") should throw 450/550 for non-existent dir");
    		}            
            
            ftp.quit();
        }
        finally {
            log.debug("testDir() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
        
        
    }

    /**
     *  Test full directory listings
     */ 
    public void testDirFull() throws Exception {

        log.debug("testDirFull() - ENTRY");
        
        try {
        
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    
            // list current dir by name
            String[] list = ftp.dir(".", true);
            print(list);
            
            log.debug("******* dirDetails *******");
            FTPFile[] files = ftp.dirDetails(".");
            print(files);
            log.debug("******* end dirDetails *******");
    
            // list empty dir by name
            log.debug("Testing for empty dir: " + remoteEmptyDir);
            list = ftp.dir(remoteEmptyDir, true);
            print(list);
            log.debug("End testing for empty dir");
    
            // non-existent file. Some FTP servers don't send
            // a 450/450, but IIS does - so we catch it and
            // confirm it is a 550
            String randomName = generateRandomFilename();
            log.debug("Testing for non-existent dir: " + randomName);
            try {        
            	list = ftp.dir(randomName, true);
            	print(list);
            }
            catch (FTPException ex) { // reply code 2 is SFTP
            	if (ex.getReplyCode() != 550 && ex.getReplyCode() != 450 && ex.getReplyCode() != 2)
    				fail("dir(" + randomName + ") should throw 450/550/2 for non-existent dir");
            }
            
            ftp.quit();
        }
        finally {
            log.debug("testDirFull() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
        
        
    }
    
    
    /**
     *  Test full directory listings
     */ 
    public void testDirFull2() throws Exception {

        log.debug("testDirFull2() - ENTRY");
        
        try {
        
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
                
            FTPFile[] files = ftp.dirDetails(".");
            
            DirCallback dirCallback = new DirCallback();
            ftp.dirDetails(".", dirCallback);
            
            if (dirCallback.getFiles().size() != files.length) {
                log.debug("Callback=" + dirCallback.getFiles().size() + ", listing=" + files.length);
                Iterator i = dirCallback.getFiles().keySet().iterator();
                while (i.hasNext()) {
                    log.debug((String)i.next());
                }
                log.debug("Listing=" + files.length);
                for (int j = 0; j < files.length; j++)
                    log.debug(files[j].getName());
                fail("Mismatch in listing length");
            }
            
            log.debug("Listing lengths match!");
            
            boolean allFound = true;
            for (int i = 0; i < files.length; i++) {
                if (dirCallback.getFiles().get(files[i].getName()) == null) {
                    log.debug("Failed to find " + files[i].getName());
                    allFound = false;
                }
            }
            
            if (!allFound) {
                Iterator i = dirCallback.getFiles().values().iterator();
                while (i.hasNext()) {
                    log.debug(((FTPFile)i.next()).getName());
                }
                fail("Failed to find all files");
            }
            
            ftp.quit();
        }
        finally {
            log.debug("testDirFull2() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
        
        
    }
    
    class DirCallback implements DirectoryListCallback {

        private Map files = new HashMap();
        
        public void listDirectoryEntry(DirectoryListArgument arg) {
            files.put(arg.getEntry().getName(), arg.getEntry());            
        }
        
        public Map getFiles() {
            return files;
        }
        
    }


    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestListings.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

