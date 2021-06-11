/**
 *
 *  edtFTPj
 *
 *  Copyright (C) 2000-2003 Enterprise Distributed Technologies Ltd
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
 *        $Log: FTPControlSocket.java,v $
 *        Revision 1.61  2011-12-14 07:22:52  bruceb
 *        isSecureMode() added
 *
 *        Revision 1.60  2011-02-07 01:14:29  bruceb
 *        fix multiline bug
 *
 *        Revision 1.59  2011-01-20 23:29:35  bruceb
 *        line length check
 *
 *        Revision 1.58  2011-01-16 22:46:16  bruceb
 *        trim control lines
 *
 *        Revision 1.57  2010-10-08 04:35:07  bruceb
 *        fix so that the reply obj includes all lines of data
 *
 *        Revision 1.56  2010-04-27 08:35:13  bruceb
 *        mask ACCT arg
 *
 *        Revision 1.55  2010-04-26 15:50:27  bruceb
 *        add usingProxy() and remote useAutoPassiveIPSubstitution()
 *
 *        Revision 1.54  2009-10-18 23:59:30  bruceb
 *        added useAutoPassiveIPSubstitution()
 *
 *        Revision 1.53  2009-09-02 22:02:10  bruceb
 *        fix re wildcard address used for PORT
 *
 *        Revision 1.52  2009-07-17 03:04:22  bruceb
 *        proxy changes
 *
 *        Revision 1.51  2009-04-14 01:47:26  bruceb
 *        PASV/PORT callbacks
 *
 *        Revision 1.50  2009-01-21 04:46:36  bruceb
 *        better logging of control channel messages on failure
 *
 *        Revision 1.49  2009-01-15 03:39:23  bruceb
 *        introduce ControlChannelException
 *
 *        Revision 1.48  2008-11-25 01:04:07  bruceb
 *        permit \n as well as \r\n to end reply lines
 *
 *        Revision 1.47  2008-10-29 03:25:23  bruceb
 *        fix to allow \r in replies without it being a EOL
 *
 *        Revision 1.46  2008-08-26 04:35:40  bruceb
 *        MalformedReplyException added
 *
 *        Revision 1.45  2008-07-15 05:40:48  bruceb
 *        fixed bug where socket not closed
 *
 *        Revision 1.44  2008-05-22 04:20:55  bruceb
 *        moved stuff to internal etc
 *
 *        Revision 1.43  2008-05-14 05:51:53  bruceb
 *        added code to cycle through port range and ignore bind exceptions
 *
 *        Revision 1.42  2008-04-02 23:29:21  bruceb
 *        improved "null reply" message
 *
 *        Revision 1.41  2008-03-13 00:21:27  bruceb
 *        421 exception
 *
 *        Revision 1.40  2007-11-13 07:14:04  bruceb
 *        ListenOnAllInterfaces
 *
 *        Revision 1.39  2007-11-07 23:53:14  bruceb
 *        refactoring for FXP
 *
 *        Revision 1.38  2007-10-23 07:20:42  bruceb
 *        fixed doco spelling mistake
 *
 *        Revision 1.37  2007/02/07 23:03:10  bruceb
 *        added close()
 *
 *        Revision 1.36  2007/02/04 23:03:30  bruceb
 *        extra codes and extra debug
 *
 *        Revision 1.35  2007/01/10 02:36:53  bruceb
 *        sendPORTCommand takes a port number now
 *
 *        Revision 1.34  2006/10/27 15:43:23  bruceb
 *        added connect with timeout
 *
 *        Revision 1.33  2006/10/17 10:28:15  bruceb
 *        refactored to get sendPORTCommand()
 *
 *        Revision 1.32  2006/10/11 08:51:44  hans
 *        made cvsId final
 *
 *        Revision 1.31  2006/08/25 20:40:54  hans
 *        Fixed documentation.
 *
 *        Revision 1.30  2006/07/27 14:11:00  bruceb
 *        IPV6 changes (for subclass)
 *
 *        Revision 1.29  2006/05/23 00:17:42  bruceb
 *        apply timeout to active data socket
 *
 *        Revision 1.28  2006/03/09 21:44:24  bruceb
 *        made PASV parsing cleaner
 *
 *        Revision 1.27  2006/02/16 19:47:57  hans
 *        Changed comment
 *
 *        Revision 1.26  2006/02/09 09:00:44  bruceb
 *        fix to allow for missing end bracket re PASV response
 *
 *        Revision 1.25  2005/09/21 08:38:53  bruceb
 *        allow 230 to be initial server response
 *
 *        Revision 1.24  2005/09/02 21:02:44  bruceb
 *        bug fix in readreply
 *
 *        Revision 1.23  2005/08/26 17:48:26  bruceb
 *        passive ip address setting
 *
 *        Revision 1.22  2005/08/04 22:08:42  hans
 *        Remember encoding so that it can be reused when initStreams is called in places other than the constructor
 *
 *        Revision 1.21  2005/06/10 15:43:59  bruceb
 *        message length check
 *
 *        Revision 1.20  2005/06/03 11:26:25  bruceb
 *        comment change
 *
 *        Revision 1.21  2005/05/15 20:44:15  bruceb
 *        removed debug
 *
 *        Revision 1.20  2005/05/15 19:46:28  bruceb
 *        changes for testing setActivePortRange + STOR accepting 350 nonstrict
 *
 *        Revision 1.19  2005/03/26 12:35:45  bruceb
 *        allow for blank lines in server replies
 *
 *        Revision 1.18  2004/11/19 08:28:10  bruceb
 *        added setPORTIP()
 *
 *        Revision 1.17  2004/10/18 15:56:46  bruceb
 *        set encoding for sock, remove sendCommandOld etc
 *
 *        Revision 1.16  2004/09/18 09:33:47  bruceb
 *        1.1.8 tweaks
 *
 *        Revision 1.15  2004/08/31 10:46:59  bruceb
 *        restructured reply code
 *
 *        Revision 1.14  2004/07/23 23:29:57  bruceb
 *        sendcommand public again
 *
 *        Revision 1.13  2004/07/23 08:30:40  bruceb
 *        restructured re non-strict replies
 *
 *        Revision 1.12  2004/05/22 16:52:57  bruceb
 *        message listener
 *
 *        Revision 1.11  2004/05/01 17:05:15  bruceb
 *        Logger stuff added
 *
 *        Revision 1.10  2004/03/23 20:25:47  bruceb
 *        added US-ASCII to control stream constructor
 *
 *        Revision 1.9  2003/11/15 11:23:55  bruceb
 *        changes required for ssl subclasses
 *
 *        Revision 1.6  2003/05/31 14:53:44  bruceb
 *        1.2.2 changes
 *
 *        Revision 1.5  2003/01/29 22:46:08  bruceb
 *        minor changes
 *
 *        Revision 1.4  2002/11/19 22:01:25  bruceb
 *        changes for 1.2
 *
 *        Revision 1.3  2001/10/09 20:53:46  bruceb
 *        Active mode changes
 *
 *        Revision 1.1  2001/10/05 14:42:04  bruceb
 *        moved from old project
 *
 *
 */

package com.enterprisedt.net.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Random;
import java.util.Vector;

import com.enterprisedt.net.ftp.internal.FTPActiveDataSocket;
import com.enterprisedt.net.ftp.internal.FTPDataSocket;
import com.enterprisedt.net.ftp.internal.FTPPassiveDataSocket;
import com.enterprisedt.util.debug.Logger;
import com.enterprisedt.util.proxy.PlainSocket;
import com.enterprisedt.util.proxy.StreamSocket;

/**
 *  Supports client-side FTP operations
 *
 *  @author         Bruce Blackshaw
 *  @version        $Revision: 1.61 $
 *
 */
 public class FTPControlSocket {

     /**
      *  Revision control id
      */
     public static final String cvsId = "@(#)$Id: FTPControlSocket.java,v 1.61 2011-12-14 07:22:52 bruceb Exp $";

     /**
      *   Standard FTP end of line sequence
      */
     static final String EOL = "\r\n";
     
     /**
      * Used for ASCII translation
      */
     private static final byte CARRIAGE_RETURN = 13;

     /**
      * Used for ASCII translation
      */
     private static final byte LINE_FEED = 10;
     
     /**
      * Maximum number of auto retries in active mode
      */
     public static final int MAX_ACTIVE_RETRY = 100;

     /**
      *   The default and standard control port number for FTP
      */
     public static final int CONTROL_PORT = 21;
     
     /**
      *   Used to flag messages
      */
     private static final String DEBUG_ARROW = "---> ";
     
     /**
      *   Start of password message
      */
     private static final String PASSWORD_MESSAGE = DEBUG_ARROW + "PASS";
     
     /**
      *   Start of password message
      */
     private static final String ACCT_MESSAGE = DEBUG_ARROW + "ACCT";
     
     /**
      * Logging object
      */
     private static Logger log = Logger.getLogger("FTPControlSocket");

     /**
      * Use strict return codes if true
      */
     private boolean strictReturnCodes = true;
     
     /**
      * Listen to all interfaces in active mode
      */
     protected boolean listenOnAllInterfaces = true;

     /**
      *  The underlying socket.
      */
     protected StreamSocket controlSock = null;

     /**
      *  The write that writes to the control socket
      */
	 protected Writer writer = null;

     /**
      *  The reader that reads control data from the
      *  control socket
      */
     protected Reader reader = null;
     
     /**
      * Message listener
      */
     private FTPMessageListener messageListener = null;

     /**
      * IP address we force PORT to send - useful with certain
      * NAT configurations
      */
     protected String forcedActiveIP;

     /**
      * Lowest port in active mode port range
      */
     private int lowPort = -1;

     /**
      * Highest port in active mode port range
      */
     private int highPort = -1;

     /**
      * Next port number to use. 0 indicates let Java decide
      */
     private int nextPort = 0;
     
     /**
      * Character encoding.
      */
     private String encoding;
     
     /**
      * The remote address to connect to
      */
     protected InetAddress remoteAddr;
     
     /**
      * If true, uses the original host IP if an internal IP address
      * is returned by the server in PASV mode
      */     
     protected boolean autoPassiveIPSubstitution = false;
     
     /**
      * Pasv callback method
      */
     protected DataChannelCallback dataChannelCallback = null;
     
     /**
      *   Constructor. Performs TCP connection and
      *   sets up reader/writer. Allows different control
      *   port to be used
      *
      *   @param   remoteAddr       Remote inet address
      *   @param   controlPort      port for control stream
      *   @param   timeout          the length of the timeout, in milliseconds
      *   @param   encoding         character encoding used for data
      *   @param   messageListener  listens for messages
      */
     protected FTPControlSocket(InetAddress remoteAddr, int controlPort, int timeout, 
                      String encoding, FTPMessageListener messageListener)
         throws IOException, FTPException {
         
         this(remoteAddr, PlainSocket.createPlainSocket(remoteAddr, controlPort, timeout), 
                     timeout, encoding, messageListener);
     }

    /**
     * Constructs a new <code>FTPControlSocket</code> using the given
     * <code>Socket</code> object.
     * 
     * @param remoteAddr       the remote address
	 * @param controlSock      Socket to be used. 
	 * @param timeout          Timeout to be used.
     * @param encoding         character encoding used for data
     * @param messageListener  listens for messages
     * 
	 * @throws IOException Thrown if no connection response could be read from the server.
	 * @throws FTPException Thrown if the incorrect connection response was sent by the server.
	 */
	protected FTPControlSocket(InetAddress remoteAddr, StreamSocket controlSock, int timeout, 
                                String encoding, FTPMessageListener messageListener)
		throws IOException, FTPException {
         
        this.remoteAddr = remoteAddr;
		this.controlSock = controlSock;
        this.messageListener = messageListener;
        this.encoding = encoding;
         
        try {
    		setTimeout(timeout);
    		initStreams();
    		validateConnection();
        }
        catch (IOException ex) {
            log.error("Failed to initialize control socket", ex);
            controlSock.close();
            controlSock = null;
            throw ex;
        }
        catch (FTPException ex) {
            log.error("Failed to initialize control socket", ex);
            controlSock.close();
            controlSock = null;
            throw ex;
        }
    }
    
    /**
     * Set automatic substitution of the remote host IP on if
     * in passive mode
     * 
     * @param autoPassiveIPSubstitution true if set to on, false otherwise
     */
    protected void setAutoPassiveIPSubstitution(boolean autoPassiveIPSubstitution) {
        this.autoPassiveIPSubstitution = autoPassiveIPSubstitution;        
    }

     /**
      *   Checks that the standard 220 reply is returned
      *   following the initiated connection. Allow 230 as some
      *   proxy servers return it
      */
     private void validateConnection()
         throws IOException, FTPException {

         FTPReply reply = readReply();
         String[] validCodes = {"220", "230"};
         validateReply(reply, validCodes);
     }


     /**
      *  Initialize the reader/writer streams for this connection.
      */
     protected void initStreams()
         throws IOException {

         // input stream
         InputStream is = controlSock.getInputStream();
         reader = new InputStreamReader(is, encoding);

         // output stream
         OutputStream os = controlSock.getOutputStream();
         writer = new OutputStreamWriter(os, encoding);
     }
     
     /**
      * Is this socket in secure mode?
      * 
      * @return true if secure mode
      */
     public boolean isSecureMode() {
         return false;
     }


     /**
      *  Get the name of the remote host
      *
      *  @return  remote host name
      */
     String getRemoteHostName() {
         InetAddress addr = controlSock.getInetAddress();
         return addr.getHostName();
     }
     
     /**
      * Set strict checking of FTP return codes. If strict 
      * checking is on (the default) code must exactly match the expected 
      * code. If strict checking is off, only the first digit must match.
      * 
      * @param strict    true for strict checking, false for loose checking
      */
     void setStrictReturnCodes(boolean strict) {
         this.strictReturnCodes = strict;
     }
     
     /**
      * Listen on all interfaces for active mode transfers (the default).
      * 
      * @param listenOnAll   true if listen on all interfaces, false to listen on the control interface
      */
     void setListenOnAllInterfaces(boolean listenOnAll) {
         this.listenOnAllInterfaces = listenOnAll;
     }
     
     /**
      * Are we listening on all interfaces in active mode, which is the default?
      * 
      * @return true if listening on all interfaces, false if listening just on the control interface
      */
     boolean getListenOnAllInterfaces() {
         return listenOnAllInterfaces;
     }
     

    /**
     *   Set the TCP timeout on the underlying control socket.
     *
     *   If a timeout is set, then any operation which
     *   takes longer than the timeout value will be
     *   killed with a java.io.InterruptedException.
     *
     *   @param millis The length of the timeout, in milliseconds
     */
    void setTimeout(int millis)
        throws IOException {

        if (controlSock == null)
            throw new IllegalStateException(
                        "Failed to set timeout - no control socket");

        controlSock.setSoTimeout(millis);
    }
    
    
    /**
     * Set a listener that handles all FTP messages
     * 
     * @param listener  message listener
     */
    void setMessageListener(FTPMessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * Close the socket
     * 
     * @throws IOException
     */
    public void close() throws IOException {
        controlSock.close();
    }

    /**
     *  Quit this FTP session and clean up.
     */
    public void logout()
        throws IOException {

        IOException ex = null;
        try {
            writer.close();
        }
        catch (IOException e) {
            ex = e;
        }
        try {
            reader.close();
        }
        catch (IOException e) {
            ex = e;
        }
        try {
            controlSock.close();
        }
        catch (IOException e) {
            ex = e;
        }
        if (ex != null)
            throw ex;
     }
              
     /**
      *  Request a data socket be created on the
      *  server, connect to it and return our
      *  connected socket.
      *
      *  @param  active   if true, create in active mode, else
      *                   in passive mode
      *  @return  connected data socket
      */
     FTPDataSocket createDataSocket(FTPConnectMode connectMode)
         throws IOException, FTPException {

        if (connectMode == FTPConnectMode.ACTIVE) {
            return createDataSocketActive();
        }
        else { // PASV
            return createDataSocketPASV();
        }
     }        

     /**
      *  Request a data socket be created on the Client
      *  client on any free port, do not connect it to yet.
      *
      *  @return  not connected data socket
      */
	 FTPDataSocket createDataSocketActive()
        throws IOException, FTPException {

	    try {
	        int count = 0;
            int maxCount = MAX_ACTIVE_RETRY;
            if (lowPort >= 0 && highPort >= 0) {
                int range = highPort-lowPort+1;
                if (range < MAX_ACTIVE_RETRY)
                    maxCount = range;
            }
            while (count < maxCount)
            {
                count++;
                try
                {
                    // use the next port in list (or 0 by default, indicating any port number)
                    FTPDataSocket socket = newActiveDataSocket(nextPort);
                    int port = socket.getLocalPort();
                    InetAddress addr = socket.getLocalAddress();
                    sendPORTCommand(addr, port);                  
                    return socket;
                }
                catch (SocketException ex) 
                {
                    // check ok to retry
                    if (count < maxCount)
                    {
                        log.warn("Detected socket in use - retrying and selecting new port");
                        setNextAvailablePortFromRange();
                    }
                }
            }
            throw new FTPException("Exhausted active port retry count - giving up");
	    }
        finally {
            setNextAvailablePortFromRange();
        }        
     }       
	 
	 
     private void setNextAvailablePortFromRange() 
     {
         // keep using 0 if using OS ports
         if (lowPort < 0 && highPort < 0)
             return;

         // need to set next port to random port in range if it is 0 and we
         // get to here - means the active port ranges have been changed
         if (nextPort == 0) {            
              nextPort = lowPort + new Random().nextInt(highPort-lowPort);
         }
         else
             nextPort++;

         // if exceeded the high port drop to low
         if (nextPort > highPort)
             nextPort = lowPort;

         log.debug("Next active port will be: " + nextPort);
     }
     
     /**
      * Send the PORT command to the server
      * 
      * @param socket           data socket
      * @throws IOException
      * @throws FTPException
      */
     void sendPORTCommand(InetAddress addr, int port) 
         throws IOException, FTPException {
        
         // send the PORT command to the server
         setDataPort(addr, port);
     }
     
    /**
     *  Helper method to convert a byte into an unsigned short value
     *
     *  @param  value   value to convert
     *  @return  the byte value as an unsigned short
     */
    private short toUnsignedShort(byte value) {
        return ( value < 0 )
            ? (short) (value + 256)
            : (short) value;
     }

    /**
     *  Convert a short into a byte array
     *
     *  @param  value   value to convert
     *  @return  a byte array
     */
    protected byte[] toByteArray (int value) {

        byte[] bytes = new byte[2];
        bytes[0] = (byte) (value >> 8);     // bits 1- 8
        bytes[1] = (byte) (value & 0x00FF); // bits 9-16
        return bytes;
    }
    
    /**
     * Set the data channel callback, which notifies of the
     * ip and port number to be connected to, and gives an opportunity
     * to modify these values
     * 
     * @param callback  callback to set
     */
    void setDataChannelCallback(DataChannelCallback callback) {
        this.dataChannelCallback = callback;
    }
    
    
    /**
     * We can force PORT to send a fixed IP address, which can be useful with certain
     * NAT configurations. Must be connected to the remote host to call this method.
     * 
     * @param forcedActiveIP     IP address to force
     */    
    void setActivePortIPAddress(String forcedActiveIP) {
        this.forcedActiveIP = forcedActiveIP;        
    }
    
    /**
     * Set the port number range for active mode
     * 
     * @param lowest        lowest port number in range
     * @param highest       highest port number in range
     */
    public void setActivePortRange(int lowest, int highest) {
        this.lowPort = lowest;
        this.highPort = highest;
        this.nextPort = lowPort;
    }
    
    /**
     * Gets the IP address bytes from an IPV4 address that is
     * a string
     * 
     * @param IPAddress   ip address such as 192.168.10.0
     * @return
     * @throws FTPException
     */
    private byte[] getIPAddressBytes(String IPAddress) 
        throws FTPException {
        
        byte ipbytes[] = new byte[4];
        int len = IPAddress.length();
        int partCount = 0;
        StringBuffer buf = new StringBuffer();
    
        // loop thru and examine each char
        for (int i = 0; i < len && partCount <= 4; i++) {
    
            char ch = IPAddress.charAt(i);
            if (Character.isDigit(ch))
                buf.append(ch);
            else if (ch != '.') {
                throw new FTPException("Incorrectly formatted IP address: " + IPAddress);
            }
    
            // get the part
            if (ch == '.' || i+1 == len) { // at end or at separator
                try {
                    ipbytes[partCount++] = (byte)Integer.parseInt(buf.toString());
                    buf.setLength(0);
                }
                catch (NumberFormatException ex) {
                    throw new FTPException("Incorrectly formatted IP address: " + IPAddress);
                }
            }
        }
        return ipbytes;
    }


    /**
     *  Sets the data port on the server, that is, sends a PORT
     *  command.
     *
     *  @param  host    the local host the server will connect to
     *  @param  portNo  the port number to connect to
     */
    protected void setDataPort(InetAddress host, int portNo)
        throws IOException, FTPException {

        String hostIP = host.getHostAddress();
        
        byte[] hostBytes = host.getAddress();
        byte[] portBytes = toByteArray(portNo);
        
        if (forcedActiveIP != null) {
            log.info("Forcing use of fixed IP for PORT command");
            hostBytes = getIPAddressBytes(forcedActiveIP);
            hostIP = forcedActiveIP;
        }
        
        if (dataChannelCallback != null) {
            IPEndpoint origEndpoint = new IPEndpoint(hostIP, portNo);
            IPEndpoint newEndpoint = dataChannelCallback.onPORTCommand(origEndpoint);
            hostBytes = getIPAddressBytes(newEndpoint.getIPAddress());
            portBytes = toByteArray(newEndpoint.getPort());
            log.info("Changed PORT endpoint from " + origEndpoint.toString() + " => " + newEndpoint.toString());
        }

        // assemble the PORT command
        String cmd = new StringBuffer ("PORT ")
            .append (toUnsignedShort (hostBytes[0])) .append (",")
            .append (toUnsignedShort (hostBytes[1])) .append (",")
            .append (toUnsignedShort (hostBytes[2])) .append (",")
            .append (toUnsignedShort (hostBytes[3])) .append (",")
            .append (toUnsignedShort (portBytes[0])) .append (",")
            .append (toUnsignedShort (portBytes[1])) .toString ();

        // send command and check reply
        // CoreFTP returns 250 incorrectly
        FTPReply reply = sendCommand(cmd);
        String[] validCodes = {"200", "250"};
        validateReply(reply, validCodes);
     }

     /**
      *  Request a data socket be created on the
      *  server, connect to it and return our
      *  connected socket.
      *
      *  @return  connected data socket
      */
     protected FTPDataSocket createDataSocketPASV()
         throws IOException, FTPException {

         // PASSIVE command - tells the server to listen for
         // a connection attempt rather than initiating it
         FTPReply replyObj = sendCommand("PASV");
         validateReply(replyObj, "227");
         String reply = replyObj.getReplyText();

         int[] parts = getPASVParts(reply);

         // assemble the IP address
         // we try connecting, so we don't bother checking digits etc
         String ipAddress = parts[0] + "."+ parts[1]+ "." +
             parts[2] + "." + parts[3];

         // assemble the port number
         int port = (parts[4] << 8) + parts[5];         
         
         String hostIP = ipAddress;
         if (autoPassiveIPSubstitution) {
             if (usingProxy()) {// we can't use the remoteAddr as that will be set to the proxy
                 hostIP = controlSock.getRemoteHost();
                 log.debug("Using proxy");
             }
             else 
                 hostIP = remoteAddr.getHostAddress();
             StringBuffer msg = new StringBuffer("Substituting server supplied IP (");
             msg.append(ipAddress).append(") with remote host IP (").append(hostIP).append(")");
             log.info(msg.toString());
         }
         
         if (dataChannelCallback != null) {
             IPEndpoint origEndpoint = new IPEndpoint(hostIP, port);
             IPEndpoint newEndpoint = dataChannelCallback.onPASVResponse(origEndpoint);
             hostIP = newEndpoint.getIPAddress();
             port = newEndpoint.getPort();
             log.info("Changed PASV endpoint from " + origEndpoint.toString() + " => " + newEndpoint.toString());
         }

         // create the socket
         return newPassiveDataSocket(hostIP, port);
     }
          
     protected boolean usingProxy() {
         return false;
     }
     
     /**
      * Get the parts that make up the PASV reply
      * 
      * @param reply  reply string
      * @return
      * @throws FTPException
      */
     int[] getPASVParts(String reply) throws FTPException {
         
         // The reply to PASV is in the form:
         // 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2).
         // where h1..h4 are the IP address to connect and
         // p1,p2 the port number
         // Example:
         // 227 Entering Passive Mode (128,3,122,1,15,87).
         // NOTE: PASV command in IBM/Mainframe returns the string
         // 227 Entering Passive Mode 128,3,122,1,15,87 (missing 
         // brackets)

         // extract the IP data string from between the brackets
         int startIP = reply.indexOf('(');
         int endIP = reply.indexOf(')');
         
         // if didn't find start bracket, figure out where it should have been
         if (startIP < 0) {
             startIP = 0;
             while (startIP < reply.length() && !Character.isDigit(reply.charAt(startIP)))
                 startIP++;
             startIP--; // go back so this is where the '(' should be
         }
         
         // if didn't find end bracket, set to end of reply
         if (endIP < 0) {
             endIP = reply.length()-1;
             while (endIP > 0 && !Character.isDigit(reply.charAt(endIP)))
                 endIP--;
             endIP++; // go forward so this is where the ')' should be
             if (endIP >= reply.length())
                 reply += ")";
         }
                  
         String ipData = reply.substring(startIP+1,endIP).trim();
         int parts[] = new int[6];

         int len = ipData.length();
         int partCount = 0;
         StringBuffer buf = new StringBuffer();

         // loop thru and examine each char
         for (int i = 0; i < len && partCount <= 6; i++) {

             char ch = ipData.charAt(i);
             if (Character.isDigit(ch))
                 buf.append(ch);
             else if (ch != ',' && ch != ' ') {
                 throw new FTPException("Malformed PASV reply: " + reply);
             }

             // get the part
             if (ch == ',' || i+1 == len) { // at end or at separator
                 try {
                     parts[partCount++] = Integer.parseInt(buf.toString());
                     buf.setLength(0);
                 }
                 catch (NumberFormatException ex) {
                     throw new FTPException("Malformed PASV reply: " + reply);
                 }
             }
         }
         return parts;         
     }

	/**
	 * Constructs a new <code>FTPDataSocket</code> object (client mode) and connect
	 * to the given remote host and port number.
	 * 
	 * @param remoteHost Remote host to connect to.
	 * @param port Remote port to connect to.
	 * @return A new <code>FTPDataSocket</code> object (client mode) which is
	 * connected to the given server.
	 * @throws IOException Thrown if no TCP/IP connection could be made. 
	 */
	protected FTPDataSocket newPassiveDataSocket(String remoteHost, int port) 
		throws IOException {
 
	    StreamSocket sock = PlainSocket.createPlainSocket(remoteHost, port, controlSock.getSoTimeout());
        return new FTPPassiveDataSocket(sock);
	}

    /**
     * Constructs a new <code>FTPDataSocket</code> object (server mode) which will
     * listen on the given port number.
     * 
     * @param port Remote port to listen on.
     * @return A new <code>FTPDataSocket</code> object (server mode) which is
     *         configured to listen on the given port.
     * @throws IOException Thrown if an error occurred when creating the socket. 
     */
     protected FTPDataSocket newActiveDataSocket(int port) 
    	throws IOException {
         
     	 // ensure server sock gets the timeout
    	 ServerSocket sock = listenOnAllInterfaces ? 
    	         new ServerSocket(port) : new ServerSocket(port, 0, controlSock.getLocalAddress());
    	 log.debug("ListenOnAllInterfaces=" + listenOnAllInterfaces);
    	 sock.setSoTimeout(controlSock.getSoTimeout()); 
    	 FTPActiveDataSocket activeSock = new FTPActiveDataSocket(sock);
    	 activeSock.setLocalAddress(controlSock.getLocalAddress());
    	 return activeSock;
     }
     
     /**
      *  Send a command to the FTP server and
      *  return the server's reply as a structured
      *  reply object
      * 
      *  @param command   command to send
      *
      *  @return  reply to the supplied command
     * @throws IOException, FTPException 
      */
     public FTPReply sendCommand(String command)
         throws IOException, IOException, FTPException {
         
         writeCommand(command);
         
         // and read the result
         return readReply();
     }
     
     /**
      *  Send a command to the FTP server. Don't
      *  read the reply
      *
      *  @param command   command to send
      */     
     void writeCommand(String command)
         throws IOException {
         
         log(DEBUG_ARROW + command, true);
         
         // send it
         try {
             writer.write(command + EOL);
             writer.flush();  
         }
         catch (IOException ex) {
             throw new ControlChannelIOException(ex.getMessage());
         }
     }
     
     /**
      * Read a line, which means until a \n is reached. Any \r's
      * are ignored
      * 
      * @throws IOException 
      */
     private String readLine() throws IOException {
         int current = 0;
         StringBuffer str = new StringBuffer();
         StringBuffer err = new StringBuffer();
         while (true)
         {
             try {
                 current = reader.read(); 
             }
             catch (IOException ex) {
                 log.error("Read failed ('" + err.toString() + "' read so far)");
                 throw new ControlChannelIOException(ex.getMessage());
             }
             if (current < 0) {
                 String msg = "Control channel unexpectedly closed ('" + err.toString() + "' read so far)";
                 log.error(msg);
                 throw new ControlChannelIOException(msg);
             }
             if (current == LINE_FEED)
                 break;

             if (current != CARRIAGE_RETURN) {                 
                 str.append((char)current);
                 err.append((char)current);
             }
             else {
                 err.append("<cr>");
             }
                 
         }
         return str.toString();
     }
     
     /**
      *  Read the FTP server's reply to a previously
      *  issued command. RFC 959 states that a reply
      *  consists of the 3 digit code followed by text.
      *  The 3 digit code is followed by a hyphen if it
      *  is a multiline response, and the last line starts
      *  with the same 3 digit code.
      *
      *  @return  structured reply object
      */
     FTPReply readReply()
         throws IOException, FTPException {
         
         String line = readLine();
         while (line != null && line.trim().length() == 0)
             line = readLine();
         
         line = line.trim();
         
         log(line, false);     
         
         if (line.length() < 3) {
             String msg = "Short reply received (" + line + ")";
             log.error(msg);
             throw new MalformedReplyException(msg);
         }
         
         String replyCode = line.substring(0, 3);
         StringBuffer reply = new StringBuffer("");
         if (line.length() > 3)
             reply.append(line.substring(4));
                  
         Vector dataLines = null;

         // check for multi-line response and build up
         // the reply
         if (line.length() > 3 && line.charAt(3) == '-') {
             dataLines = new Vector();
             
             // if first line has data, add to data list
             if (line.length() > 4) {
                 line = line.substring(4).trim();
                 if (line.length() > 0)
                     dataLines.addElement(line);
             }
             
             boolean complete = false;
             while (!complete) {
                 
                 line = readLine();
                 if (line == null){
                     String msg = "Control channel unexpectedly closed";
                     log.error(msg);
                     throw new ControlChannelIOException(msg);
                 }
                                  
                 if (line.length() == 0)
                     continue;
                 
                 log(line, false);
                 
                 if (line.length() > 3 &&
                         line.substring(0, 3).equals(replyCode) &&
                         line.charAt(3) == ' ') {
                     line = line.substring(3).trim(); // get rid of the code
                     if (line.length() > 0) {
                         if (reply.length() > 0) 
                             reply.append(" ");
                         reply.append(line);
                         dataLines.addElement(line);
                     }
                     complete = true;
                 }
                 else { // not the last line
                     reply.append(" ").append(line);
                     dataLines.addElement(line);
                 }
             } // end while
         } // end if
         
         if (dataLines != null) {
             String[] data = new String[dataLines.size()];
             dataLines.copyInto(data);
             return new FTPReply(replyCode, reply.toString(), data);
         }
         else {
             return new FTPReply(replyCode, reply.toString());
         }
     }
    
    
     /**
      *  Validate the response the host has supplied against the
      *  expected reply. If we get an unexpected reply we throw an
      *  exception, setting the message to that returned by the
      *  FTP server
      *
      *  @param   reply              the entire reply string we received
      *  @param   expectedReplyCode  the reply we expected to receive
      *
      */
     FTPReply validateReply(String reply, String expectedReplyCode)
         throws FTPException {
    
         FTPReply replyObj = new FTPReply(reply);
         
         if (validateReplyCode(replyObj, expectedReplyCode))
             return replyObj;
             
         // if unexpected reply, throw an exception
         throw new FTPException(replyObj);         
     }
    
     
     /**
      *  Validate the response the host has supplied against the
      *  expected reply. If we get an unexpected reply we throw an
      *  exception, setting the message to that returned by the
      *  FTP server
      *
      *  @param   reply               the entire reply string we received
      *  @param   expectedReplyCodes  array of expected replies
      *  @return  an object encapsulating the server's reply
      *
      */
     public FTPReply validateReply(String reply, String[] expectedReplyCodes)
         throws IOException, FTPException {
         
         FTPReply replyObj = new FTPReply(reply);        
         return validateReply(replyObj, expectedReplyCodes);
     }
     
     
     /**
      *  Validate the response the host has supplied against the
      *  expected reply. If we get an unexpected reply we throw an
      *  exception, setting the message to that returned by the
      *  FTP server
      *
      *  @param   reply               reply object
      *  @param   expectedReplyCodes  array of expected replies
      *  @return  reply object
      *
      */
     public FTPReply validateReply(FTPReply reply, String[] expectedReplyCodes)
         throws FTPException {
                  
         for (int i = 0; i < expectedReplyCodes.length; i++)
             if (validateReplyCode(reply, expectedReplyCodes[i]))
                 return reply;
             
             // got this far, not recognised
         StringBuffer buf = new StringBuffer("[");
         for (int i = 0; i < expectedReplyCodes.length; i++) {
             buf.append(expectedReplyCodes[i]);
             if (i+1 < expectedReplyCodes.length)
                 buf.append(",");
         }
         buf.append("]");
         log.info("Expected reply codes = " + buf.toString());
         throw new FTPException(reply);  
     }
     
     /**
      *  Validate the response the host has supplied against the
      *  expected reply. If we get an unexpected reply we throw an
      *  exception, setting the message to that returned by the
      *  FTP server
      *
      *  @param   reply               reply object
      *  @param   expectedReplyCode   expected reply
      *  @return  reply object
      *
      */
     public FTPReply validateReply(FTPReply reply, String expectedReplyCode)
         throws FTPException {
                  
         if (validateReplyCode(reply, expectedReplyCode))
                 return reply;
             
         // got this far, not recognised
         log.info("Expected reply code = [" + expectedReplyCode + "]");
         throw new FTPException(reply);  
     }
     
     /**
      * Validate reply object
      * 
      * @param reply                reference to reply object
      * @param expectedReplyCode    expect reply code
      * @return true if valid, false if invalid
      */
     private boolean validateReplyCode(FTPReply reply, String expectedReplyCode) 
         throws FTPConnectionClosedException {
         
         String replyCode = reply.getReplyCode();
         
         if ("421".equals(replyCode)) {
             throw new FTPConnectionClosedException(reply.getReplyText());
         }
         if (strictReturnCodes) {
             if (replyCode.equals(expectedReplyCode)) 
                 return true;
             else
                 return false;
         }
         else { // non-strict - match first char
             if (replyCode.charAt(0) == expectedReplyCode.charAt(0))
                 return true;
             else
                 return false;
         }         
     }
        
    
     /**
      *  Log a message, checking for passwords
      * 
      *  @param msg	message to log
      *  @param reply  true if a response, false otherwise
      */
     void log(String msg, boolean command) {
  	 	 if (msg.startsWith(PASSWORD_MESSAGE))
 	     	 msg = PASSWORD_MESSAGE + " ********";
  	 	 else if (msg.startsWith(ACCT_MESSAGE))
  	 	     msg = ACCT_MESSAGE + " ********";
         log.debug(msg);
         if (messageListener != null)
             if (command)
                 messageListener.logCommand(msg);
             else
                 messageListener.logReply(msg);
         
     }        
}


