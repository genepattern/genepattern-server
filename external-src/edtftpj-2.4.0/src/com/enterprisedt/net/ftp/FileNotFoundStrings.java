/**
 *
 *  Copyright (C) 2000-2007  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: FileNotFoundStrings.java,v $
 *        Revision 1.4  2011-10-26 23:51:47  bruceb
 *        extra strings
 *
 *        Revision 1.3  2007-10-23 07:20:06  bruceb
 *        new string
 *
 *        Revision 1.2  2007-07-05 05:27:40  bruceb
 *        extra strings added
 *
 *        Revision 1.1  2007/01/12 02:04:23  bruceb
 *        string matchers
 *
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Contains fragments of server replies that indicate no files were
 *  found in a supplied directory.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.4 $
 */
final public class FileNotFoundStrings extends ServerStrings {

    /**
     * Server string indicating file not found
     */
    final public static String FILE_NOT_FOUND = "NOT FOUND";
    
    /**
     * Server string indicating file not found
     */
    final public static String NO_SUCH_FILE = "NO SUCH FILE";
    
    /**
     * Server string indicating file not found 
     */
    final public static String CANNOT_FIND_THE_FILE = "CANNOT FIND THE FILE";
    
    /**
     * Server string indicating file not found 
     */
    final public static String CANNOT_FIND = "CANNOT FIND";
    
    /**
     * Server string indicating file not found
     */
    final public static String FAILED_TO_OPEN_FILE = "FAILED TO OPEN FILE";
    
    /**
     * Server string indicating file not found
     */
    final public static String COULD_NOT_GET_FILE = "COULD NOT GET FILE";
    
    /**
     * Server string indicating file not found
     */
    final public static String DOES_NOT_EXIST = "DOES NOT EXIST";
    
    /**
     * Server string indicating file not found
     */
    final public static String NOT_REGULAR_FILE = "NOT A REGULAR FILE";

    
    /**
     * Constructor. Adds the fragments to match on
     */
    public FileNotFoundStrings() {
        add(FILE_NOT_FOUND);
        add(NO_SUCH_FILE);
        add(CANNOT_FIND_THE_FILE);
        add(FAILED_TO_OPEN_FILE);
        add(COULD_NOT_GET_FILE);
        add(DOES_NOT_EXIST);
        add(NOT_REGULAR_FILE);            
        add(CANNOT_FIND);
    }

}
