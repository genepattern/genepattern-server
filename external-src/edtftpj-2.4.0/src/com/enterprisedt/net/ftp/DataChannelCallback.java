/**
 *
 *  Copyright (C) 2000-2009  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: DataChannelCallback.java,v $
 *        Revision 1.1  2009-04-14 01:47:26  bruceb
 *        PASV/PORT callbacks
 *
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Callback that users can implement to intercept the
 *  reply from PASV and substitute a different IP address
 *  or port number to connect to. Similarly the PORT command
 *  being sent to the server can also be intercepted and modified.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.1 $
 */
public interface DataChannelCallback {
    
    /**
     * If this callback is implemented, it should return the endpoint that the user
     * wishes to connect to. The supplied endpoint is what the server is returning.
     * 
     * @param endpoint  the endpoint specified by the server's response to the PASV command
     * @return  the actual endpoint that should be used
     */
    public IPEndpoint onPASVResponse(IPEndpoint endpoint);
    
    /**
     * If this callback is implemented, it should return the endpoint that the server
     * should connect to in active (PORT) mode. The supplied endpoint is what the client 
     * is going to use..
     * 
     * @param endpoint  the endpoint the client will begin listening on 
     * @return  the actual endpoint that should be used
     */
    public IPEndpoint onPORTCommand(IPEndpoint endpoint);

}
