/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.util;

import java.io.IOException;

/**
 * BrowserLauncher is a class that provides one static method, openURL, which
 * opens the default web browser for the current user of the system to the given
 * URL. It may support other protocols depending on the system -- mailto, ftp,
 * etc. -- but that has not been rigorously tested and is not guaranteed to
 * work.
 * <p>
 * Yes, this is platform-specific code, and yes, it may rely on classes on
 * certain platforms that are not part of the standard JDK. What we're trying to
 * do, though, is to take something that's frequently desirable but inherently
 * platform-specific -- opening a default browser -- and allow programmers (you,
 * for example) to do so without worrying about dropping into native code or
 * doing anything else similarly evil.
 * <p>
 * Anyway, this code is completely in Java and will run on all JDK 1.1-compliant
 * systems without modification or a need for additional libraries. All classes
 * that are required on certain platforms to allow this to run are dynamically
 * loaded at runtime via reflection and, if not found, will not cause this to do
 * anything other than returning an error when opening the browser.
 * <p>
 * There are certain system requirements for this class, as it's running through
 * Runtime.exec(), which is Java's way of making a native system call.
 * Currently, this requires that a Macintosh have a Finder which supports the
 * GURL event, which is true for Mac OS 8.0 and 8.1 systems that have the
 * Internet Scripting AppleScript dictionary installed in the Scripting
 * Additions folder in the Extensions folder (which is installed by default as
 * far as I know under Mac OS 8.0 and 8.1), and for all Mac OS 8.5 and later
 * systems. On Windows, it only runs under Win32 systems (Windows 95, 98, and NT
 * 4.0, as well as later versions of all). On other systems, this drops back
 * from the inherently platform-sensitive concept of a default browser and
 * simply attempts to launch Netscape via a shell command.
 * <p>
 * This code is Copyright 1999-2001 by Eric Albert (ejalbert@cs.stanford.edu)
 * and may be redistributed or modified in any form without restrictions as long
 * as the portion of this comment from this paragraph through the end of the
 * comment is not removed. The author requests that he be notified of any
 * application, applet, or other binary that makes use of this code, but that's
 * more out of curiosity than anything and is not required. This software
 * includes no warranty. The author is not repsonsible for any loss of data or
 * functionality or any adverse or unexpected effects of using this software.
 * <p>
 * Credits: <br>
 * Steven Spencer, JavaWorld magazine ( <a
 * href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip
 * 66 </a>) <br>
 * Thanks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, Andrea
 * Cantatore, Larry Barowski, Trevor Bedzek, Frank Miedrich, and Ron Rabakukk
 * 
 * @author Eric Albert ( <a
 *         href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu </a>)
 * @version 1.4b1 (Released June 20, 2001)
 */
public class BrowserLauncher {

    protected static boolean DEBUG = (System.getProperty("DEBUG") != null);

    /**
     * The Java virtual machine that we are running on. Actually, in most cases
     * we only care about the operating system, but some operating systems
     * require us to switch on the VM.
     */
    private static int jvm;

    /** The browser for the system */
    private static Object browser;

    /**
     * Caches whether any classes, methods, and fields that are not part of the
     * JDK and need to be dynamically loaded at runtime loaded successfully.
     * <p>
     * Note that if this is <code>false</code>,<code>openURL()</code> will
     * always return an IOException.
     */
    private static boolean loadedWithoutErrors;

    /** JVM constant for any Mac JVM */
    private static final int MAC = 4;

    /** JVM constant for any Windows NT JVM */
    private static final int WINDOWS_NT = 5;

    /** JVM constant for any Windows 9x JVM */
    private static final int WINDOWS_9x = 6;

    /** JVM constant for any other platform */
    private static final int OTHER = -1;

    /** specified the path to the browser */
    private static final int SPECIFIED = -2;

    /**
     * The shell parameters for Netscape that opens a given URL in an
     * already-open copy of Netscape on many command-line systems.
     */
    private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";

    private static final String NETSCAPE_OPEN_PARAMETER_START = "'openURL(";

    private static final String NETSCAPE_OPEN_PARAMETER_END = ")'";

    /**
     * The message from any exception thrown throughout the initialization
     * process.
     */
    private static String errorMessage;

    /**
     * An initialization block that determines the operating system and loads
     * the necessary runtime data.
     */
    static {
        loadedWithoutErrors = true;
        final String path = org.genepattern.util.GPpropertiesManager.getProperty("path.to.browser");
        String osName = System.getProperty("os.name");
        if (path != null && path.trim().length() > 0) {
            browser = path;
            jvm = SPECIFIED;
        } else if (osName.startsWith("Mac OS")) {
            jvm = MAC;
        } else if (osName.startsWith("Windows")) {
            if (osName.indexOf("9") != -1) {
                jvm = WINDOWS_9x;
            } else {
                jvm = WINDOWS_NT;
            }
        } else {
            jvm = OTHER;
        }

    }

    /**
     * This class should be never be instantiated; this just ensures so.
     */
    private BrowserLauncher() {
    }

    public static final void main(final String[] args) throws IOException {
        if (System.getProperty("DEBUG") != null)
            System.out.println("Tying to open url with " + browser + " using jvm=" + jvm);
        openURL(args[0]);
        if (System.getProperty("DEBUG") != null)
            System.out.println("Java done!");
    }

    /**
     * Attempts to open the default web browser to the given URL.
     * 
     * @param url
     *            The URL to open
     * @throws IOException
     *             If the web browser could not be located or does not run
     */
    public static void openURL(String url) throws IOException {
        if (!loadedWithoutErrors) {
            System.err.println("Exception in finding browser: " + errorMessage);
            throw new IOException("Exception in finding browser: " + errorMessage);
        }

        // end state info
        switch (jvm) {
        case MAC:
            String[] args = new String[] { "/usr/bin/open", url };
            Runtime.getRuntime().exec(args);
            break;

        case WINDOWS_NT:
        case WINDOWS_9x:
            Process process = Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            // Add quotes around the URL to allow ampersands and other special
            // characters to work.
            /*
             * Process process = Runtime.getRuntime().exec(new String[] {
             * (String) browser, FIRST_WINDOWS_PARAMETER,
             * SECOND_WINDOWS_PARAMETER, THIRD_WINDOWS_PARAMETER, '"' + url +
             * '"' });
             */
            // This avoids a memory leak on some versions of Java on Windows.
            // That's hinted at in
            // <http://developer.java.sun.com/developer/qow/archive/68/>.
            try {
                process.waitFor();
                process.exitValue();
            } catch (InterruptedException ie) {
                throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
            }
            break;
        case OTHER:
            // Assume that we're on Unix and that Netscape is installed

            // First, attempt to open the URL in a currently running session of
            // Netscape
            if (DEBUG)
                System.out.println("Assuming Netscape Web Browser");
            process = Runtime.getRuntime().exec(
                    new String[] { (String) browser, NETSCAPE_REMOTE_PARAMETER,
                            NETSCAPE_OPEN_PARAMETER_START + url + NETSCAPE_OPEN_PARAMETER_END });
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) { // if Netscape was not open
                    process = Runtime.getRuntime().exec(new String[] { (String) browser, url });
                    exitCode = process.waitFor();
                    if (exitCode != 0) { // if Netscape was not available
                        if (DEBUG)
                            System.out.println("Assuming Mozilla Web Browser");
                        process = Runtime.getRuntime().exec(new String[] { "mozilla", url });
                    }

                }
            } catch (InterruptedException ie) {
                throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
            }
            break;
        case SPECIFIED:
            browser = org.genepattern.util.GPpropertiesManager.getProperty("path.to.browser");
            process = Runtime.getRuntime().exec(new String[] { (String) browser, url });

            try {
                process.waitFor();
                process.exitValue();
            } catch (InterruptedException ie) {
                throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
            }
            break;
        default:
            // This should never occur, but if it does, we'll try the simplest
            // thing possible
            Runtime.getRuntime().exec(new String[] { (String) browser, url });
            break;
        }
    }

    /**
     * Methods required for Mac OS X. The presence of native methods does not
     * cause any problems on other platforms.
     */
    private native static int ICStart(int[] instance, int signature);

    private native static int ICStop(int[] instance);

    private native static int ICLaunchURL(int instance, byte[] hint, byte[] data, int len, int[] selectionStart,
            int[] selectionEnd);
}