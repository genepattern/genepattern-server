/*
 * Logo.java
 *
 * Created on May 6, 2002, 4:01 PM
 */

package org.genepattern.modules.ui.graphics;

import java.awt.*;
import javax.swing.*;
import org.genepattern.data.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  KOhm
 * @version 
 */
public class Logo extends javax.swing.JLabel{
     // static variables
    /** the version text to display */
    public static final String VERSION_STUFF;
    /** the major version number ie the 2 in 2.1 */
    public static final String MAJOR_VERSION;
    /** the minor version number ie the 1 in 2.1 */
    public static final String MINOR_VERSION;
    /** the revision number: the 3 in Version 2.1.3 */
    public static final String REVISION;
    /** the date of the build */
    public static final String BUILD_DATE;
    /** the build number reduced to 8 digits */
    public static final long   BUILD;
    /** the build number as a string */
    public static final String BUILD_STRING;
    /** the release alpha beta final etc */
    public static final String RELEASE;
    /** the number of builds */
    public static final int    BUILD_COUNT;
    /** static initializers gets the build/version info */
    static { 
        String major, minor, revision, release, date,  build_string;
        long build = -1L;
        int count  = 0;
        major = minor = revision = release = date = build_string = null;
        Properties internal = null;
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream ("com/mprgroup/resources/properties");
            
            if(in != null) {
                internal = new Properties ();
                internal.load (in);
                System.out.println ("loaded internal properties file:\n"+internal);
            }
        } catch (IOException ex) {
            System.err.println("Logo static initializer: couldn't load the properties file\n"+ex);
            
        }
        if(internal != null) {
            major   = internal.getProperty ("gc.version.major", "2");
            minor   = internal.getProperty ("gc.version.minor", "1");
            revision= internal.getProperty ("gc.version.revision");
            release = internal.getProperty ("gc.release");
            date    = internal.getProperty ("gc.date");
            String tmp;
            
            tmp = internal.getProperty("gc.build");
            build_string = tmp;
            try {// if this fails build = -1
                if(tmp != null) {
                    int len = Math.min(tmp.length(), 8);
                    build   = Long.parseLong (tmp.substring(0, len));
                }
            } catch (NumberFormatException e) {
                System.err.println("Logo static initializer: invalid build");
            }
            try { // if this fails count = 0
                tmp = internal.getProperty ("gc.count");
                if(tmp != null)
                    count   = Integer.parseInt (tmp);
            } catch (NumberFormatException e) {
                System.err.println("Logo static initializer: invalid count");
            }
        } else {
            System.err.println("Logo static initializer: null properties");
            major = minor = "?";
            release = date = "[UNK]";
        }
        
        MAJOR_VERSION = major;
        MINOR_VERSION = minor;
        REVISION      = revision;
        RELEASE       = (release == null || release.equals("0") ? null : release);
        BUILD_DATE    = date;
        BUILD         = build;
        BUILD_STRING  = build_string;
        BUILD_COUNT   = count;
        
        VERSION_STUFF = "Version "+MAJOR_VERSION+"."
            +MINOR_VERSION+(REVISION != null ? "."+REVISION : "")
            +" "+RELEASE+" Build "+BUILD+" "+BUILD_DATE;
    }

    /** Creates new Logo */
    public Logo () {
        ClassLoader cl = getClass().getClassLoader();
        java.net.URL logoURL = cl.getResource("images/GeneCluster_splash.gif");
        logoIcon = new ImageIcon(logoURL);
        
        setIcon(logoIcon);
        setFont(new Font("SansSerif", Font.BOLD, 15));
    }
    
    /** changes the font and sets the half size of the text */
    public final void setFont(Font font) {
        if(font == null)
            return ;
        super.setFont(font);
        FontMetrics metrics = getFontMetrics (font);
        if(metrics != null && VERSION_STUFF != null)
            stringWidth = metrics.stringWidth (VERSION_STUFF);
    }
    /** paints the version and build info over the top of the LOGO */
    public void paintComponent (Graphics g) {
        Dimension d = getSize ();
        if(should_rescale) {
            g.drawImage (logoIcon.getImage (), 0, 0, d.width, d.height, null);
        } else {
            super.paintComponent (g);
        }
        g.setColor (Color.black);
        //g.setFont (getFont());
        g.drawString(VERSION_STUFF, (d.width - stringWidth)/2, (d.height*6)/10);
    }
    // instance fields 
    /** the current size of the text */
    private int stringWidth = 1;
    /** the ImageIcon */
    private ImageIcon logoIcon;
    /** rescale ? */
    public boolean should_rescale = false;

//        final JLabel fLogoLabel = CMJAUI.createLabel(logoIcon, JLabel.CENTER) {
//            /** paints the version and build info over the top of the LOGO */
//            public void paintComponent(Graphics g) {
//                super.paintComponent (g);
//                g.setColor (Color.black);
//                g.setFont(font);
//                Dimension d = getSize ();
//                g.drawString (VERSION_STUFF,
//                    ((d.width - g.getFontMetrics ().stringWidth (VERSION_STUFF)))/2,
//                    (d.height*6)/10);
//            }
//            /** the perferred font */
//            private Font font = new Font("SansSerif", Font.BOLD, 16);
//        };

}
