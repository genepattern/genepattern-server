/**
 *
 *  Copyright (C) 2000-2005  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: VersionDetails.java,v $
 *        Revision 1.6  2009-01-19 23:10:37  bruceb
 *        make more robust
 *
 *        Revision 1.5  2008-09-23 03:55:29  bruceb
 *        fixed codesource location
 *
 *        Revision 1.4  2008-08-26 04:36:26  bruceb
 *        print CodeSource
 *
 *        Revision 1.3  2007-05-29 03:10:09  bruceb
 *        add default unknown version string
 *
 *        Revision 1.2  2005/11/15 21:01:53  bruceb
 *        fix javadoc
 *
 *        Revision 1.1  2005/11/10 13:40:28  bruceb
 *        more elaborate versioning info to debug
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.CodeSource;

/**
 *  Aggregates the version information
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.6 $
 */
public class VersionDetails {
    
    /**
     * Major version (substituted by ant)
     */
    final private static String majorVersion = "2";
    
    /**
     * Middle version (substituted by ant)
     */
    final private static String middleVersion = "4";
    
    /**
     * Middle version (substituted by ant)
     */
    final private static String minorVersion = "0";
    
    /**
     * Full version
     */
    private static int[] version;
    
    /**
     * Full version string
     */
    private static String versionString;
    
    /**
     * Timestamp of build
     */
    final private static String buildTimestamp = "24-Dec-2012 12:36:06 EST";
    
    /**
     * Work out the version array
     */
    static {
        try {
            version = new int[3];
            version[0] = Integer.parseInt(majorVersion);
            version[1] = Integer.parseInt(middleVersion);
            version[2] = Integer.parseInt(minorVersion);
            versionString = version[0] + "." + version[1] + "." + version[2];
        }
        catch (NumberFormatException ex) {
            System.err.println("Failed to calculate version: " + ex.getMessage());
            versionString = "Unknown";
        }
    }
    
    /**
     * Get the product version
     * 
     * @return int array of {major,middle,minor} version numbers 
     */
    final public static int[] getVersion() {
        return version;
    }
    
    /**
     * Get the product version string
     * 
     * @return version as string 
     */
    final public static String getVersionString() {
        return versionString;
    }
    
    /**
     * Get the build timestamp
     * 
     * @return d-MMM-yyyy HH:mm:ss z build timestamp 
     */
    final public static String getBuildTimestamp() {
        return buildTimestamp;
    }
    
    /**
     * Report on useful things for debugging purposes
     * 
     * @param obj     instance to report on
     * @return string
     */
    final public static String report(Object obj) {
        StringWriter result = new StringWriter();
        PrintWriter report = new PrintWriter(result, true);
        try {     
            if (obj != null) {
                report.print("Class: ");
                report.println(obj.getClass().getName());
                report.print("Location: ");
                CodeSource cs = obj.getClass().getProtectionDomain().getCodeSource();
                if (cs != null)
                    report.println(cs.getLocation().toString());
                else
                    report.println("unknown");
            }
            else {
                report.print("Null object supplied");                
            }
            report.print("Version: ");
            report.println(versionString);
            report.print("Build timestamp: ");
            report.println(buildTimestamp);
        
            report.print("Java version: ");
            report.println(System.getProperty("java.version"));
            report.print("CLASSPATH: ");
            report.println(System.getProperty("java.class.path"));
            report.print("OS name: ");
            report.println(System.getProperty("os.name"));
            report.print("OS arch: ");
            report.println(System.getProperty("os.arch"));
            report.print("OS version: ");
            report.println(System.getProperty("os.version"));
        }
        catch (Throwable ex) {
            report.println("Could not obtain version details: " + ex.getMessage());
        }
        return result.toString();
    }
}
