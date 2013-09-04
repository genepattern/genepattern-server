/**
 *
 *  Copyright (C) 2004  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: FileParserTest.java,v $
 *        Revision 1.7  2010-11-04 01:08:22  bruceb
 *        other locale before english
 *
 *        Revision 1.6  2010-03-25 04:02:19  bruceb
 *        specify classname
 *
 *        Revision 1.5  2007-10-12 05:21:54  bruceb
 *        test locales
 *
 *        Revision 1.4  2007/03/28 06:04:57  bruceb
 *        add locale arg
 *
 *        Revision 1.3  2005/06/03 11:27:05  bruceb
 *        comment update
 *
 *        Revision 1.2  2004/10/19 16:16:08  bruceb
 *        made test more realistic
 *
 *        Revision 1.1  2004/09/17 14:23:03  bruceb
 *        test harness
 *
 *
 */
package com.enterprisedt.net.ftp.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.Vector;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPFileFactory;
import com.enterprisedt.net.ftp.FTPFileParser;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

/**
 *  Test harness for testing out listings. Simply copy and
 *  paste a listing into a file and use this test harness to
 *  pinpoint the error 
 * 
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.7 $
 */
public class FileParserTest {
    
    /**
     * Standard main()
     * 
     * @param args  standard args - supply filename
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
        }
        
        Logger log = Logger.getLogger(FileParserTest.class);
        Logger.setLevel(Level.ALL);
        
        String type = args[0];
        String filename = args[1];
        String locale = null;
        if (args.length ==3)
            locale = args[2];
        FTPFileParser parser = null;
        try {
            Class clazz = Class.forName(type);
            parser = (FTPFileParser)clazz.newInstance();
        }
        catch (Throwable t) {
            usage();
        }
        log.debug("Type=" + type);
        
        Vector lines = new Vector();
        BufferedReader reader = null;
        String line = null;
        try {
            FTPFileFactory ff = new FTPFileFactory(parser);
            if (locale != null) {
                System.out.println("Setting locale to " + locale);
                Locale l = new Locale(locale);
                Locale[] locales = new Locale[2];
                locales[0] = l;
                locales[1] = Locale.ENGLISH;
                ff.setLocales(locales);
            }
            else {
                Locale[] locales = new Locale[1];
                locales[0] = Locale.ENGLISH;
                ff.setLocales(locales);
            }
            reader = new BufferedReader(new FileReader(filename));
            while ((line = reader.readLine()) != null) {
                lines.addElement(line);
                System.out.println(line);
            }
            String[] listings = new String[lines.size()];
            lines.copyInto(listings);
            FTPFile[] files = ff.parse(listings);
            for (int i = 0; i < files.length; i++)
                System.out.println(files[i].toString());
        }
        catch (IOException ex) {
            System.out.println("Failed to read file: " + filename);
            ex.printStackTrace();
        } 
        catch (ParseException ex) {
            System.out.println("Failed to parse line '" + line + "'");
            ex.printStackTrace();
        }
    }
    
    /**
     * Usage statement
     *
     */
    private static void usage() {
        System.out.println("Usage: FileParserTest parserclass filename [locale]");
        System.exit(-1);        
    }

}
