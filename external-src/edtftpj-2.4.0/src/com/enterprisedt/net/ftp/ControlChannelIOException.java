/**
 *
 *  Copyright (C) 2000-2009  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: ControlChannelIOException.java,v $
 *        Revision 1.1  2009-01-15 03:39:23  bruceb
 *        introduce ControlChannelException
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.IOException;


/**
 *  IOException that appears on the control channel
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.1 $
 */
public class ControlChannelIOException extends IOException {
    
    /**
     * Constructs an {@code ControlChannelIOException} with {@code null}
     * as its error detail message.
     */
    public ControlChannelIOException() {
        super();
    }

    /**
     * Constructs an {@code ControlChannelIOException} with the specified detail message.
     *
     * @param message
     *        The detail message (which is saved for later retrieval
     *        by the {@link #getMessage()} method)
     */
    public ControlChannelIOException(String message) {
        super(message);
    }
}
