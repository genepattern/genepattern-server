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
 *        $Log: VMSTests.java,v $
 *        Revision 1.3  2007/03/02 07:10:18  bruceb
 *        tweak to try cd into a dir that is returned from listing.
 *
 *        Revision 1.2  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.1  2005/06/03 11:26:49  bruceb
 *        new tests
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Tests against an external (public) VMS FTP server - so we
 *  can't do put's.
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.3 $
 */
public class VMSTests extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: VMSTests.java,v 1.3 2007/03/02 07:10:18 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestVMS.log";
    }

    /**
     *  Test directory listings
     */ 
    public void testDir() throws Exception {

        connect();

        // move to test directory
        ftp.chdir(testdir);

        // list current dir
        String[] list = ftp.dir();
        print(list);

        // non-existent file
		String randomName = generateRandomFilename();
        try {        
            list = ftp.dir(randomName);
            print(list);
		}
		catch (FTPException ex) {
            if (ex.getReplyCode() != 550 && ex.getReplyCode() != 552)
                fail("dir(" + randomName + ") should throw 550/552 for non-existent dir");
		}            
        
        ftp.quit();
    }

    /**
     *  Test full directory listings
     */ 
    public void testDirFull() throws Exception {

        connect();
 
        // move to test directory
        ftp.chdir(testdir);

        // list current dir by name
        String[] list = ftp.dir("", true);
        print(list);
        
        log.debug("******* dirDetails *******");
        FTPFile[] files = ftp.dirDetails("");
        print(files);
        log.debug("******* end dirDetails *******");

        // non-existent file. Some FTP servers don't send
        // a 450/450, but IIS does - so we catch it and
        // confirm it is a 550
        String randomName = generateRandomFilename();
        try {        
        	list = ftp.dir(randomName, true);
        	print(list);
        }
        catch (FTPException ex) {
        	if (ex.getReplyCode() != 550 && ex.getReplyCode() != 552)
				fail("dir(" + randomName + ") should throw 550/552 for non-existent dir");
        }
        
        ftp.quit();
    }
    
    
    /**
     *  Test transfering a text file
     */
    public void testChangeDir() throws Exception {

        log.debug("testChangeDir");

        connect();
        
        // move to test directory
        ftp.chdir(testdir);
        
        String dir = getRemoteDirName();
        
        ftp.chdir(dir);
        
        ftp.cdup();
        
        ftp.quit();
    }

    /**
     *  Test transfering a text file
     */
    public void testTransfer() throws Exception {

        log.debug("testTransfer");

        connect();
        
        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.ASCII);

        // random filename
        String filename = generateRandomFilename();
        
        String remoteFile = getRemoteFileName();
        
        if (remoteFile == null)
            remoteFile = remoteTextFile;

        // get it        
        ftp.get(localDataDir + filename, remoteFile);
        
        ftp.setType(FTPTransferType.BINARY);
        
        ftp.get(localDataDir + filename, remoteFile);

        // and delete local file
        File local = new File(localDataDir + filename);
        local.delete();

        ftp.quit();
    }
    
    
    private String getRemoteFileName() throws IOException, FTPException, ParseException {
        FTPFile[] files = ftp.dirDetails("");
        String remoteFile = null;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().toUpperCase().endsWith(".TXT")) {
                remoteFile = files[i].getName();
                break;
            }                   
        }
        return remoteFile;
    }
    
    private String getRemoteDirName() throws IOException, FTPException, ParseException {
        FTPFile[] files = ftp.dirDetails("");
        String dir = null;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDir()) {
                dir = files[i].getName();
                break;
            }                   
        }
        return dir;
    }

    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(VMSTests.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

