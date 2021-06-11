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
 *    $Log: MLSXEntryParser.java,v $
 *    Revision 1.2  2011-03-18 06:28:16  bruceb
 *    fix setPath
 *
 *    Revision 1.1  2007/01/15 23:02:50  bruceb
 *    Parses MLST and MLSD entries
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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *  Parses the string returned from the MLSD or MLST command 
 *  (defined in the "Extensions to FTP" IETF draft). Just grabs
 *  the basic fields, as most servers don't support anything else.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class MLSXEntryParser extends FTPFileParser {

    /**
     *  Revision control id
     */
	final public static String cvsId = "@(#)$Id: MLSXEntryParser.java,v 1.2 2011-03-18 06:28:16 bruceb Exp $";

    /**
     * Fields that are possible
     */
    final private static String SIZE = "Size";
    
    final private static String MODIFY = "Modify";
    
    final private static String CREATE = "Create";
    
    final private static String TYPE = "Type";
    
    final private static String UNIQUE = "Unique";
    
    final private static String PERM = "Perm";
    
    final private static String LANG = "Lang";
    
    final private static String MEDIA_TYPE = "Media-Type";
    
    final private static String CHARSET = "CharSet";
        
    /**
     * File type constants
     */
    final private static String FILE_TYPE  = "file"; // a file entry
    final private static String LISTED_DIR_TYPE  = "cdir"; // the listed directory
    final private static String PARENT_DIR_TYPE = "pdir"; // a parent directory
    final private static String SUB_DIR_TYPE = "dir";   // a directory or sub-directory
    
    /**
     *  Format to interpret MTDM timestamp
     */
    private SimpleDateFormat tsFormat1 =
        new SimpleDateFormat("yyyyMMddHHmmss");
    
    /**
     *  Format to interpret MTDM timestamp
     */
    private SimpleDateFormat tsFormat2 =
        new SimpleDateFormat("yyyyMMddHHmmss.SSS");
    
    /**
     *  Instance initializer. Sets formatters to GMT.
     */
    {
        tsFormat1.setTimeZone(TimeZone.getTimeZone("GMT"));
        tsFormat2.setTimeZone(TimeZone.getTimeZone("GMT"));
    }  
            
    /**
     * Parse server supplied string that is returned from MLST/D
     * 
     * @param raw   raw string to parse
     */
    public FTPFile parse(String raw) throws ParseException {
        String[] fields = split(raw, ';');
        String path = null;
        FTPFile ftpFile = new FTPFile(raw);
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if (i+1 == fields.length) {
                path = field.trim();  // last field is the path
                String name = path;
                int pos = name.lastIndexOf('/');
                if (pos >= 0) {
                    path = name.substring(0, pos);
                    ftpFile.setPath(path);
                    name = name.substring(++pos);
                }
                ftpFile.setName(name);                
            }
            else {
                int pos = field.indexOf('=');
                if (pos > 0) {
                    String name = field.substring(0, pos);
                    String value = field.substring(++pos);
                    if (name.equalsIgnoreCase(SIZE)) {
                        ftpFile.setSize(parseSize(value));
                    }
                    else if (name.equalsIgnoreCase(MODIFY)) {
                        ftpFile.setLastModified(parseDate(value));
                    }
                    else if (name.equalsIgnoreCase(TYPE)) {
                        if (value.equalsIgnoreCase(FILE_TYPE))
                            ftpFile.setDir(false);
                        else // assume a dir if not a file for the moment
                            ftpFile.setDir(true);
                    }
                    else if (name.equalsIgnoreCase(PERM)) {
                        ftpFile.setPermissions(value);
                    }
                    else if (name.equalsIgnoreCase(CREATE)) {
                        ftpFile.setCreated(parseDate(value));
                    }
                }
            }
        }
        return ftpFile;
    }
    
    /**
     * Parse the size string
     * 
     * @param value   string containing size value
     * @return
     * @throws ParseException
     */
    private long parseSize(String value) throws ParseException {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ex) {
            throw new ParseException("Failed to parse size: " + value, 0);
        }
    }
    
    /**
     * Parse the date string. In format YYYYMMDDHHMMSS.sss
     * 
     * @param value     string to parse
     * @return Date from string
     * @throws ParseException
     */
    private Date parseDate(String value) throws ParseException {
        try {
            return tsFormat1.parse(value);
        }
        catch (ParseException ex) {
            return tsFormat2.parse(value);
        }
    }
    
    /**
     * Set the locale for date parsing of listings. As
     * the timestamps follow a standard without names of months,
     * this is not used in this parser.
     * 
     * @param locale    locale to set
     */
    public void setLocale(Locale locale) {
        // not required
    }  
}
