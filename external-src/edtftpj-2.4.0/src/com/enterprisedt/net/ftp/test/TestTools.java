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
*        $Log: TestTools.java,v $
*        Revision 1.9  2011/03/28 03:41:41  hans
*        Made logger static.
*
*        Revision 1.8  2008-06-06 02:11:18  bruceb
*        tweaks to pass in port range
*
*        Revision 1.7  2008-06-03 05:52:39  bruceb
*        reconnect changes
*
*        Revision 1.6  2007/04/04 05:34:20  bruceb
*        made non-abstract
*
*        Revision 1.5  2007/01/15 22:57:06  bruceb
*        can now modify username, pwd etc
*
*        Revision 1.4  2006/10/27 15:23:12  bruceb
*        renamed logger
*
*        Revision 1.3  2006/02/16 19:49:14  hans
*        Fixed comments.
*
*        Revision 1.2  2005/10/15 22:45:36  bruceb
*        added getters
*
*        Revision 1.1  2005/07/15 14:41:12  bruceb
*        needed for rework of unit testing structure
*
*/

package com.enterprisedt.net.ftp.test;

import java.util.Properties;

import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.util.debug.Logger;

/**
*  Base class for login tools
*
*  @author         Bruce Blackshaw
*  @version        $Revision: 1.9 $
*/
public class TestTools {
    
    /**
     *  Log stream
     */
    protected static Logger log = Logger.getLogger("TestTools");
    
    /**
     * Test properties
     */
    protected Properties props;
    
    /**
     *  Test user
     */
    protected String user;

    /**
     *  User password
     */
    protected String password;

    /**
     *  Remote test host
     */
    protected String host;

    /**
     *  Socket timeout
     */
    protected int timeout;
        
    /**
     * Constructor
     */
    public TestTools() {}
    
    
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * Get password
     * 
     * @return Password
     */    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get username 
     * 
     * @return User
     */
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * Get host 
     * 
     * @return Host
     */
    public String getHost() {
        return host;
    }  
    
    public void setHost(String host) {
        this.host = host;
    }


    /**
     * Set test properties for connecting
     * 
     * @param props     properties obj
     */
    public void setProperties(Properties props) {
        this.props = props;    
        
        user = props.getProperty("ftptest.user");
        password = props.getProperty("ftptest.password");
        
        host = props.getProperty("ftptest.host");
        
        // socket timeout
        String timeoutStr = props.getProperty("ftptest.timeout");
        this.timeout = Integer.parseInt(timeoutStr);
    }
    
    /**
     * Connect to the remote host
     * 
     * @return          connected FTPClient
     * @throws Exception
     */
	public FTPClientInterface connect() throws Exception {
	    throw new Exception("connect() not implemented");
    }
	
	/**
     * Connect to the remote host using supplied active port range
     * 
     * @return          connected FTPClient
     * @throws Exception
     */
    public FTPClientInterface connect(int lowest, int highest) throws Exception {
        throw new Exception("connect(int lowest, int highest) not implemented");
    }
	
	/**
	 * Reconnects 
	 * 
	 * @param ftp
	 * @throws Exception
	 */
	public void reconnect(FTPClientInterface ftp) throws Exception {
        throw new Exception("reconnect() not implemented");
    }
}
