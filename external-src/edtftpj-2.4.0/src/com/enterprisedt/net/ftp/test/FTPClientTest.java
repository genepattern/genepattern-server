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
 *      $Log: FTPClientTest.java,v $
 *      Revision 1.7  2007-08-09 00:10:53  hans
 *      Removed unused imports.
 *
 *      Revision 1.6  2005/06/03 11:27:05  bruceb
 *      comment update
 *
 *      Revision 1.5  2004/08/31 10:45:08  bruceb
 *      assign args correctly
 *
 *      Revision 1.4  2004/05/01 16:56:14  bruceb
 *      cleaned up and deprecated
 *
 *      Revision 1.3  2002/11/19 22:00:41  bruceb
 *      simple test case
 *
 *
 */

package com.enterprisedt.net.ftp.test;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

/**
 *  Basic test harness. No longer in use or maintained - see the
 *  JUnit tests and the demo program for current tests.
 *
 *
 *  @author         Bruce Blackshaw
 *  @version        $Revision: 1.7 $
 *
 */
public class FTPClientTest {

    /**
     *  Revision control id
     */
    public static final String cvsId = "$Id: FTPClientTest.java,v 1.7 2007-08-09 00:10:53 hans Exp $";

    /**
     *   Test harness
     */
    public static void main(String args[]) {
    	
    	Logger.setLevel(Level.ALL);
        
    	// we want remote host, user name and password
        if (args.length < 7) {
            System.out.println(args.length);
            usage();
            System.exit(1);
        }
        try {

            // assign args to make it clear
            String host = args[0];
            String user = args[1];
            String password = args[2];
            String filename = args[3];
            String directory = args[4];
            String mode = args[5];
            String connMode = args[6];

            // connect and test supplying port no.
            FTPClient ftp = new FTPClient();
            ftp.setControlEncoding("UTF-8");
            ftp.setRemoteHost(host);
            ftp.setRemotePort(21);
            ftp.connect();
            ftp.login(user, password);
            ftp.quote("LANG da-DK", new String[] { "200" });
//            ftp.dirDetails(".");
            ftp.chdir("test");
            ftp.quit();
            if (true)
            	return;

            // connect again
            ftp = new FTPClient();
            ftp.setRemoteHost(host);

            ftp.login(user, password);
            
            ftp.dir(".", false);

            // binary transfer
            if (mode.equalsIgnoreCase("BINARY")) {
                ftp.setType(FTPTransferType.BINARY);
            }
            else if (mode.equalsIgnoreCase("ASCII")) {
                ftp.setType(FTPTransferType.ASCII);
            }
            else {
                System.out.println("Unknown transfer type: " + args[5]);
                System.exit(-1);
            }

            // PASV or active?
            if (connMode.equalsIgnoreCase("PASV")) {
                ftp.setConnectMode(FTPConnectMode.PASV);
            }
            else if (connMode.equalsIgnoreCase("ACTIVE")) {
                ftp.setConnectMode(FTPConnectMode.ACTIVE);
            }
            else {
                System.out.println("Unknown connect mode: " + args[6]);
                System.exit(-1);
            }

            // change dir
            ftp.chdir(directory);

            // put a local file to remote host
            ftp.put(filename, filename);

            // get bytes
            byte[] buf = ftp.get(filename);
            System.out.println("Got " + buf.length + " bytes");

            // append local file
            try {
                ftp.put(filename, filename, true);
            }
            catch (FTPException ex) {
                System.out.println("Append failed: " + ex.getMessage());
            }

            // get bytes again - should be 2 x
            buf = ftp.get(filename);
            System.out.println("Got " + buf.length + " bytes");

            // rename
            ftp.rename(filename, filename + ".new");

            // get a remote file - the renamed one
            ftp.get(filename + ".tst", filename + ".new");

            // ASCII transfer
            ftp.setType(FTPTransferType.ASCII);

            // test that list() works
            String[] listing = ftp.dir(".");
            for (int i=0; i<listing.length; i++)
            	System.out.println(listing[i]);

            // test that dir() works in full mode
            String[] listings = ftp.dir(".", true);
            for (int i = 0; i < listings.length; i++)
                System.out.println(listings[i]);

            // try system()
            System.out.println(ftp.system());

            // try pwd()
            System.out.println(ftp.pwd());

            ftp.quit();
        }
        catch (Exception ex) {
            System.out.println("Caught exception: " + ex.getMessage());
        }
    }


    /**
     *  Basic usage statement
     */
    public static void usage() {

        System.out.println("Usage: ");
        System.out.println("com.enterprisedt.net.ftp.FTPClientTest " +
                           "remotehost user password filename directory " +
                           "(ascii|binary) (active|pasv)");
    }

}
