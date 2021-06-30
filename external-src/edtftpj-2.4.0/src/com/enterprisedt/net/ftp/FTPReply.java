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
 *        $Log: FTPReply.java,v $
 *        Revision 1.7  2008-08-26 04:35:40  bruceb
 *        MalformedReplyException added
 *
 *        Revision 1.6  2007/05/03 04:20:46  bruceb
 *        added rawReply
 *
 *        Revision 1.5  2006/10/11 08:58:29  hans
 *        made cvsId final
 *
 *        Revision 1.4  2005/06/03 11:26:25  bruceb
 *        comment change
 *
 *        Revision 1.3  2004/08/31 10:46:11  bruceb
 *        data lines added
 *
 *        Revision 1.2  2004/07/23 08:28:19  bruceb
 *        new constructor
 *
 *        Revision 1.1  2002/11/19 22:01:25  bruceb
 *        changes for 1.2
 *
 *
 */

package com.enterprisedt.net.ftp;

/**
 *  Encapsulates the FTP server reply
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.7 $
 */
public class FTPReply {

    /**
     *  Revision control id
     */
    public static final String cvsId = "@(#)$Id: FTPReply.java,v 1.7 2008-08-26 04:35:40 bruceb Exp $";

    /**
     *  Reply code
     */
    private String replyCode;

    /**
     *  Reply text
     */
    private String replyText;
    
    /**
     * Raw reply
     */
    private String rawReply;

    /**
     * Lines of data returned, e.g. FEAT
     */
    private String[] data;

    /**
     *  Constructor. Only to be constructed
     *  by this package, hence package access
     *
     *  @param  replyCode  the server's reply code
     *  @param  replyText  the server's reply text
     */
    FTPReply(String replyCode, String replyText) throws MalformedReplyException {
        this.replyCode = replyCode;
        this.replyText = replyText;
        rawReply = replyCode + " " + replyText;
        validateCode(replyCode);
    }
    
    
    /**
     *  Constructor. Only to be constructed
     *  by this package, hence package access
     *
     *  @param  replyCode  the server's reply code
     *  @param  replyText  the server's full reply text
     *  @param  data       data lines contained in reply text
     */
    FTPReply(String replyCode, String replyText, String[] data) throws MalformedReplyException {
        this.replyCode = replyCode;
        this.replyText = replyText;
        this.data = data;
        validateCode(replyCode);
    }
    
    
    /**
     *  Constructor. Only to be constructed
     *  by this package, hence package access
     *
     *  @param  rawReply  the server's raw reply
     */
    FTPReply(String reply) throws MalformedReplyException {        
        // all reply codes are 3 chars long
        this.rawReply = reply.trim();
        replyCode = rawReply.substring(0, 3);
        if (rawReply.length() > 3)
            replyText = rawReply.substring(4);
        else
            replyText = "";
        validateCode(replyCode);
    }
    
    /**
     *  Getter for raw reply
     *
     *  @return server's raw reply
     */
    public String getRawReply() {
        return rawReply;
    }

    /**
     *  Getter for reply code
     *
     *  @return server's reply code
     */
    public String getReplyCode() {
        return replyCode;
    }

    /**
     *  Getter for reply text
     * 
     *  @return server's reply text
     */
    public String getReplyText() {
        return replyText;
    }
    
    /**
     * Getter for reply data lines
     * 
     * @return array of data lines returned (if any). Null
     *          if no data lines
     */
    public String[] getReplyData() {
        return data;
    }
    
    private void validateCode(String code) throws MalformedReplyException {
        try {
            Integer.parseInt(code);
        }
        catch (NumberFormatException ex) {
            throw new MalformedReplyException("Invalid reply code '" + code + "'");
        }
    }

}
