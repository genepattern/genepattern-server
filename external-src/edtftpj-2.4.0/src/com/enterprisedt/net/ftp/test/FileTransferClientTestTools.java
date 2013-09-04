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
*        $Log: FileTransferClientTestTools.java,v $
*        Revision 1.5  2011-03-28 03:41:41  hans
*        Made logger static.
*
*        Revision 1.4  2008-06-06 02:11:18  bruceb
*        tweaks to pass in port range
*
*        Revision 1.3  2008-06-06 00:19:51  bruceb
*        changes to get the FTC client
*
*        Revision 1.2  2008-06-03 05:52:39  bruceb
*        reconnect changes
*
*        Revision 1.1  2007-12-18 07:55:19  bruceb
*        prepare for FileTransferClient
*
*        Revision 1.4  2006/10/27 15:21:37  bruceb
*        renamed logger
*
*        Revision 1.3  2006/02/16 19:58:43  hans
*        Fixed comments
*
*        Revision 1.2  2005/08/26 17:52:06  bruceb
*        passive ip address setting
*
*        Revision 1.1  2005/07/22 10:29:38  bruceb
*        test framework changes
*
*/

package com.enterprisedt.net.ftp.test;

import java.util.Properties;

import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.net.ftp.FileTransferClientInterface;
import com.enterprisedt.util.debug.Logger;

/**
*  Base class for login tools
*
*  @author         Bruce Blackshaw
*  @version        $Revision: 1.5 $
*/
public class FileTransferClientTestTools extends FTPTestTools {
    
    /**
     *  Log stream
     */
    protected static Logger log = Logger.getLogger("FileTransferClientTestTools");
            
    /**
     * Set test properties for connecting
     * 
     * @param props     properties obj
     */
    public void setProperties(Properties props) {
        super.setProperties(props);         
    }
    
    /**
     * Connect to the remote host
     * 
     * @return          connected FTPClientInterface
     * @throws Exception
     */
	public FTPClientInterface connect() throws Exception {
        // connect
	    FileTransferClient client = createClient();
        client.connect();	
        return new FileTransferClientAdapter(client);
    }
	
    /**
     * Connect to the remote host supplying an active port range
     * 
     * @return          connected FTPClientInterface
     * @throws Exception
     */
    public FTPClientInterface connect(int lowest, int highest) throws Exception {
        // connect
        FileTransferClient client = createClient();
        client.getAdvancedFTPSettings().setActivePortRange(lowest, highest);
        client.connect();   
        return new FileTransferClientAdapter(client);
    }
    
    private FileTransferClient createClient() throws Exception {
        FileTransferClient client = new FileTransferClient();
        client.setRemoteHost(host);
        client.setTimeout(timeout);
        client.getAdvancedFTPSettings().setConnectMode(connectMode);
        client.setUserName(user);
        client.setPassword(password);
        return client;
    }
	
	public void reconnect(FTPClientInterface ftp) throws Exception {
	    FileTransferClientAdapter client = (FileTransferClientAdapter)ftp;
        FileTransferClientInterface ftc = client.getFileTransferClient();
        ftc.connect();
    }

}
