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
 *        $Log: TestAutoModeTransfer.java,v $
 *        Revision 1.1  2007/02/06 07:23:09  bruceb
 *        test autodetect transfer mode
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

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Test automatic switching between ASCII & binary files
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.1 $
 */
public class TestAutoModeTransfer extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestAutoModeTransfer.java,v 1.1 2007/02/06 07:23:09 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestAutoModeTransfer.log";
    }

    /**
     *  Test transfering files with auto switched on
     */
    public void testAutoModeTransfer() throws Exception {

        log.debug("testAutoModeTransfer()");

        connect();
        
        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.BINARY);
        ftp.setDetectTransferMode(true);
        
        TestProgressMonitor monitor = new TestProgressMonitor(ftp);
        ftp.setProgressMonitor(monitor, 1000); 
        
        // put to a random filename
        String filename = generateRandomFilename() + ".txt";
        ftp.put(localDataDir + localTextFile, filename);
        
        assertTrue(ftp.getType().equals(FTPTransferType.BINARY));
        assertTrue(monitor.getType().equals(FTPTransferType.ASCII));

        // get it back        
        ftp.get(localDataDir + filename, filename);
        
        assertTrue(ftp.getType().equals(FTPTransferType.BINARY));
        assertTrue(monitor.getType().equals(FTPTransferType.ASCII));

        // delete remote file
        ftp.delete(filename);
        
        // check equality of local files
        assertIdentical(localDataDir + localTextFile, localDataDir + filename);
        
        // and delete local file
        File local = new File(localDataDir + filename);
        local.delete();
        
        ftp.setType(FTPTransferType.ASCII);
        
        filename = generateRandomFilename() + ".exe";
        ftp.put(localDataDir + localBinaryFile, filename);
        
        assertTrue(ftp.getType().equals(FTPTransferType.ASCII));
        assertTrue(monitor.getType().equals(FTPTransferType.BINARY));

        // get it back        
        ftp.get(localDataDir + filename, filename);
        
        assertTrue(ftp.getType().equals(FTPTransferType.ASCII));
        assertTrue(monitor.getType().equals(FTPTransferType.BINARY));

        // delete remote file
        ftp.delete(filename);
        
        // check equality of local files
        assertIdentical(localDataDir + localBinaryFile, localDataDir + filename);

        // and delete local file
        local = new File(localDataDir + filename);
        local.delete();

        ftp.quit();
    }
    
    
    /**
     *  We use the progress monitor to grab a snapshot of the transfer
     *  type during the transfer
     */
    class TestProgressMonitor implements FTPProgressMonitor {
        
        /**
         * FTPClient reference
         */
        private FTPClientInterface ftpClient;
        
        private FTPTransferType type;
        
        /**
         * Constructor
         * 
         * @param ftp   FTP client reference
         */
        public TestProgressMonitor(FTPClientInterface ftp) {
            this.ftpClient = ftp;
        }
        
        public FTPTransferType getType() {
            return type;
        }
        
        public void setType(FTPTransferType type) {
            this.type = type;
        }

		/* (non-Javadoc)
		 * @see com.enterprisedt.net.ftp.FTPProgressMonitor#bytesTransferred(long)
		 */
		public void bytesTransferred(long count) {
            log.debug("Saving transfer type (count=" + count + ")");
			type = ftpClient.getType();		
		}            
    }
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestAutoModeTransfer.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

