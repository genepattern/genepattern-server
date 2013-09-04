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
*        $Log: FTPTestTools.java,v $
*        Revision 1.10  2012/11/15 06:10:36  bruceb
*        integrity check props
*
*        Revision 1.9  2011-03-28 03:41:41  hans
*        Made logger static.
*
*        Revision 1.8  2008-06-06 02:11:18  bruceb
*        tweaks to pass in port range
*
*        Revision 1.7  2008-06-03 05:52:39  bruceb
*        reconnect changes
*
*        Revision 1.6  2008-05-22 04:21:14  bruceb
*        small tweaks
*
*        Revision 1.5  2006/10/27 15:44:44  bruceb
*        renamed logger, put deprecated constructor call back in
*
*        Revision 1.4  2006/10/11 08:59:20  hans
*        Fixed use of deprecated FTPClient constructor.
*
*        Revision 1.3  2005/11/09 21:21:09  bruceb
*        detect transfer mode on
*
*        Revision 1.2  2005/08/26 17:48:33  bruceb
*        passive ip address setting
*
*        Revision 1.1  2005/07/15 14:41:12  bruceb
*        needed for rework of unit testing structure
*
*/

package com.enterprisedt.net.ftp.test;

import java.util.Properties;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPControlSocket;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Logger;

/**
*  Base class for login tools
*
*  @author         Bruce Blackshaw
*  @version        $Revision: 1.10 $
*/
public class FTPTestTools extends TestTools {
    
    /**
     *  Log stream
     */
    protected static Logger log = Logger.getLogger("FTPTestTools");
        
    /**
     * If true use deprecated constructors
     */
    protected boolean useDeprecatedConstructors = false;
    
    /**
     *  Connect mode for test
     */
    protected FTPConnectMode connectMode;
    
    /**
     *  Strict reply checking?
     */
    protected boolean strictReplies = true;
    
    /**
     * If true, uses the original host IP if an internal IP address
     * is returned by the server in PASV mode
     */   
    protected boolean autoPassiveIPSubstitution = false;
    
    /**
     * Used in subclasses for integrity checking of transfers
     */
    protected boolean integrityCheck = true;
    
    /**
     * Set test properties for connecting
     * 
     * @param props     properties obj
     */
    public void setProperties(Properties props) {
        super.setProperties(props); 
        
        String strict = props.getProperty("ftptest.strictreplies");
        if (strict != null && strict.equalsIgnoreCase("false"))
            this.strictReplies = false;
        else
            this.strictReplies = true;
        
        String autoPassive = props.getProperty("ftptest.autopassivesubstitution");
        if (autoPassive != null && autoPassive.equalsIgnoreCase("true"))
            this.autoPassiveIPSubstitution = true;
        else
            this.autoPassiveIPSubstitution = false;
        
        String deprecatedConstructorsStr = props.getProperty("ftptest.deprecatedconstructors");
        if (deprecatedConstructorsStr != null)
            useDeprecatedConstructors = Boolean.getBoolean(deprecatedConstructorsStr);
        
        // active or passive?
        String connectModeStr = System.getProperty("ftptest.connectmode");
        if (connectModeStr != null && connectModeStr.equalsIgnoreCase("active"))
            this.connectMode = FTPConnectMode.ACTIVE;
        else
            this.connectMode = FTPConnectMode.PASV;
        
        String integrityCheckStr = props.getProperty("ftptest.integritycheck", "true");
        if (System.getProperty("ftptest.integritycheck") != null)
            integrityCheckStr = System.getProperty("ftptest.integritycheck");
        if (integrityCheckStr.equalsIgnoreCase("false")) {           
            integrityCheck = false;
        }
    }
    
    /**
     * Connect to the remote host
     * 
     * @return          connected FTPClient
     * @throws Exception
     */
    public FTPClientInterface connect() throws Exception {
        FTPClient ftp = createClient();
        ftp.connect();	
        ftp.login(user, password);
        return ftp;
    }
    
    private FTPClient createClient() throws Exception {
     // connect
        FTPClient ftp = null;
        if (!useDeprecatedConstructors) {
            ftp = new FTPClient();
            ftp.setRemoteHost(host);
            ftp.setTimeout(timeout);
            
        }
        else {
            ftp = new FTPClient(host, FTPControlSocket.CONTROL_PORT, timeout);
        }
        ftp.setAutoPassiveIPSubstitution(autoPassiveIPSubstitution);
        ftp.setConnectMode(connectMode);
        ftp.setDetectTransferMode(true);
        if (!strictReplies) {
            log.warn("Strict replies not enabled");
            ftp.setStrictReturnCodes(false);
        }
        return ftp;
    }
    
    /**
     * Connect to the remote host supplying an active port range
     * 
     * @return          connected FTPClientInterface
     * @throws Exception
     */
    public FTPClientInterface connect(int lowest, int highest) throws Exception {
        FTPClient ftp = createClient();
        ftp.setActivePortRange(lowest, highest);
        ftp.connect();  
        ftp.login(user, password);
        return ftp;
    }
    
    public void reconnect(FTPClientInterface ftp) throws Exception {
        FTPClient client = (FTPClient)ftp;
        client.connect();
        client.login(user, password);
    }

}
