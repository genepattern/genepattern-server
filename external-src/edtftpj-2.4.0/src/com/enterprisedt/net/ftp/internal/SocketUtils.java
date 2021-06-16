/**
 *
 *  Copyright (C) 2000-2006  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: SocketUtils.java,v $
 *        Revision 1.2  2009-01-15 03:40:29  bruceb
 *        remember what's supported
 *
 *        Revision 1.1  2008-05-22 04:20:55  bruceb
 *        moved stuff to internal etc
 *
 *        Revision 1.5  2007-12-18 07:53:07  bruceb
 *        extra debug
 *
 *        Revision 1.4  2007-05-29 06:07:21  bruceb
 *        use casts to remove compile warnings
 *
 *        Revision 1.3  2007-05-29 04:16:44  bruceb
 *        added isConnected()
 *
 *        Revision 1.2  2006/11/02 17:37:31  bruceb
 *        remove unneeded sendUrgentData()
 *
 *        Revision 1.1  2006/10/27 15:38:01  bruceb
 *        used for connect with timeout
 *
 *
 */
package com.enterprisedt.net.ftp.internal;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;

import com.enterprisedt.util.debug.Logger;

/**
 * Utility class that allows 1.4 socket methods to be called while
 * still being able to be compiled in 1.1.x
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.2 $
 */
public class SocketUtils {
    
    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("SocketUtils");
    
    private static boolean timeoutSupported = true;
    
    private static boolean isConnectedSupported = true;

    /**
     * Create a connected socket, using a timeout if it is available. 
     * Availability is tested by trying to create instances of the 
     * required classes and methods (JRE 1.4+)
     * 
     * @param host     remote host to connect to
     * @param port     port on remote host
     * @param timeout  timeout in milliseconds on 
     * @exception IOException
     */
    public static Socket createSocket(InetAddress host, int port, int timeout)
            throws IOException {

        // don't bother going thru the below if a timeout isn't asked for ...
        if (timeout == 0 || !timeoutSupported) {
            return new Socket(host, port);
        } 
        else {
            // attempt to set up 1.4 and later's Socket.connect method which
            // provides a timeout
            try {
                // get the correct connect method
                Class socketAddress = Class.forName("java.net.SocketAddress");
                Method connectMethod = Socket.class.getMethod("connect", new Class[] {
                        socketAddress, int.class });
                
                // create an unconnected socket instance
                Socket sock = (Socket) Socket.class.newInstance();

                // need an InetSocketAddress instance for connect()
                Class inetSocketAddress = Class.forName("java.net.InetSocketAddress");
                Constructor inetSocketAddressCtr = 
                    inetSocketAddress.getConstructor(
                            new Class[] { InetAddress.class, int.class });
                Object address = inetSocketAddressCtr.newInstance(
                        new Object[] { host, new Integer(port) });

                // now invoke the connect method with the timeout
                log.debug("Invoking connect with timeout=" + timeout);
                connectMethod.invoke(sock, new Object[]{address, new Integer(timeout)});
                log.debug("Connected successfully");
                return sock;
            } 
            catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof IOException)
                    throw (IOException)target;
                log.debug("Could not use timeout connecting to host (" + ex.toString() + ")");
                timeoutSupported = false;
                return new Socket(host, port);
            } 
            catch (Exception ex) {
                log.debug("Could not use timeout connecting to host (" + ex.toString() + ")");
                timeoutSupported = false;
                return new Socket(host, port);
            }
        }
    }
    
    
    /**
     * Test if a socket is connected by using the isConnected method, only
     * available from JRE 1.4+. So invoke using reflection. If can't check
     * it assumes the socket is connected.
     * 
     * @param sock     socket to test
     * @exception IOException
     */
    public static boolean isConnected(Socket sock) throws IOException {
        if (!isConnectedSupported)
            return true;
        try {
            // get the isConnected method
            Method connectedMethod = Socket.class.getMethod("isConnected", (Class[])null);
            
            Boolean result = (Boolean)connectedMethod.invoke(sock, (Object[])null);
            return result.booleanValue();
        } 
        catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof IOException)
                throw (IOException)target;
            isConnectedSupported = false;
            log.debug("Could not use Socket.isConnected (" + ex.toString() + ")");
            return true;
        } 
        catch (Exception ex) {
            log.debug("Could not use Socket.isConnected (" + ex.toString() + ")");
            isConnectedSupported = false;
            return true;
        }
    }
    
    
}
