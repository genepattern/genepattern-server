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
 *    $Log: FTPFileFactory.java,v $
 *    Revision 1.27  2012/08/17 04:15:28  bruceb
 *    OS400 changes
 *
 *    Revision 1.26  2012-02-07 03:20:26  bruceb
 *    MVS parser
 *
 *    Revision 1.25  2011-05-03 01:58:08  bruceb
 *    make 1.4 compilable
 *
 *    Revision 1.24  2010-04-26 15:51:31  bruceb
 *    add parse for single line parsing
 *
 *    Revision 1.23  2010-03-25 04:03:11  bruceb
 *    netware
 *
 *    Revision 1.22  2010-02-25 01:22:32  bruceb
 *    add toString()
 *
 *    Revision 1.21  2010-02-17 01:42:49  bruceb
 *    use indexOf
 *
 *    Revision 1.20  2010-02-17 01:35:25  bruceb
 *    AIX => UNIX
 *
 *    Revision 1.19  2009-06-22 09:09:00  bruceb
 *    bug fix re disconnect/reconnect
 *
 *    Revision 1.18  2009-01-15 03:38:11  bruceb
 *    remember what parser was detected
 *
 *    Revision 1.17  2008-07-15 05:41:33  bruceb
 *    refactor parsing code
 *
 *    Revision 1.16  2007-10-12 05:21:44  bruceb
 *    multiple locale stuff
 *
 *    Revision 1.15  2007/02/26 07:15:52  bruceb
 *    Add getVMSParser() method
 *
 *    Revision 1.14  2006/10/27 15:38:16  bruceb
 *    renamed logger
 *
 *    Revision 1.13  2006/10/11 08:54:34  hans
 *    made cvsId final
 *
 *    Revision 1.12  2006/05/24 11:35:54  bruceb
 *    fix VMS problem for listings over 3+ lines
 *
 *    Revision 1.11  2006/01/08 19:10:19  bruceb
 *    better error information
 *
 *    Revision 1.10  2005/06/10 15:43:41  bruceb
 *    more VMS tweaks
 *
 *    Revision 1.9  2005/06/03 11:26:05  bruceb
 *    VMS stuff
 *
 *    Revision 1.8  2005/04/01 13:57:35  bruceb
 *    added some useful debug
 *
 *    Revision 1.7  2004/10/19 16:15:16  bruceb
 *    swap to unix if seems like unix listing
 *
 *    Revision 1.6  2004/10/18 15:57:16  bruceb
 *    set locale
 *
 *    Revision 1.5  2004/08/31 10:45:50  bruceb
 *    removed unused import
 *
 *    Revision 1.4  2004/07/23 08:31:52  bruceb
 *    parser rotation
 *
 *    Revision 1.3  2004/05/01 11:44:21  bruceb
 *    modified for server returning "total 3943" as first line
 *
 *    Revision 1.2  2004/04/17 23:42:07  bruceb
 *    file parsing part II
 *
 *    Revision 1.1  2004/04/17 18:37:23  bruceb
 *    new parse functionality
 *
 */

package com.enterprisedt.net.ftp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.enterprisedt.util.debug.Logger;

/**
 *  Factory for creating FTPFile objects
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.27 $
 */
public class FTPFileFactory {
    
    /**
     *  Revision control id
     */
    public static final String cvsId = "@(#)$Id: FTPFileFactory.java,v 1.27 2012/08/17 04:15:28 bruceb Exp $";
    
    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("FTPFileFactory");

    /**
     * Windows server comparison string
     */
    final static String WINDOWS_STR = "WINDOWS";
    
    /**
     * Netware server comparison string
     */
    final static String NETWARE_STR = "NETWARE";
                  
    /**
     * UNIX server comparison string
     */
    final static String UNIX_STR = "UNIX";
    
    /**
     * VMS server comparison string
     */
    final static String VMS_STR = "VMS";
    
    /**
     * AIX server comparison string
     */
    final static String AIX_STR = "AIX";
    
    /**
     * MVS server comparison string
     */
    final static String MVS_STR = "MVS";
    
    /**
     * OS/400 server comparison string
     */
    final static String OS400_STR = "OS/400";
        
    /**
     * SYST string
     */
    private String system;
    
    /**
     * Cached windows parser
     */
    private WindowsFileParser windows = new WindowsFileParser();
    
    /**
     * Cached unix parser
     */
    private FTPFileParser unix = new UnixFileParser();
    
    /**
     * Cached vms parser
     */
    private VMSFileParser vms = new VMSFileParser();
    
    
    /**
     * Cached netware parser
     */
    private NetwareFileParser netware = new NetwareFileParser();
    
    /**
     * Cached MVS parser
     */
    private MVSFileParser mvs = new MVSFileParser();
    
    /**
     * Cached OS400 parser
     */
    private OS400FileParser os400 = new OS400FileParser();
   
    /**
     * Current parser
     */
    private FTPFileParser parser = null;
                   
    /**
     * User set the parser - don't detect
     */
    private boolean userSetParser = false;
    
    /**
     * Has the parser been detected?
     */
    private boolean parserDetected = false;
    
    /**
     * Locales to try out
     */
    private Locale[] localesToTry;
    
    /**
     * Index of locale to try next
     */
    private int localeIndex = 0;
    
    /**
     * All the parsers
     */
    private List parsers = new ArrayList();
    
    {
        parsers.add(unix);
        parsers.add(windows);
        parsers.add(vms);
        parsers.add(netware);
        parsers.add(mvs);
        parsers.add(os400);
    }
     
    /**
     * Constructor
     * 
     * @param system    SYST string
     */
    public FTPFileFactory(String system) throws FTPException {
        setParser(system);
    }
    
    /**
     * Constructor. User supplied parser. Note that parser
     * rotation (in case of a ParseException) is disabled if
     * a parser is explicitly supplied
     * 
     * @param parser   the parser to use
     */
    public FTPFileFactory(FTPFileParser parser) {
        this.parser = parser;
        userSetParser = true;
    } 
    
    public String toString() {
        return parser.getClass().getName();
    }
    
    
    /**
     * Return a reference to the VMS parser being used.
     * This allows the user to set VMS-specific settings on
     * the parser.
     * 
     * @return  VMSFileParser object
     */
    public VMSFileParser getVMSParser() {
        return vms;
    }
    
    /**
     * Rather than forcing a parser (as in the constructor that accepts
     * a parser), this adds a parser to the list of those used.
     * 
     * @param parser   user supplied parser to add
     */
    public void addParser(FTPFileParser parser) {
        parsers.add(parser);
    }


    /**
     * Set the locale for date parsing of listings
     * 
     * @param locale    locale to set
     */
    public void setLocale(Locale locale) {        
        parser.setLocale(locale); // might be user supplied
        Iterator i = parsers.iterator();
        while (i.hasNext()) {
            FTPFileParser p = (FTPFileParser)i.next();
            p.setLocale(locale);
        }
    }
    
    /**
     * Set the locales to try for date parsing of listings
     * 
     * @param locales    locales to try
     */
    public void setLocales(Locale[] locales) {
        this.localesToTry = locales;
        setLocale(locales[0]); 
        localeIndex = 1;
    }
    
    /**
     * Set the remote server type
     * 
     * @param system    SYST string
     */
    private void setParser(String system) {
        parserDetected = false;
        this.system = system != null ? system.trim() : null;
        if (system.toUpperCase().startsWith(WINDOWS_STR)) {
            log.debug("Selected Windows parser");
            parser = windows;
        }
        else if (system.toUpperCase().indexOf(UNIX_STR) >= 0 ||
                system.toUpperCase().indexOf(AIX_STR) >= 0) {
            log.debug("Selected Unix parser");
            parser = unix;
        }
        else if (system.toUpperCase().indexOf(VMS_STR) >= 0) {
            log.debug("Selected VMS parser");
            parser = vms;
        }
        else if (system.toUpperCase().indexOf(NETWARE_STR) >= 0) {
            log.debug("Selected Netware parser");
            parser = netware;
        }
        else if (system.toUpperCase().indexOf(MVS_STR) >= 0) {
            log.debug("Selected MVS parser");
            parser = mvs;
        }
        else if (system.toUpperCase().indexOf(OS400_STR) >= 0)
        {
            log.debug("Selected OS/400 parser");
            parser = os400;
        }
        else {
            parser = unix;
            log.warn("Unknown SYST '" + system + "' - defaulting to Unix parsing");
        }
    }
    
    /**
     * Reinitialize the parsers
     */
    private void reinitializeParsers() {        
        parser.setIgnoreDateParseErrors(false);
        Iterator i = parsers.iterator();
        while (i.hasNext()) {
            FTPFileParser p = (FTPFileParser)i.next();
            p.setIgnoreDateParseErrors(false);
        }
    }
    
    private void detectParser(String[] files) {
        // use the initially set parser (from SYST)
        if (parser.isValidFormat(files)) {
            log.debug("Confirmed format " + parser.toString());
            parserDetected = true;
            return;
        }   
        Iterator i = parsers.iterator();
        while (i.hasNext()) {
            FTPFileParser p = (FTPFileParser)i.next();
            if (p.isValidFormat(files)) {
                parser = p;
                log.debug("Detected format " + parser.toString());
                parserDetected = true;
                return;
            }
        }
        parser = unix;
        log.warn("Could not detect format. Using default " + parser.toString());
    }
    
    /**
     * Parse a single line of file listing
     * 
     * @param line
     * @return FTPFile
     * @throws ParseException
     */
    public FTPFile parse(String line) throws ParseException {
        if (parser.isMultiLine())
            throw new ParseException("Cannot use this method with multi-line parsers", 0);
        FTPFile file = null;
        try {
            file = parser.parse(line);
        }
        catch (DateParseException ex) {
            parser.setIgnoreDateParseErrors(true);
            file = parser.parse(line);
        }
        return file;
    }
    
    
    /**
     * Parse an array of raw file information returned from the
     * FTP server
     * 
     * @param files     array of strings
     * @return array of FTPFile objects
     */
    public FTPFile[] parse(String[] files) throws ParseException {
               
        reinitializeParsers();
        
        FTPFile[] temp = new FTPFile[files.length];
        
        // quick check if no files returned
        if (files.length == 0)
            return temp;
        
        if (!userSetParser && !parserDetected)
            detectParser(files);
                
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            
            if (files[i] == null || files[i].trim().length() == 0)
                continue;

            try {
                FTPFile file = null;
                if(parser.isMultiLine()) {
                    // vms uses more than 1 line for some file listings. We must keep going
                    // thru till we've got everything
                    StringBuffer filename = new StringBuffer(files[i]);
                    while (i+1 < files.length && files[i+1].indexOf(';') < 0) {
                        filename.append(" ").append(files[i+1]);
                        i++;
                    }
                    file = parser.parse(filename.toString());
                }
                else {
                    file = parser.parse(files[i]);
                }
                // we skip null returns - these are duff lines we know about and don't
                // really want to throw an exception
                if (file != null) {
                    temp[count++] = file;
                }
            }
            catch (DateParseException ex) {
                // try going thru the locales               
                if (localesToTry != null && localesToTry.length > localeIndex) {
                    log.info("Trying " + localesToTry[localeIndex].toString() + " locale");
                    setLocale(localesToTry[localeIndex]);
                    localeIndex++;  
                    count = 0;
                    i = -1; // account for the increment to set i back to 0
                    continue;
                }  
                // from this point start again ignoring date errors (we've tried all our locales)
                count = 0;
                i = -1; // account for the increment to set i back to 0
                parser.setIgnoreDateParseErrors(true);
                log.debug("Ignoring date parsing errors");
                continue;
            }
        }
        FTPFile[] result = new FTPFile[count];
        System.arraycopy(temp, 0, result, 0, count);
        return result;
    }
    
    

    /**
     * Get the SYST string
     * 
     * @return the system string.
     */
    public String getSystem() {
        return system;
    }


}
