/**
 *
 *  edtFTPj
 *
 *  Copyright (C) 2005  Enterprise Distributed Technologies Ltd
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
 *  Bug fixes, suggestions and comments should posted on the EDT forums at
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *        $Log: TestBigTransfer.java,v $
 *        Revision 1.2  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.1  2005/03/18 13:42:06  bruceb
 *        tests big files
 *
 *        Revision 1.1  2005/01/14 18:07:37  bruceb
 *        bulk test2 TestBulkTransfer.java
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 * Test get'ing and put'ing of a big file
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.2 $
 */
public class TestBigTransfer extends FTPTestCase {

    /**
     * Revision control id
     */
    public static String cvsId = "@(#)$Id: TestBigTransfer.java,v 1.2 2005/07/15 17:30:06 bruceb Exp $";

    /**
     * Interval between reporting progress
     */
    private static int PROGRESS_INTERVAL = 1048575;
    
    /**
     * get name of log file
     * 
     * @return name of file to log to
     */
    protected String getLogName() {
        return "TestTransferLarge.log";
    }

    /**
     * Test transfering a binary file
     */
    public void testTransferLarge() throws Exception {
        log.debug("TransferLarge()");

        connect();

        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.BINARY);
        
        ftp.setProgressMonitor(new TestProgressMonitor(), PROGRESS_INTERVAL);
        
        bigTransfer(localBigFile);

        ftp.quit();
    }

    /**
     * Transfer back and forth multiple times
     */
    private void bigTransfer(String bigFile) throws Exception {

        ftp.put(localDataDir + bigFile, bigFile);

        ftp.get(localDataDir + bigFile + ".copy", bigFile);

        // delete remote file
        ftp.delete(bigFile);
        
        // check equality of local files
        assertIdentical(localDataDir + bigFile + ".copy", localDataDir + bigFile);

        // and delete local file
        File local = new File(localDataDir + bigFile + ".copy");
        local.delete();
    }
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestBigTransfer.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     *  As soon it receives notification of bytes transferred, it
     *  cancels the transfer    
     */
    class TestProgressMonitor implements FTPProgressMonitor {
        
        /* (non-Javadoc)
         * @see com.enterprisedt.net.ftp.FTPProgressMonitor#bytesTransferred(long)
         */
        public void bytesTransferred(long bytes) {
            log.debug(bytes + " transferred");
        }            
    }

}