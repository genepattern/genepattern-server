/**
 *
 *  edtFTPj client library.
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
 *        $Log: FTPTestCase.java,v $
 *        Revision 1.30  2012/11/15 06:11:02  bruceb
 *        edtFTPj
 *
 *        Revision 1.29  2011-03-28 03:41:41  hans
 *        Made logger static.
 *
 *        Revision 1.28  2010-10-22 04:21:25  bruceb
 *        call clearAppenders() in tearDown()
 *
 *        Revision 1.27  2009-01-15 03:41:13  bruceb
 *        lowPort calc change
 *
 *        Revision 1.26  2008-06-06 02:11:18  bruceb
 *        tweaks to pass in port range
 *
 *        Revision 1.25  2008-06-03 05:52:39  bruceb
 *        reconnect changes
 *
 *        Revision 1.24  2008-05-14 05:52:10  bruceb
 *        allow empty lists
 *
 *        Revision 1.23  2008-04-17 04:52:10  bruceb
 *        use commandline supplied tools class in preference
 *
 *        Revision 1.22  2007-05-29 04:16:16  bruceb
 *        connected() test
 *
 *        Revision 1.21  2007/05/03 04:21:30  bruceb
 *        rename logger
 *
 *        Revision 1.20  2007/04/24 01:35:40  bruceb
 *        set to DEBUG
 *
 *        Revision 1.19  2007/04/21 04:14:20  bruceb
 *        *** empty log message ***
 *
 *        Revision 1.18  2007/02/26 07:14:54  bruceb
 *        more info on char pos if mismatch in file comparison
 *
 *        Revision 1.17  2006/11/14 11:41:02  bruceb
 *        added localBigTextFile
 *
 *        Revision 1.16  2005/07/15 17:30:06  bruceb
 *        rework of unit testing structure
 *
 *        Revision 1.15  2005/07/11 21:15:31  bruceb
 *        minor tweak re listings
 *
 *        Revision 1.14  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.14  2005/05/15 19:45:36  bruceb
 *        changes for testing setActivePortRange
 *
 *        Revision 1.13  2005/03/18 11:12:44  bruceb
 *        deprecated constructors
 *
 *        Revision 1.12  2005/01/14 18:07:19  bruceb
 *        bulk count added
 *
 *        Revision 1.11  2004/10/18 15:58:58  bruceb
 *        test encoding constructor
 *
 *        Revision 1.10  2004/08/31 10:44:49  bruceb
 *        minor tweaks re compile warnings
 *
 *        Revision 1.9  2004/07/23 08:33:44  bruceb
 *        enable testing for strict replies or not
 *
 *        Revision 1.8  2004/06/25 12:03:54  bruceb
 *        get mode from sys property
 *
 *        Revision 1.7  2004/05/11 21:58:05  bruceb
 *        getVersion() added
 *
 *        Revision 1.6  2004/05/01 17:05:59  bruceb
 *        cleaned up and deprecated
 *
 *        Revision 1.5  2004/04/17 18:38:38  bruceb
 *        tweaks for ssl and new parsing functionality
 *
 *        Revision 1.4  2004/04/05 20:58:41  bruceb
 *        latest hans tweaks to tests
 *
 *        Revision 1.3  2003/11/02 21:51:44  bruceb
 *        fixed bug re transfer mode not being set
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.util.debug.FileAppender;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

/**
 *  Generic JUnit test class for FTP, that provides some
 *  useful methods for subclasses that implement the actual
 *  test cases
 *
 *  @author         Bruce Blackshaw
 *  @version        $Revision: 1.30 $
 */
abstract public class FTPTestCase extends TestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: FTPTestCase.java,v 1.30 2012/11/15 06:11:02 bruceb Exp $";

    /**
     *  Log stream
     */
    protected static Logger log = Logger.getLogger("FTPTestCase");

    /**
     *  Reference to the FTP client
     */
    protected FTPClientInterface ftp;

    /**
     * Lowest port
     */
    protected int lowPort = 10000 + (int)(Math.random()*20000.0);
    
    /**
     * Highest port
     */
    protected int highPort = lowPort + 15;

    /**
     *  Remote directory that remote test files/dirs are in
     */
    protected String testdir;

    /**
     *  Remote text file
     */
    protected String remoteTextFile;

    /**
     *  Local text file
     */
    protected String localTextFile;
    
    /**
     *  Local text file
     */
    protected String localUnixTextFile;


    /**
     *  Remote binary file
     */
    protected String remoteBinaryFile;

    /**
     *  Local binary file
     */
    protected String localBinaryFile;

    /**
     *  Local empty file
     */
    protected String localEmptyFile;

    /**
     *  Remote empty file
     */
    protected String remoteEmptyFile;

    /**
     *  Remote empty dir
     */
    protected String remoteEmptyDir;
    
    /**
     * Big local file for testing
     */
    protected String localBigFile;
    
    /**
     * Big local text file for testing
     */
    protected String localBigTextFile;
    
    /**
     * Local test dir
     */
    protected String localTestDir;
    
    /**
     * Log directory
     */
    protected String logDir;
    
    /**
     * Local data directory
     */
    protected String localDataDir;
    
    /**
     * Number of operations for stress testing
     */
    protected int bulkCount = 100;
        
    /**
     *  Loaded properties
     */
    protected Properties props = new Properties();
    
    /**
     * Server specific tools 
     */
    protected TestTools tools = null;
    
    /**
     *  Initialize test properties
     */
    public FTPTestCase() {     
        
        Logger.setLevel(Level.DEBUG);
        
        String propsfile = System.getProperty("ftptest.properties.filename", "test.properties");
                
        try {
            props.load(new FileInputStream(propsfile));  
        }
        catch (IOException ex) {
            System.out.println("Failed to open " + propsfile);
            System.exit(-1);
        }       
               
        // various test files and dirs
        testdir = props.getProperty("ftptest.testdir");
        localTextFile = props.getProperty("ftptest.file.local.text");
        localUnixTextFile = props.getProperty("ftptest.file.local.text.unix");
        localTestDir = props.getProperty("ftptest.dir.local");
        localDataDir = props.getProperty("ftptest.datadir.local", "data");
        if (!localDataDir.endsWith(File.separator))
            localDataDir += File.separator;
        localBigFile = props.getProperty("ftptest.file.local.big");
        localBigTextFile = props.getProperty("ftptest.file.local.big.text");
        remoteTextFile = props.getProperty("ftptest.file.remote.text");
        localBinaryFile = props.getProperty("ftptest.file.local.binary");
        remoteBinaryFile = props.getProperty("ftptest.file.remote.binary");
        localEmptyFile = props.getProperty("ftptest.file.local.empty");
        remoteEmptyFile = props.getProperty("ftptest.file.remote.empty");
        remoteEmptyDir = props.getProperty("ftptest.dir.remote.empty");
        String bulkCountStr = props.getProperty("ftptest.bulkcount");
        logDir = props.getProperty("ftptest.logdir", "log");
        if (bulkCountStr != null)
            bulkCount = Integer.parseInt(bulkCountStr);
        String lowPortStr = props.getProperty("ftptest.lowport");
        if (lowPortStr != null)
            lowPort = Integer.parseInt(lowPortStr);
        String highPortStr = props.getProperty("ftptest.highport");
        if (highPortStr != null)
            highPort = Integer.parseInt(highPortStr);
        
        String testToolsClass = System.getProperty("ftptest.testtools", props.getProperty("ftptest.testtools"));
        tools = loadTestTools(testToolsClass);
        tools.setProperties(props);
    }

    /**
     * Load the test tools class
     * 
     * @param testToolsClass    full class name
     * @return
     */
    private TestTools loadTestTools(String testToolsClass) {
        try {
            Class clazz = Class.forName(testToolsClass);
            return (TestTools)clazz.newInstance();
        }
        catch (Exception ex) {
            log.error("Failed to instantiate " + testToolsClass, ex);
        }
        return null;
    }

    /**
     *  Setup is called before running each test
     */    
    protected void setUp() throws Exception {
        Logger.addAppender(new FileAppender(logDir + File.separator + getLogName()));
    }
    
    /**
     *  Deallocate resources at close of each test
     */
    protected void tearDown() throws Exception {
        Logger.shutdown();
        Logger.clearAppenders();
    }

    /**
     *  Connect to the server and setup log stream
     */
    protected void connect() throws Exception {
    	ftp = tools.connect();
        assertEquals(true, ftp.connected());
        log.debug("connected successfully");
    }
    
    /**
     *  Connect to the server and setup log stream
     */
    protected void connect(int lowest, int highest) throws Exception {
        ftp = tools.connect(lowest, highest);
        assertEquals(true, ftp.connected());
        log.debug("connected successfully");
    }
    
    protected void reconnect(FTPClientInterface ftp) throws Exception {
        tools.reconnect(ftp);
        log.debug("Reconnected successfully");
    }
    
    /**
     *  Generate a random file name for testing
     *
     *  @return  random filename
     */
    protected String generateRandomFilename() {
        Date now = new Date();
        Long ms = new Long(now.getTime());
        return ms.toString();
    }

    /**
     *  Test to see if two buffers are identical, byte for byte
     *
     *  @param buf1   first buffer
     *  @param buf2   second buffer
     */
    protected void assertIdentical(byte[] buf1, byte[] buf2) 
        throws Exception {
        
        assertEquals(buf1.length, buf2.length);
        for (int i = 0; i < buf1.length; i++)
            assertEquals(buf1[i], buf2[i]);
    }

    /**
     *  Test to see if two files are identical, byte for byte
     *
     *  @param file1  name of first file
     *  @param file2  name of second file
     */
    protected void assertIdentical(String file1, String file2) 
        throws Exception {
        File f1 = new File(file1);
        File f2 = new File(file2);
        assertIdentical(f1, f2);
    }

    /**
     *  Test to see if two files are identical, byte for byte
     *
     *  @param file1  first file object
     *  @param file2  second file object
     */
    protected void assertIdentical(File file1, File file2) 
        throws Exception {

        BufferedInputStream is1 = null;
        BufferedInputStream is2 = null;
        try {
            // check lengths first
            assertEquals(file1.length(), file2.length());
            log.debug("Identical size [" + file1.getName() + 
                        "," + file2.getName() + "]"); 

            // now check each byte
            is1 = new BufferedInputStream(new FileInputStream(file1));
            is2 = new BufferedInputStream(new FileInputStream(file2));
            int ch1 = 0;
            int ch2 = 0;
            int count = 0;
            try {
                while ((ch1 = is1.read()) != -1 && 
                       (ch2 = is2.read()) != -1) {
                    count++;
                    assertEquals(ch1, ch2);
                }    
            }
            catch (AssertionFailedError e) {
                log.debug("Comparison failed on char position=" + count);
                throw e;
            }
            log.debug("Contents equal");
        }
        catch (IOException ex) {
            fail("Caught exception: " + ex.getMessage());
        }
        finally {
            if (is1 != null)
                is1.close();
            if (is2 != null)
                is2.close();
        }
    }
    
    
    /**
     * Transfer back and forth multiple times
     */
    protected void bulkTransfer(String localFile) throws Exception {
        // put to a random filename muliple times
        String filename = generateRandomFilename();
        log.debug("Bulk transfer count=" + bulkCount);
        for (int i = 0; i < bulkCount; i++) {
            ftp.put(localDataDir + localFile, filename);

            // get it back
            ftp.get(localDataDir + filename, filename);
            
            // delete remote file
            ftp.delete(filename);
        }

        // check equality of local files
        assertIdentical(localDataDir + localFile, localDataDir + filename);

        // and delete local file
        File local = new File(localDataDir + filename);
        local.delete();
    }
    
    
    /**
     *  Helper method for dumping a listing
     * 
     *  @param list   directory listing to print
     */
    protected void print(String[] list) {
        log.debug("Directory listing:");
        if (list == null) {
            log.debug("Empty");
            return;
        }
        for (int i = 0; i < list.length; i++)
            log.debug(list[i]);
        log.debug("Listing complete");
    }
    
    /**
     *  Helper method for dumping a listing
     * 
     *  @param list   directory listing to print
     */
    protected void print(File[] list) {
        log.debug("Directory listing:");
        if (list == null) {
            log.debug("Empty");
            return;
        }
        for (int i = 0; i < list.length; i++)
            log.debug(list[i].getName());
        log.debug("Listing complete");
    }
    
    /**
     *  Helper method for dumping a listing
     * 
     *  @param list   directory listing to print
     */
    protected void print(FTPFile[] list) {
        log.debug("Directory listing:");
        if (list == null) {
            log.debug("Empty");
            return;
        }
        for (int i = 0; i < list.length; i++)
            log.debug(list[i].toString());
        log.debug("Listing complete");
    }    
        
    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    abstract protected String getLogName();
}

