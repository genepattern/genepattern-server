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
 *        $Log: TestMLST.java,v $
 *        Revision 1.2  2007/01/15 23:02:28  bruceb
 *        change hardcoded ipswitch file
 *
 *        Revision 1.1  2007/01/15 22:57:33  bruceb
 *        test MLST command (via fileDetails)
 *
 *        Revision 1.1  2006/09/11 12:34:21  bruceb
 *        test exists() method
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

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPFile;

/**
 *  Test MLST 
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.2 $
 */
public class TestMLST extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestMLST.java,v 1.2 2007/01/15 23:02:28 bruceb Exp $";

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestMLST.log";
    }

    /**
     *  Test MLST
     */
    public void testMLST() throws Exception {

        log.debug("testMLST()");

        connect();
        
        // move to test directory
        ftp.chdir(testdir);
        
        // put to a random filename
        String filename = generateRandomFilename();
        ftp.put(localDataDir + localTextFile, filename);

        FTPFile ftpFile = ((FTPClient)ftp).fileDetails(filename);
        
        log.debug("[" + ftpFile.toString() +"]");
        
        ftp.delete(filename);
        
        ftp.quit();
    }
    
    /**
     *  Test MLST
     */
    public void testIpswitch() throws Exception {

        log.debug("testIpswitch()");
        
        tools.setHost("ftp.ipswitch.com");
        tools.setUser("anonymous");
        tools.setPassword("test@test.com");

        connect();
                
        FTPFile ftpFile = ((FTPClient)ftp).fileDetails("index_renamed.html");
        
        log.debug("[" + ftpFile.toString() +"]");
                
        ftp.quit();
    }

    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestMLST.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

