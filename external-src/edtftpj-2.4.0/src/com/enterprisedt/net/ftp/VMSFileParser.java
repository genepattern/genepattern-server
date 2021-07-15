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
 *
 */
package com.enterprisedt.net.ftp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.enterprisedt.util.debug.Logger;

/**
 *  Represents a remote OpenVMS file parser. Thanks to Jason Schultz for contributing
 *  significantly to this class 
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.9 $
 */
public class VMSFileParser extends FTPFileParser {

    /**
     *  Revision control id
     */
    public static String cvsId = "@(#)$Id: VMSFileParser.java,v 1.9 2011-05-03 01:49:00 bruceb Exp $";
             
    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("VMSFileParser");

    /**
     * Directory field
     */
    private final static String DIR = ".DIR";
    
    /**
     * Directory line
     */
    private final static String HDR = "Directory";
    
    /**
     * Total line
     */
    private final static String TOTAL = "Total";
    
    /**
     * Blocksize for calculating file sizes
     */
    private final static int DEFAULT_BLOCKSIZE = 512*1024;
    
    /**
     * Number of expected fields
     */
    private final static int MIN_EXPECTED_FIELD_COUNT = 4;
    
    /**
     * Date formatter
     */
    private SimpleDateFormat formatter1;
    
    /**
     * Date formatter
     */
    private SimpleDateFormat formatter2;
    
    /**
     * Is the version returned with the name?
     */
    private boolean versionInName = false;

    /**
     * Block size used to calculate size
     */
    private int blocksize = DEFAULT_BLOCKSIZE;
    
    /**
     * Constructor
     */
    public VMSFileParser() {
         setLocale(Locale.getDefault());
    }
    
    /**
     * Does this parser parse multiple lines to get one listing?
     * 
     * @return  
     */
    public boolean isMultiLine() {
        return true;
    }
    
    /**
     * Get the VMS blocksize, used for calculating file 
     * sizes
     * 
     * @return blocksize
     */
    public int getBlocksize() {
        return blocksize;
    }

    /**
     * Set the VMS blocksize, used for calculating file 
     * sizes. This might need to be changed if unexpected file
     * sizes are being returned for VMS files.
     * 
     * @param blocksize   new blocksize
     */
    public void setBlocksize(int blocksize) {
        this.blocksize = blocksize;
    }


    /**
     * Get the property that controls whether or not the version
     * number is returned as part of the filename, e.g. FILENAME.TXT;2
     * would be returned as is if this property is true, or FILENAME.TXT
     * if it is false.
     * 
     * @return true if version to be returned as part of the filename
     */
    public boolean isVersionInName() {
        return versionInName;
    }


    /**
     * Set the property that controls whether or not the version
     * number is returned as part of the filename, e.g. FILENAME.TXT;2
     * would be returned as is if this property is true, or FILENAME.TXT
     * if it is false.
     * 
     * @param versionInName     true if version to be returned as part of the filename
     */
    public void setVersionInName(boolean versionInName) {
        this.versionInName = versionInName;
    }
    
    public String toString() {
        return "VMS";
    }
    
    
    /**
     * Valid format for this parser
     * 
     * @param listing
     * @return true if valid
     */
    public boolean isValidFormat(String[] listing) {
        int count = Math.min(listing.length, 10);
        
        boolean semiColonName = false;
        boolean squareBracketStart = false, squareBracketEnd = false;

        for (int i = 0; i < count; i++) {
            if (listing[i].trim().length() == 0)
                continue;
            int pos = 0;
            if ((pos = listing[i].indexOf(';')) > 0 && (++pos < listing[i].length()) && 
                Character.isDigit(listing[i].charAt(pos)))
                semiColonName = true;
            if (listing[i].indexOf('[') > 0)
                squareBracketStart = true;
            if (listing[i].indexOf(']') > 0)
                squareBracketEnd = true;
        }
        if (semiColonName && squareBracketStart && squareBracketEnd)
            return true;
        log.debug("Not in VMS format");
        return false;
    }

    /**
     * Parse server supplied string
     *
     * OUTPUT: <begin>
     * 
     * Directory <dir>
     *  
     * <filename>
     *      used/allocated  dd-MMM-yyyy HH:mm:ss [unknown]      (PERMS)
     * <filename>
     *      used/allocated  dd-MMM-yyyy HH:mm:ss [unknown]      (PERMS)
     * ...
     * 
     * Total of <> files, <>/<> blocks
     *
     * @param raw   raw string to parse
     */
    public FTPFile parse(String raw) throws ParseException {
        String[] fields = split(raw);
        
        // skip blank lines
        if(fields.length <= 0)
        	return null;
        // skip line which lists Directory
        if (fields.length >= 2 && fields[0].compareTo(HDR) == 0)
        	return null;
        // skip line which lists Total
        if (fields.length > 0 && fields[0].compareTo(TOTAL) == 0)
        	return null;
        // probably the remainder of a listing on 2nd line
        if (fields.length < MIN_EXPECTED_FIELD_COUNT) 
            return null; 
        
        // first field is name
        String name = fields[0];
        
        // make sure it is the name (ends with ';<INT>')
        int semiPos = name.lastIndexOf(';');
        // check for ;
        if(semiPos <= 0) {
            log.warn("File version number not found in name '" + name + "'");
            return null;
        }
        
        String nameNoVersion = name.substring(0, semiPos);
        
        // check for version after ;
        String afterSemi = name.substring(semiPos+1);
        
        try{
            Integer.parseInt(afterSemi);
            // didn't throw exception yet, must be number
            // we don't use it currently but we might in future
        }
        catch(NumberFormatException ex) {
            // don't worry about version number
        }        
        
        // test is dir
        boolean isDir = false;
        if (nameNoVersion.endsWith(DIR)) 
        {
            isDir = true;
            name = nameNoVersion.substring(0, nameNoVersion.length()-DIR.length());
        }
        
        if (!versionInName && !isDir) {
            name = nameNoVersion;
        }
        
        // 2nd field is size USED/ALLOCATED format
        int slashPos = fields[1].indexOf('/');
        String sizeUsed = fields[1];
        if (slashPos > 0)
            sizeUsed = fields[1].substring(0, slashPos);
        long size = Long.parseLong(sizeUsed) * blocksize;
        
        // 3 & 4 fields are date time
        Date lastModified = null;
        try {
            lastModified = formatter1.parse(fields[2] + " " + fields[3]);
        }
        catch (ParseException ex) {
            try {
                lastModified = formatter2.parse(fields[2] + " " + fields[3]);
            }
            catch (ParseException ex1) {
                if (!ignoreDateParseErrors)
                    throw new DateParseException(ex.getMessage());
            }
        }
        
        // 5th field is [group,owner]
        String group = null;
        String owner = null;
        if (fields.length >= 5) {     
            if (fields[4].charAt(0) == '[' && fields[4].charAt(fields[4].length()-1) == ']') {
                int commaPos = fields[4].indexOf(',');
                if (commaPos < 0) {
                    group = owner = fields[4]; // just make them the same, e.g. SYSTEM
                }
                else {
	                group = fields[4].substring(1, commaPos);
	                owner = fields[4].substring(commaPos+1, fields[4].length()-1);
                }
            }
        }
        
        // 6th field is permissions e.g. (RWED,RWED,RE,)
        String permissions = null;
        if (fields.length >= 6) {     
            if (fields[5].charAt(0) == '(' && fields[5].charAt(fields[5].length()-1) == ')') {
                permissions = fields[5].substring(1, fields[5].length()-2);
            }
        }
        
        FTPFile file = new FTPFile(raw, name, size, isDir, lastModified); 
        file.setGroup(group);
        file.setOwner(owner);
        file.setPermissions(permissions);
        return file;        
    }

    /* (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPFileParser#setLocale(java.util.Locale)
     */
    public void setLocale(Locale locale) {
        formatter1 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", locale);
        formatter2 = new SimpleDateFormat("dd-MMM-yyyy HH:mm", locale);
    }
  
}
