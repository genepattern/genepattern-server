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
 *  Bug fixes, suggestions and comments should posted on the EDT forums at
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *        $Log: TestBulkTransfer.java,v $
 *        Revision 1.3  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.2  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.2  2005/05/15 19:45:36  bruceb
 *        changes for testing setActivePortRange
 *
 *        Revision 1.1  2005/01/14 18:07:37  bruceb
 *        bulk test2 TestBulkTransfer.java
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPTransferType;

/**
 * Test get'ing and put'ing of remote files multiple times - stress test
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.3 $
 */
public class TestBulkTransfer extends FTPTestCase {

    /**
     * Revision control id
     */
    public static String cvsId = "@(#)$Id: TestBulkTransfer.java,v 1.3 2005/07/15 17:30:06 bruceb Exp $";

    /**
     * get name of log file
     * 
     * @return name of file to log to
     */
    protected String getLogName() {
        return "TestBulkTransfer.log";
    }

    /**
     * Test transfering a binary file
     */
    public void testTransferBinary() throws Exception {
        log.debug("TransferBinary()");

        connect();

        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.BINARY);

        bulkTransfer(localBinaryFile);

        ftp.quit();
    }

    /**
     * Test transfering a text file
     */
    public void testTransferText() throws Exception {
        log.debug("TransferText()");

        connect();

        // move to test directory
        ftp.chdir(testdir);
        ftp.setType(FTPTransferType.ASCII);

        bulkTransfer(localTextFile);

        ftp.quit();
    }
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestBulkTransfer.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }

}
