/**
 *
 *  edtFTPj
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
 *  Bug fixes, suggestions and comments should be should posted on 
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *    $Log: NetwareFileParser.java,v $
 *    Revision 1.2  2010-03-31 00:54:48  bruceb
 *    tweak re ./
 *
 *    Revision 1.1  2010-03-25 04:02:33  bruceb
 *    new class for netware
 *
 *    Revision 1.23  2009-01-15 03:36:26  bruceb
 *    added isNumeric
 *
 *    Revision 1.22  2009-01-15 00:16:58  hans
 *    Removed unused local variable.
 *
 *    Revision 1.21  2008-07-29 02:58:44  bruceb
 *    Connect:Enterprise tweaks
 *
 *    Revision 1.20  2008-07-15 05:41:33  bruceb
 *    refactor parsing code
 *
 *    Revision 1.19  2007-12-18 07:52:53  bruceb
 *    trimStart() changes
 *
 *    Revision 1.18  2007-10-12 05:20:44  bruceb
 *    permit ignoring date parser errors
 *
 *    Revision 1.17  2007-06-15 08:15:30  bruceb
 *    Connect:Enterprise fix
 *
 *    Revision 1.16  2007/03/28 06:04:15  bruceb
 *    support reverse MMM/dd formats
 *
 *    Revision 1.15  2007/03/19 22:10:57  bruceb
 *    when testing for future, set future date 2 days ahead to account for time zones
 *
 *    Revision 1.14  2006/10/11 08:57:40  hans
 *    Removed usage of deprecated FTPFile constructor and made cvsId final
 *
 *    Revision 1.13  2006/05/23 04:10:17  bruceb
 *    support Unix listing starting with 'p'
 *
 *    Revision 1.12  2005/06/03 11:26:25  bruceb
 *    comment change
 *
 *    Revision 1.11  2005/04/01 13:57:15  bruceb
 *    minor tweak re groups
 *
 *    Revision 1.10  2004/10/19 16:15:49  bruceb
 *    minor restructuring
 *
 *    Revision 1.9  2004/10/18 15:58:15  bruceb
 *    setLocale
 *
 *    Revision 1.8  2004/09/20 21:36:13  bruceb
 *    tweak to skip invalid lines
 *
 *    Revision 1.7  2004/09/17 14:56:54  bruceb
 *    parse fixes including wrong year
 *
 *    Revision 1.6  2004/07/23 08:32:36  bruceb
 *    made cvsId public
 *
 *    Revision 1.5  2004/06/11 10:19:59  bruceb
 *    fixed bug re filename same as user
 *
 *    Revision 1.4  2004/05/20 19:47:00  bruceb
 *    blanks in names fix
 *
 *    Revision 1.3  2004/05/05 20:27:41  bruceb
 *    US locale for date formats
 *
 *    Revision 1.2  2004/05/01 11:44:21  bruceb
 *    modified for server returning "total 3943" as first line
 *
 *    Revision 1.1  2004/04/17 23:42:07  bruceb
 *    file parsing part II
 *
 *    Revision 1.1  2004/04/17 18:37:23  bruceb
 *    new parse functionality
 *
 */
package com.enterprisedt.net.ftp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.enterprisedt.util.debug.Logger;

/**
 *  Represents a remote Netware file parser
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class NetwareFileParser extends FTPFileParser {

    /**
     *  Revision control id
     */
	final public static String cvsId = "@(#)$Id: NetwareFileParser.java,v 1.2 2010-03-31 00:54:48 bruceb Exp $";
	
	/**
     * Logging object
     */
    private static Logger log = Logger.getLogger("NetwareFileParser");
        
    /**
     * These chars indicates ordinary files
     */
    private final static char FILE_CHAR = '-';
    
    /**
     * Indicates directory
     */
    private final static char DIRECTORY_CHAR = 'd';
    
    /**
     * Prefix e.g. ./WebshotsData
     */
    private final static String CURRENT_DIR_PREFIX = "./";
    
    /**
     * Date formatter
     */
    private SimpleDateFormat dateFormatter;
        
    /**
     * Minimum number of expected fields
     */
    private final static int MIN_FIELD_COUNT = 8;
    
    /**
     * Constructor
     */
    public NetwareFileParser() {
        setLocale(Locale.getDefault());
    }
    
    /**
     * Set the locale for date parsing of listings
     * 
     * @param locale    locale to set
     */
    public void setLocale(Locale locale) {
        dateFormatter = new SimpleDateFormat("MMM-dd-yyyy-HH:mm", locale);
    }  
    
    public String toString() {
        return "NETWARE";
    }
    
    
    /**
     * Valid format for this parser
     * 
     * @param listing
     * @return true if valid
     */
    public boolean isValidFormat(String[] listing) {
        int count = Math.min(listing.length, 10);      

        for (int i = 0; i < count; i++) {            
            if (listing[i].trim().length() == 0)
                continue;
            if (isNetware(listing[i]))
                return true;            
        }
        log.debug("Not in Netware format");
        return false;
    }
    
    /**
     * Is this a Netware format listing?
     * 
     * @param raw   raw listing line
     * @return true if Netware, false otherwise
     */
    public static boolean isNetware(String raw) {
        raw = raw.trim();
        if (raw.length() < 3)
            return false;
        char ch1 = raw.charAt(0);
        char ch2 = raw.charAt(2);
        if ((ch1 == '-' || ch1 == 'd') && (ch2 == '['))
            return true;
        return false;
    } 
    
    
    /**
     * Parse server supplied string, e.g.:
     * 
     * d [RWCEAFMS] PhilliJb                          512 May 10  2007 2007 Upgrade
     * - [RWCEAFMS] PhilliJb                       700730 Jun 26  2008 xtag_manual_v1.5.pdf
     * 
     * @param raw   raw string to parse
     */
    public FTPFile parse(String raw) throws ParseException {
        
        // test it is a valid line, e.g. "total 342522" is invalid
        if (!isNetware(raw))
            return null;
        
        String[] fields = split(raw);
         
        if (fields.length < MIN_FIELD_COUNT) {
            StringBuffer msg = new StringBuffer("Unexpected number of fields in listing '");
            msg.append(raw).append("' - expected minimum ").append(MIN_FIELD_COUNT). 
                    append(" fields but found ").append(fields.length).append(" fields");
            log.warn(msg.toString());
            return null;
        }
        
        // field pos
        int index = 0;
        
        // first field is perms
        boolean isDir = false;
        char ch = raw.charAt(0);
        if (ch == DIRECTORY_CHAR)
            isDir = true;
        
        String permissions = fields[++index];
        if (permissions.charAt(0) == '[' && permissions.charAt(permissions.length()-1) == ']') {
            permissions = permissions.substring(1);
            permissions = permissions.substring(0, permissions.length()-1);
        }
                
        // owner and group
        String owner = fields[++index];
        
        // size
        long size = 0L;
        String sizeStr = fields[++index];
        try {
            size = Long.parseLong(sizeStr);
        }
        catch (NumberFormatException ex) {
            log.warn("Failed to parse size: " + sizeStr);
        }
        
        // next 3 are the date time
        String month = fields[++index];
        String day = fields[++index];
        String year = fields[++index];
        String time = "00:00";
        
        Calendar cal = Calendar.getInstance();
        if (year.indexOf(':') > 0) {
            time = year;
            year = Integer.toString(cal.get(Calendar.YEAR));
        }
            
        // put together & parse        
        StringBuffer stamp = new StringBuffer(month);
        stamp.append('-');
        stamp.append(day);
        stamp.append('-');
        stamp.append(year);
        stamp.append('-');
        stamp.append(time);
        
        Date lastModified = null;
        try {
            lastModified = dateFormatter.parse(stamp.toString());
        }
        catch (ParseException ex) {
            if (!ignoreDateParseErrors)
                throw new DateParseException(ex.getMessage());
        }
                  
        // can't be in the future - must be the previous year
        // add 2 days just to allow for different time zones
        cal.add(Calendar.DATE, 2);
        if (lastModified != null && lastModified.after(cal.getTime())) {
            cal.setTime(lastModified);
            cal.add(Calendar.YEAR, -1);
            lastModified = cal.getTime();
        }
                
        // we've got to find the starting point of the name. 
        String name = raw.trim();
        for (int i = 0; i < MIN_FIELD_COUNT-1; i++) {
            int pos = name.indexOf(' ');
            if (pos > 0)
            {
                name = name.substring(pos).trim();
            }     
            else 
            {
                name = null;
                log.debug("Failed to extract filename");
                break;
            }     
        }
        
        // trim off './' if it is there
        if (name.startsWith(CURRENT_DIR_PREFIX))
            name = name.substring(CURRENT_DIR_PREFIX.length());
         
        FTPFile file = new FTPFile(raw, name, size, isDir, lastModified);
        file.setOwner(owner);
        file.setPermissions(permissions);
        return file;
    }
}
