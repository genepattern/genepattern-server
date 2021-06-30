/**
 *
 *  Java FTP client library.
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
 *        $Log: FTPTransferType.java,v $
 *        Revision 1.8  2007-08-07 04:44:38  bruceb
 *        added toString()
 *
 *        Revision 1.7  2006/10/11 08:56:18  hans
 *        made cvsId and ASCII and BINARY final
 *
 *        Revision 1.6  2005/06/03 11:26:25  bruceb
 *        comment change
 *
 *        Revision 1.5  2004/07/23 08:28:54  bruceb
 *        made public cvsId
 *
 *        Revision 1.4  2002/11/19 22:01:25  bruceb
 *        changes for 1.2
 *
 *        Revision 1.3  2001/10/09 20:54:08  bruceb
 *        No change
 *
 *        Revision 1.1  2001/10/05 14:42:04  bruceb
 *        moved from old project
 *
 *
 */

package com.enterprisedt.net.ftp;

/**
 *  Enumerates the transfer types possible. We
 *  support only the two common types, ASCII and
 *  Image (often called binary).
 *
 *  @author             Bruce Blackshaw
 *  @version        $Revision: 1.8 $
 *
 */
 public class FTPTransferType {

     /**
      *  Revision control id
      */
     public static final String cvsId = "@(#)$Id: FTPTransferType.java,v 1.8 2007-08-07 04:44:38 bruceb Exp $";
     
     /**
      * Readable description
      */
     private String desc;

     /**
      *   Represents ASCII transfer type
      */
     public static final FTPTransferType ASCII = new FTPTransferType("ASCII");

     /**
      *   Represents Image (or binary) transfer type
      */
     public static final FTPTransferType BINARY = new FTPTransferType("Binary");

     /**
      *   The char sent to the server to set ASCII
      */
     static String ASCII_CHAR = "A";

     /**
      *   The char sent to the server to set BINARY
      */
     static String BINARY_CHAR = "I";

     /**
      *  Private so no-one else can instantiate this class
      */
     private FTPTransferType(String desc) {
         this.desc = desc;
     }
     
     public String toString() {
         return desc;
     }
 }
