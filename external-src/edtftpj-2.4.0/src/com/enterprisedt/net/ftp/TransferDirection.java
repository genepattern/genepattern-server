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
 *    $Log: TransferDirection.java,v $
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Encapsulates the possible transfer directions
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.1 $
 */
public class TransferDirection {
    
    private String direction;
    
    /**
     *   Represents upload
     */
    public static final TransferDirection UPLOAD = new TransferDirection("Upload");

    /**
     *   Represents download
     */
    public static final TransferDirection DOWNLOAD = new TransferDirection("Download");

    /**
     *  Private so no-one else can instantiate this class
     */
    private TransferDirection(String direction) {
        this.direction = direction;
    }
    
    public String toString() {
        return direction;
    }
    
  

}
