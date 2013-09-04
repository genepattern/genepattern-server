/**
 *
 *  Java FTP client library.
 *
 *  Copyright (C) 2000-2003  Enterprise Distributed Technologies Ltd
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
 *        $Log: TestFileOperations.java,v $
 *        Revision 1.12  2008-05-14 05:52:23  bruceb
 *        add some fail()s
 *
 *        Revision 1.11  2008-05-02 07:41:43  bruceb
 *        setModTime added
 *
 *        Revision 1.10  2007-12-18 07:55:50  bruceb
 *        add finally
 *
 *        Revision 1.9  2007-08-09 00:10:53  hans
 *        Removed unused imports.
 *
 *        Revision 1.8  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.7  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.6  2004/08/31 10:44:49  bruceb
 *        minor tweaks re compile warnings
 *
 *        Revision 1.5  2004/05/01 17:05:43  bruceb
 *        Logger stuff added
 *
 *        Revision 1.4  2004/04/17 18:38:38  bruceb
 *        tweaks for ssl and new parsing functionality
 *
 *        Revision 1.3  2003/11/02 21:51:32  bruceb
 *        test for size()
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Tests various file operations
 *
 *  @author         Bruce Blackshaw
 *  @version        $Revision: 1.12 $
 */
public class TestFileOperations extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestFileOperations.java,v 1.12 2008-05-14 05:52:23 bruceb Exp $";

    /**
     *  Formatter for modtime
     */
    private SimpleDateFormat modFormatter = 
        new SimpleDateFormat("yyyy/MM/dd @ HH:mm:ss");

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestFileOperations.log";
    }

    /**
     *  Test remote deletion
     */
    public void testDelete() throws Exception {
        // test existent & non-existent file
        log.debug("testDelete() - ENTRY");
        try {
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    
           try {
                // try to delete a non-existent file
                String file = generateRandomFilename();
                log.debug("Deleting a non-existent file");
                ftp.delete(file);
                fail();
            }
            catch (FTPException ex) {
                log.debug("Expected exception: " + ex.getMessage());
            }
    
            // how to delete a file and make it repeatable?            
            ftp.quit();
        }
        finally {
            log.debug("testDelete() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }

    /**
     *  Test renaming of a file
     */
    public void testRename() throws Exception {
        log.debug("testRename() - ENTRY");
        try {
            // test existent & non-existent file
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    
            // rename file
            String rename = remoteTextFile + ".renamed";
            ftp.rename(remoteTextFile, rename);
            
            // get its mod time
            Date modTime = ftp.modtime(rename);
            String mod = modFormatter.format(modTime);
            log.debug(rename + ": " + mod);
    
            // modtime on original file should fail
            try {
                ftp.modtime(remoteTextFile);
                fail();
            }
            catch (FTPException ex) {
                log.debug("Expected exception: " + ex.getMessage());
            }
            
            // and rename file back again
            ftp.rename(rename, remoteTextFile);
    
            // and modtime should succeed 
            modTime = ftp.modtime(remoteTextFile);
            mod = modFormatter.format(modTime);
            log.debug(remoteTextFile + ": " + mod);
    
            // modtime on renamed (first time) file should 
            // now fail (as we've just renamed it to original)
            try {
                ftp.modtime(rename);
                fail();
            }
            catch (FTPException ex) {
                log.debug("Expected exception: " + ex.getMessage());
            }    
    
            ftp.quit();
        }
        finally {
            log.debug("testRename() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }


    /**
     *  Test finding the modification time 
     *  of a file
     */
    public void testModtime() throws Exception {
        log.debug("testModtime() - ENTRY");
        try {
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    
            // get modtime
            log.debug("Modtime on existing file: " + remoteTextFile);
            Date modTime = ftp.modtime(remoteTextFile);
            String mod = modFormatter.format(modTime);
            log.debug(remoteTextFile + ": " + mod);
    
            try {
                // get modtime on non-existent file
                String file = generateRandomFilename();
                log.debug("Modtime on non-existent file");
                modTime = ftp.modtime(file);
                fail(); 
            }
            catch (FTPException ex) {
                log.debug("Expected exception: " + ex.getMessage());
            }
    
            ftp.quit(); 
        }
        finally {
            log.debug("testModtime() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }
    
    
    public  void testSetModtime() throws Exception {
        log.debug("testSetModtime() - ENTRY");

        try {
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
    

            // get modtime
            log.debug("Existing modtime on existing file: " + remoteTextFile);
            Date modTime = ftp.modtime(remoteTextFile);
            log.debug(remoteTextFile + ": " + modTime.toString());
    
            // set modtime
            Calendar cal = Calendar.getInstance();
            cal.setTime(modTime);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.add(Calendar.HOUR_OF_DAY, 2);
            cal.add(Calendar.MINUTE, 3);
            cal.add(Calendar.SECOND, 4);
            Date desiredModTime = cal.getTime();
            log.debug("Setting mod-time to " + desiredModTime.toString());
            ftp.setModTime(remoteTextFile, desiredModTime);
            
            Date actualModTime = ftp.modtime(remoteTextFile);
            log.debug(remoteTextFile + ": " + actualModTime.toString());
            if ((int)desiredModTime.getTime()/1000 != (int)actualModTime.getTime()/1000)
            {
                String msg = "Desired mod-time(" + desiredModTime + ") != actual mod-time(" + actualModTime + ")";
                log.debug(msg);
                fail(msg);
            }
    
            ftp.quit(); 
        }
        finally {
            log.debug("setModtime() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }
    
    /**
     *  Test the size() method
     */
    public void testSize() throws Exception {
        log.debug("testSize() - ENTRY");
        try {
            connect();
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
    
            // put to a random filename
            String filename = generateRandomFilename();
            ftp.put(localDataDir + localTextFile, filename);
    
            // find size of local file
            File local = new File(localDataDir + localTextFile);
            long sizeLocal = local.length();
            
            // find size of remote
            long sizeRemote = ftp.size(filename);
    
            // delete remote file
            ftp.delete(filename);
            
            if (sizeLocal != sizeRemote) {
                String msg = "Local size(" + sizeLocal + ") != remote size(" + sizeRemote + ")";
                log.debug(msg);
                throw new Exception(msg);
            }
                
    
            ftp.quit();
        }
        finally {
            log.debug("testSize() - EXIT");
            if (ftp.connected()) {
                ftp.quitImmediately();
            }
        }
    }    
    

    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestFileOperations.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

