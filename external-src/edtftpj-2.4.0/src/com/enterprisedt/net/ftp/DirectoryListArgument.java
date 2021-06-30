/**
 * 
 *  Copyright (C) 2010 Enterprise Distributed Technologies Ltd
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
 *    $Log: DirectoryListArgument.java,v $
 *    Revision 1.2  2010-05-28 07:04:55  bruceb
 *    allow aborting the listing
 *
 *    Revision 1.1  2010-04-26 15:55:46  bruceb
 *    add new dirDetails method with callback
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Argument object used to provide directory listing information
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class DirectoryListArgument {

    private FTPFile entry;
    private boolean listingAborted = false;
    
    public DirectoryListArgument(FTPFile entry) {
        this.entry = entry;
    }
    
    public FTPFile getEntry() {
        return entry;
    }
    
    public void abortListing() {
        listingAborted = true;
    }
    
    public boolean isListingAborted() {
        return listingAborted;
    }
}


