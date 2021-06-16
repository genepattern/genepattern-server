/**
 * 
 *  Copyright (C) 2007 Enterprise Distributed Technologies Ltd
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
 *    $Log: FTPTransferCancelledException.java,v $
 *    Revision 1.4  2007-12-18 07:54:12  bruceb
 *    apply LGPL
 *
 *
 */

package com.enterprisedt.net.ftp;

/**
 *  Thrown when an FTP transfer has been cancelled
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.4 $
 *
 */
 public class FTPTransferCancelledException extends FTPException {

    /**
     *  Revision control id
     */
    public static final String cvsId = "@(#)$Id: FTPTransferCancelledException.java,v 1.4 2007-12-18 07:54:12 bruceb Exp $";

    /**
     * Serial uid
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Message
     */
    private static final String MESSAGE = "Transfer was cancelled";

    /**
     *   Constructor. Delegates to super.
     */
    public FTPTransferCancelledException() {
        super(MESSAGE);
    }
}
