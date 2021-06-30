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
 *    $Log: DateParseException.java,v $
 *    Revision 1.1  2008-07-15 05:41:33  bruceb
 *    refactor parsing code
 *
 *    Revision 1.8  2006/10/11 08:53:29  hans
 *    made cvsId final
 *
 *    Revision 1.7  2005/11/10 19:44:10  bruceb
 *    added serial uid
 *
 *    Revision 1.6  2005/06/03 11:26:25  bruceb
 *    comment change
 *
 *    Revision 1.5  2004/07/23 08:27:43  bruceb
 *    new constructor
 *
 *    Revision 1.4  2002/11/19 22:01:25  bruceb
 *    changes for 1.2
 *
 *    Revision 1.3  2001/10/09 20:54:08  bruceb
 *    No change
 *
 *    Revision 1.1  2001/10/05 14:42:04  bruceb
 *    moved from old project
 *
 */

package com.enterprisedt.net.ftp;

import java.text.ParseException;

/**
 *  Throw this in parsers for date parsing exceptions
 *
 *  @author     Bruce Blackshaw
 *  @version    $Revision: 1.1 $
 *
 */
 public class DateParseException extends ParseException {

    /**
     *  Revision control id
     */
    public static final String cvsId = "@(#)$Id: DateParseException.java,v 1.1 2008-07-15 05:41:33 bruceb Exp $";

    /**
     * Serial uid
     */
    private static final long serialVersionUID = 1L;

    /**
     *   Constructor. Delegates to super.
     *
     *   @param   msg   Message that the user will be
     *                  able to retrieve
     */
    public DateParseException(String msg) {
        super(msg, 0);
    }

}
