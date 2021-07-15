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
 *    $Log: OS400FileParser.java,v $
 *    Revision 1.3  2012/08/17 04:11:56  bruceb
 *    better logging and detection
 *
 *    Revision 1.2  2012-01-19 11:13:32  bruceb
 *    spelling
 *
 *    Revision 1.1  2011-01-16 22:44:36  bruceb
 *    new parser
 *
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
 *  Represents an OS400 file parser
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.3 $
 */
public class OS400FileParser extends FTPFileParser {   
    
    /* 
     * Directory field
     */
    private final static String DIR = "*DIR";

    /*
     * Directory field
     */
    private final static String DDIR = "*DDIR";

    /*
     * MEM field?
     */
    private final static String MEM = "*MEM";

    /* 
     * Number of expected fields
     */
    private final static int MIN_EXPECTED_FIELD_COUNT = 6;

    /*
     * Date formats
     */
    private final static String DATE_FORMAT_1 = "dd'/'MM'/'yy' 'HH':'mm':'ss";	
    private final static String DATE_FORMAT_2 = "dd'.'MM'.'yy' 'HH':'mm':'ss";
    
    private final static String DATE_FORMAT_11 = "MM'/'dd'/'yy' 'HH':'mm':'ss";
    private final static String DATE_FORMAT_12 = "MM'.'dd'.'yy' 'HH':'mm':'ss";

    private final static String DATE_FORMAT_21 = "yy'/'MM'/'dd' 'HH':'mm':'ss";	
    private final static String DATE_FORMAT_22 = "yy'.'MM'.'dd' 'HH':'mm':'ss";

    /*
     * array of formats
     */
    private static String[] formats1 = {DATE_FORMAT_1,DATE_FORMAT_2};
    private static String[] formats2 = {DATE_FORMAT_11,DATE_FORMAT_12};
    private static String[] formats3 = { DATE_FORMAT_21, DATE_FORMAT_22};
    private String[][] formats = {formats1,formats2,formats3};

    /* 
     * Logging object
     */
    private static Logger log = Logger.getLogger("OS400FileParser");

    private int formatIndex = 0;
    
    private Locale locale;
    
    public OS400FileParser() {
        locale = Locale.getDefault();
    }

    /**
     * Set the locale for date parsing of listings
     * 
     * @param locale    locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }  

    public String toString() {
        return "OS400";
    }

    /**
     * Valid format for this parser
     * 
     * @param listing listing to test
     * @return true if valid
     */
    public boolean isValidFormat(String[] listing) {
        int count = Math.min(listing.length, 10);   

        boolean dir = false;
        boolean ddir = false;
        boolean lib = false;
        boolean stmf = false;
        boolean flr = false;
        boolean file = false;
        boolean mem = false;

        for (int i = 0; i < count; i++) {
            if (listing[i].indexOf("*DIR") > 0)
                dir = true;
            else if (listing[i].indexOf("*FILE") > 0)
                file = true;
            else if (listing[i].indexOf("*FLR") > 0)
                flr = true;
            else if (listing[i].indexOf("*DDIR") > 0)
                ddir = true;
            else if (listing[i].indexOf("*STMF") > 0)
                stmf = true;
            else if (listing[i].indexOf("*LIB") > 0)
                lib = true;
            else if (listing[i].indexOf("*MEM") > 0)
                mem = true;
        }
        if (dir || file || ddir || lib || stmf || flr || mem)
            return true;
        log.debug("Not in OS/400 format");
        return false;
    }
    
    /*
     * Parse server supplied string. Listing looks like the below:
     * 
     *        CFT             45056 04/12/06 14:19:31 *FILE AFTFRE1.FILE
     *        CFT                                     *MEM AFTFRE1.FILE/AFTFRE1.MBR
     *        CFT             36864 28/11/06 15:19:30 *FILE AFTFRE2.FILE
     *        CFT                                     *MEM AFTFRE2.FILE/AFTFRE2.MBR
     *        CFT             45056 04/12/06 14:19:37 *FILE AFTFRE6.FILE
     *        CFT                                     *MEM  AFTFRE6.FILE/AFTFRE6.MBR
     *        QSYSOPR         28672 01/12/06 20:08:04 *FILE FPKI45POK5.FILE
     *        QSYSOPR                                 *MEM FPKI45POK5.FILE/FPKI45POK5.MBR        
     */
    public FTPFile parse(String raw) throws DateParseException {
        String[] fields = split(raw);
        
        // skip blank lines
        if(fields.length <= 0)
            return null;
        
        // return what we can for MEM
        if (fields.length >= 2 && fields[1].equals(MEM)) {
            String owner = fields[0];
            String name = fields[2];
            FTPFile file = new FTPFile(raw, name, 0, false, null);
            file.setOwner(owner);
            return file;
        } 
        if (fields.length < MIN_EXPECTED_FIELD_COUNT)
            return null;
        
        // first field is owner
        String owner = fields[0];

        // next is size
        long size = 0L;
        try {
            size = Long.parseLong(fields[1]);
        }
        catch (NumberFormatException ex) {
            log.warn("Failed to parse size: " + fields[1]);
        }
 
        String lastModifiedStr = fields[2] + " " + fields[3];
        Date lastModified = getLastModified(lastModifiedStr);
 
        // test is dir
        boolean isDir = false;
        if (fields[4] == DIR || fields[4] == DDIR)
            isDir = true; 
        
        int pos = raw.indexOf(fields[4]);
        pos += fields[4].length();
        
        String name = trimStart(raw.substring(pos));
        if (name.endsWith("/"))
        {
            isDir = true;
            name = name.substring(0, name.length() - 1);
        }
        
        FTPFile file = new FTPFile(raw, name, size, isDir, lastModified); 
        file.setOwner(owner);
        return file;
    }


    private Date getLastModified(String lastModifiedStr) throws DateParseException {
        Date lastModified = null;
        if (formatIndex >= formats.length) {
            log.warn("Exhausted formats - failed to parse date");
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        
        int prevIndex = formatIndex;
        for (int i = formatIndex; i < formats.length; i++, formatIndex++) {
            for (int j = 0; j < formats[i].length; j++) {
                try {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(formats[formatIndex][j], locale);
                    lastModified = dateFormatter.parse(lastModifiedStr);
                    if (lastModified.after(cal.getTime())) {
                        log.debug("Swapping to alternate date format (found date in future)");
                        continue;
                    }
                    else // all ok, exit loop
                        return lastModified;
                }
                catch (ParseException ex) {
                    continue;
                }
            }
        }
        if (formatIndex >= formats.length) {
            log.warn("Exhausted formats - failed to parse date");
            return null;
        }
        if (formatIndex > prevIndex) { // we've changed formatters so redo
            throw new DateParseException(null);
        }
        return lastModified;
    }        
}

