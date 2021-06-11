/**
 * 
 *  Copyright (C) 2000-2004 Enterprise Distributed Technologies Ltd
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
 *  Bug fixes, suggestions and comments should be sent to bruce@enterprisedt.com
 *
 *  Change Log:
 *
 *    $Log: BaseIOException.java,v $
 *    Revision 1.1  2006/03/16 21:49:54  hans
 *    Added getInnerThrowable which is used in place of getCause (which is not supported pre JDK1.4)
 *
 */
package com.enterprisedt;

import java.io.IOException;

/**
 * Extension of java.io.IOException which adds "cause" functionality.
 * This replicates the behaviour that is already present in JDK 1.4 
 * or better.
 * 
 * @author Hans Andersen
 */
public class BaseIOException extends IOException {
	
	private Throwable _cause;

	/**
	 * Creates a BaseIOException with no message or cause.
	 */
	public BaseIOException() {
		super();
	}

	/**
	 * Creates a BaseIOException with the given message.
	 * @param message Exception message
	 */
	public BaseIOException(String message) {
		super(message);
	}

	/**
	 * Creates a BaseIOException with the given cause.
	 * @param cause Throwable which caused this exception to be thrown.
	 */
    public BaseIOException(Throwable cause) {
        super(cause.getMessage());
        _cause = cause;
    }
    
    /**
     * Creates a BaseIOException with the given cause.
	 * @param message Exception message
	 * @param cause Throwable which caused this exception to be thrown.
     */
    public BaseIOException(String message, Throwable cause) {
        super(message);
        _cause = cause;
    }
    
    /**
     * Returns the cause of this exception.
     * Included for JDK1.4+ compatibility.
     */
    public Throwable getCause() {
    	return _cause;
    }
    
    /**
     * Returns the cause of this exception (same as getCause).
     * This has a different name from JDK1.4's Exception.getCause
     * in order to prevent cross-over behaviour.
     */
	public Throwable getInnerThrowable() {
		return _cause;
	}
	
	/**
	 * Initializes the cause.
	 * 
	 * @param cause Throwable which caused this exception to be thrown.
	 */
	public Throwable initCause(Throwable cause) {
		_cause = cause;
		return _cause;
	}
}
