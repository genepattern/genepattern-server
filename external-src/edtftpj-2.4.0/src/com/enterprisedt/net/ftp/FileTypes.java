/**
 *
 *  Copyright (C) 2000-2004  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: FileTypes.java,v $
 *        Revision 1.4  2009-01-15 03:38:44  bruceb
 *        make final
 *
 *        Revision 1.3  2007/04/26 04:12:43  hans
 *        Added lots of new ASCII types.
 *
 *        Revision 1.2  2005/11/15 21:01:40  bruceb
 *        make 1.1.x compliant
 *
 *        Revision 1.1  2005/11/09 21:14:52  bruceb
 *        from j/ssl and enhanced
 *
 *        Revision 1.1  2005/01/28 14:15:27  bruceb
 *        recursive support
 *
 *        Revision 1.1  2005/01/11 23:20:59  bruceb
 *        first cut
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *  Attempts to classify files as ASCII or binary via their filename 
 *  extension. Email support at enterprisedt dot com if you feel we
 *  have missed out important file types. Of course, extensions can
 *  be registered and unregistered at runtime to customize file types for
 *  different applications
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.4 $
 */
public class FileTypes {
        
    /**
     * Holds map of ASCII extensions
     */
    private Hashtable fileTypes = new Hashtable();
    
    public static final FileTypes ASCII = new FileTypes();
    
    public static final FileTypes BINARY = new FileTypes();
    
    static {
        // ASCII default extensions
        ASCII.registerExtension("ANS");
        ASCII.registerExtension("ASC");
        ASCII.registerExtension("ASM");
        ASCII.registerExtension("ASP");
        ASCII.registerExtension("ASPX");
        ASCII.registerExtension("ATOM");
        ASCII.registerExtension("AWK");
        ASCII.registerExtension("BAT");
        ASCII.registerExtension("BAS");
        ASCII.registerExtension("C");
        ASCII.registerExtension("CFM");
        ASCII.registerExtension("E");
        ASCII.registerExtension("CMD");
        ASCII.registerExtension("CGI");
        ASCII.registerExtension("COB");
        ASCII.registerExtension("CPP");
        ASCII.registerExtension("CS");
        ASCII.registerExtension("CSS");
        ASCII.registerExtension("CSV");
        ASCII.registerExtension("EPS");
        ASCII.registerExtension("F");
        ASCII.registerExtension("F77");
        ASCII.registerExtension("FOR");
        ASCII.registerExtension("FRM");
        ASCII.registerExtension("FTN");
        ASCII.registerExtension("H");
        ASCII.registerExtension("HPP");
        ASCII.registerExtension("HTM");
        ASCII.registerExtension("HTML");
        ASCII.registerExtension("HXX");
        ASCII.registerExtension("EML");
        ASCII.registerExtension("INC");
        ASCII.registerExtension("INF");
        ASCII.registerExtension("INFO");
        ASCII.registerExtension("INI");
        ASCII.registerExtension("JAVA");
        ASCII.registerExtension("JS");
        ASCII.registerExtension("JSP");
        ASCII.registerExtension("KSH");
        ASCII.registerExtension("LOG");
        ASCII.registerExtension("M");
        ASCII.registerExtension("PHP");
        ASCII.registerExtension("PHP1");
        ASCII.registerExtension("PHP2");
        ASCII.registerExtension("PHP3");
        ASCII.registerExtension("PHP4");
        ASCII.registerExtension("PHP5");
        ASCII.registerExtension("PHP6");
        ASCII.registerExtension("PHP7");
        ASCII.registerExtension("PHTML");
        ASCII.registerExtension("PL");
        ASCII.registerExtension("PS");
        ASCII.registerExtension("PY");
        ASCII.registerExtension("R");
        ASCII.registerExtension("RESX");
        ASCII.registerExtension("RSS");
        ASCII.registerExtension("SCPT");
        ASCII.registerExtension("SH");
        ASCII.registerExtension("SHP");
        ASCII.registerExtension("SHTML");
        ASCII.registerExtension("SQL");
        ASCII.registerExtension("SSI");
        ASCII.registerExtension("SVG");
        ASCII.registerExtension("TAB");
        ASCII.registerExtension("TCL");
        ASCII.registerExtension("TEX");
        ASCII.registerExtension("TXT");
        ASCII.registerExtension("UU");
        ASCII.registerExtension("UUE");
        ASCII.registerExtension("VB");
        ASCII.registerExtension("VBS");
        ASCII.registerExtension("XHTML");
        ASCII.registerExtension("XML");
        ASCII.registerExtension("XSL");
        
        // binary default extensions
        BINARY.registerExtension("EXE");
        BINARY.registerExtension("PDF");
        BINARY.registerExtension("XLS");
        BINARY.registerExtension("DOC");
        BINARY.registerExtension("CHM");
        BINARY.registerExtension("PPT");
        BINARY.registerExtension("DOT");
        BINARY.registerExtension("DLL");
        BINARY.registerExtension("GIF");
        BINARY.registerExtension("JPG");
        BINARY.registerExtension("JPEG");
        BINARY.registerExtension("BMP");
        BINARY.registerExtension("TIF");
        BINARY.registerExtension("TIFF");
        BINARY.registerExtension("CLASS");
        BINARY.registerExtension("JAR");
        BINARY.registerExtension("SO");
        BINARY.registerExtension("AVI");
        BINARY.registerExtension("MP3");
        BINARY.registerExtension("MPG");
        BINARY.registerExtension("MPEG");
        BINARY.registerExtension("MSI");
        BINARY.registerExtension("OCX");
        BINARY.registerExtension("ZIP");
        BINARY.registerExtension("GZ");
        BINARY.registerExtension("RAM");
        BINARY.registerExtension("WAV");
        BINARY.registerExtension("WMA");
        BINARY.registerExtension("XLA");
        BINARY.registerExtension("XLL");
        BINARY.registerExtension("MDB");
        BINARY.registerExtension("MOV");
        BINARY.registerExtension("OBJ");
        BINARY.registerExtension("PUB");
        BINARY.registerExtension("PCX");
        BINARY.registerExtension("MID");
        BINARY.registerExtension("BIN");
        BINARY.registerExtension("WKS");
        BINARY.registerExtension("PNG");
        BINARY.registerExtension("WPS");
        BINARY.registerExtension("AAC");
        BINARY.registerExtension("AIFF");
        BINARY.registerExtension("PSP");
    }
    
    
    /**
     * Private so others can't create instances
     */
    private FileTypes() {}
    
    /**
     * Get the list of registered file extensions
     * 
     * @return String[] of file extensions
     */
    public String[] extensions() {
        String[] ext = new String[fileTypes.size()];
        Enumeration e = fileTypes.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            ext[i++] = (String)e.nextElement();
        }
        return ext;
    }
    
    /**
     * Register a new file extension
     *  
     * @param ext   filename extension (excluding ".") to register
     */
    public void registerExtension(String ext) {
        ext = ext.toUpperCase();
        fileTypes.put(ext, ext);
    }
    
    /**
     * Unregister a file extension
     *  
     * @param ext   filename extension (excluding ".") to unregister
     */
    public void unregisterExtension(String ext) {
        ext = ext.toUpperCase();
        fileTypes.remove(ext);
    }
    
    /**
     * Determines if a file matches this extension type
     * 
     * @param file  handle to file
     * @return  true if matches, false otherwise
     */
    public boolean matches(File file) {
        return matches(file.getName());
    } 
    
    /**
     * Determines if a file matches this extension type
     * 
     * @param name  file's name
     * @return  true if matches, false otherwise
     */
    public boolean matches(String name) {
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            String ext = name.substring(pos+1).toUpperCase();
            if (fileTypes.get(ext) != null)
                return true;
        }
        return false;
    } 


}
