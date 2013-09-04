/**
 *
 *  edtFTPj library.
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
 *        $Log: TestZip.java,v $
 *        Revision 1.1  2012/11/15 06:09:45  bruceb
 *        test zip filesystem
 *
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.enterprisedt.net.ftp.FTPTransferType;

/**
 *  Test zip filesystem
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.1 $
 */
public class TestZip extends FTPTestCase {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: TestZip.java,v 1.1 2012/11/15 06:09:45 bruceb Exp $";
    
    private String zipArchive ;
    private String zipArchiveRemote;
    private String zipDir;
    private String zipFile;

    /**
     *  Get name of log file
     *
     *  @return name of file to log to
     */
    protected String getLogName() {
        return "TestZip.log";
    }
    
    public TestZip() {
        initProperties();
    }
    
    private void initProperties() {
        zipArchive = props.getProperty("ftptest.ziparchive");
        zipArchiveRemote = props.getProperty("ftptest.ziparchive.remote");
        zipDir = props.getProperty("ftptest.zipdir");
        zipFile = props.getProperty("ftptest.zipfile");
    }

    /**
     *  Test retrieving a file from zip file
     */
    public void testRetrieveZip() throws Exception {

        log.info("testRetrieveZip()");
        
        
        
        try {

            connect();
            
            // monitor transfer progress
            ftp.setProgressMonitor(new TestProgressMonitor(), 250000);
    
            // move to test directory
            ftp.chdir(testdir);
            ftp.setType(FTPTransferType.BINARY);
            
            // put the zip file
            ftp.put(localDataDir + zipArchive, zipArchive);
            
            // navigate into zipfile
            ftp.chdir(zipArchiveRemote);
            
            // and to a dir within the zip file
            ftp.chdir(zipDir);
            
            // get internal zip file to a random filename
            String filename = generateRandomFilename();
            ftp.get(localDataDir + filename, zipFile);
            
            // back out of zip file
            ftp.chdir("..");
            ftp.chdir("..");
    
            // now get that file out of the local zip archive     
            ZipInputStream zipinputstream = 
                 new ZipInputStream(
                    new FileInputStream(localDataDir + zipArchive));
            
            ZipEntry zipentry = null;
            String zipFilePath = zipDir + "/" + zipFile;
            while ((zipentry = zipinputstream.getNextEntry()) != null) {
               if (zipentry.getName().equals(zipFilePath)) {
                    byte[] buf = new byte[1024];
                    FileOutputStream out = new FileOutputStream(localDataDir + zipFile);
                    int count = 0;
                    while ((count = zipinputstream.read(buf)) >= 0) {
                        out.write(buf, 0, count);
                    }
                    out.close();  
                    break;
                }    
            }
            zipinputstream.close();
            
       
            // delete remote file
            ftp.delete(zipArchive);
            
    
            // check equality of local files
            assertIdentical(localDataDir + zipFile, localDataDir + filename);
    
            // and delete local file
            File local = new File(localDataDir + filename);
            local.delete();
            local = new File(localDataDir + zipFile);
            local.delete();
    
            ftp.quit();
        }
        finally {
            ftp.quitImmediately();
        }
    }    
    
    
    /**
     *  Automatic test suite construction
     *
     *  @return  suite of tests for this class
     */
    public static Test suite() {
        return new TestSuite(TestZip.class);
    } 

    /**
     *  Enable our class to be run, doing the
     *  tests
     */
    public static void main(String[] args) {       
        junit.textui.TestRunner.run(suite());
    }
}

