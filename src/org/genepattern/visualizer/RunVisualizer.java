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

package org.genepattern.visualizer;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

public class RunVisualizer {

    private boolean debug = false;

    private Map params = null;

    private String[] supportFileNames = null;

    private long[] supportFileDates = null;

    private String cookie;

    private URL documentBase;

    private String server;

    private String contextPath;

    private Applet applet;

    private static final String JAVA = "java";

    private static final String JAVA_FLAGS = "java_flags";

    private static final String ANY = "any";

    private static final String leftDelimiter = "<";

    private static final String rightDelimiter = ">";

    /**
     * @param params
     *            HashMap containing the following key/value pairs:
     *            RunVisualizerConstants.NAME=name of visualizer task
     *            RunVisualizerConstants.COMMAND_LINE=task command line (from
     *            TaskInfoAttributes) RunVisualizerConstants.DEBUG=1 if debug
     *            output to stdout is desired (otherwise omit)
     *            RunVisualizerConstants.OS=operating system choice from
     *            TaskInfoAttributes RunVisualizerConstants.CPU_TYPE=CPU choice
     *            from TaskInfoAttributes
     *            RunVisualizerConstants.LIBDIR=directory on server where the
     *            task lives RunVisualizerConstants.DOWNLOAD_FILES=CSV list of
     *            parameter names which are URLs that need downloading by client
     *            prior to execution RunVisualizerConstants.LSID=LSID of
     *            visualizer task PLUS all of the input parameters that the task
     *            requires (eg. input.filename, out.stub, etc.)
     *
     * @param supportFileNames
     *            array of names (without paths) of required support files for
     *            this task
     * @param supportFileDates
     *            array of lastModified entries (longs) corresponding to each
     *            support file
     *
     */

    public RunVisualizer(Map params, String[] supportFileNames, long[] supportFileDates, Applet applet) {
        this.params = params;
        this.supportFileNames = supportFileNames;
        this.supportFileDates = supportFileDates;
        this.cookie = applet.getParameter("browserCookie");
        this.documentBase = applet.getDocumentBase();
        this.applet = applet;
        this.server = documentBase.getProtocol() + "://" + documentBase.getHost() + ":" + documentBase.getPort();
        this.contextPath = applet.getParameter(RunVisualizerConstants.CONTEXT_PATH);
        if (contextPath == null) {
            contextPath = "/gp";
        }
    }

    public void exec() {
        JDialog dialog = new JDialog();
        JLabel label = new JLabel("Launching visualizer...");
        JProgressBar progressBar = new JProgressBar();
        dialog.setTitle("GenePattern");
        progressBar.setIndeterminate(true);
        label.setFont(new Font("Dialog", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.getContentPane().add(label);
        dialog.getContentPane().add(progressBar, BorderLayout.SOUTH);
        dialog.setResizable(false);
        dialog.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((screenSize.width - dialog.getWidth()) / 2, (screenSize.height - dialog.getHeight()) / 2);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
        _exec();
        dialog.dispose();
    }

    private void _exec() {
        // download all of the files locally, preferably checking against a
        // cache

        File libdirFile = null;
        try {
            libdirFile = downloadSupportFiles();
            if (!libdirFile.exists()) {
                throw new IOException();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, "Unable to download module files to "
                    + System.getProperty("java.io.tmpdir"));
            return;
        }

        String libdir = null;
        try {
            libdir = libdirFile.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            libdir = libdirFile.getPath();
        }

        if (debug) {
            System.out.println("libdir " + libdir);
        }
        // libdir is where all of the support files will be found on the client
        // computer
        params.put(RunVisualizerConstants.LIBDIR, libdir + File.separator);

        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
                + (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        params.put(JAVA, java);
        String javaFlags = (String) params.get(JAVA_FLAGS);
        if (javaFlags == null) {
            javaFlags = "";
        }

        params.put(JAVA_FLAGS, javaFlags);
        params.put("GENEPATTERN_PORT", "" + documentBase.getPort());

        // check OS and CPU restrictions of TaskInfoAttributes against this
        // server
        try {
            validateCPU();
            validateOS();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, e.getMessage());
            return;
        }

        String[] commandLine = null;
        try {
            commandLine = doCommandLineSubstitutions();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, "An error occurred while downloading the input files.");
            return;
        }
        try {
            runCommand(commandLine);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, "An error occurred while trying to run "
                    + params.get(RunVisualizerConstants.NAME) + ".");
        }
    }

    protected String[] doCommandLineSubstitutions() throws IOException {
        // do input argument substitution in command line
        String commandLine = (String) params.get(RunVisualizerConstants.COMMAND_LINE);

        HashMap hmDownloadables = downloadInputURLs();

        StringTokenizer stCmd = new StringTokenizer(commandLine, " ");
        ArrayList cmdArray = new ArrayList(stCmd.countTokens());
        int c = 0;
        while (stCmd.hasMoreTokens()) {
            cmdArray.add(stCmd.nextToken());
        }

        // replace variables in the command line from System.properties and the
        // params HashMap
        for (c = 0; c < cmdArray.size(); c++) {
            String cmd = (String) cmdArray.get(c);
            cmd = variableSubstitution(cmd, hmDownloadables);
            cmdArray.set(c, cmd);
            // if there is nothing in a slot after substitutions, delete the
            // slot entirely
            if (cmd.length() == 0) {
                cmdArray.remove(c);
                c--;
            }
        }
        cmdArray.trimToSize();
        return (String[]) cmdArray.toArray(new String[0]);
    }

    protected String variableSubstitution(String var, HashMap hmDownloadables) {
        int start = 0;
        int end;
        String argValue = null;
        String variableName;
        for (start = var.indexOf(leftDelimiter, 0); start != -1; start = var.indexOf(leftDelimiter, start)) {
            end = var.indexOf(rightDelimiter, start);
            if (end == -1)
                break;
            variableName = var.substring(start + leftDelimiter.length(), end);
            argValue = System.getProperty(variableName);
            if (argValue == null) {
                argValue = (String) params.get(variableName);
            }
            if (argValue != null) {

                if (hmDownloadables != null && hmDownloadables.containsKey(variableName)) {
                    try {
                        argValue = ((File) hmDownloadables.get(variableName)).getCanonicalPath();
                        if (debug)
                            System.out.println("replacing URL " + (String) params.get(variableName) + " with "
                                    + argValue);
                    } catch (IOException ioe) {
                        System.err.println(ioe + " while getting canonical path for "
                                + ((File) hmDownloadables.get(variableName)).getName());
                    }
                }
                var = replace(var, var.substring(start, end + rightDelimiter.length()), argValue);
            } else {
                System.err.println("Unable to find substitution for " + variableName);
                // throw new Exception("Unable to find substitution for " +
                // variableName);
                start = end + rightDelimiter.length();
            }
        }
        return var;
    }

    protected void runCommand(String[] commandLine) throws IOException {
        Process p = null;
        Thread stdoutReader = null;
        Thread stderrReader = null;

        if (debug) {
            System.out.println(Arrays.asList(commandLine));
        }
        p = Runtime.getRuntime().exec(commandLine);

        stdoutReader = copyStream(p.getInputStream(), System.out);
        stderrReader = copyStream(p.getErrorStream(), System.err);

        // drain the output and error streams
        stdoutReader.start();
        stderrReader.start();
    }

    protected File downloadSupportFiles() throws IOException {

        String name = (String) params.get(RunVisualizerConstants.NAME);
        String lsid = (String) params.get(RunVisualizerConstants.LSID);

        // don't even bother using the local files since downloading is so fast
        // and the caching is conservative

        Date startDLTime = new Date();
        File libdir = new File(System.getProperty("java.io.tmpdir"), name + ".libdir");
        if (!libdir.exists()) {
            if (!libdir.mkdirs()) {
                throw new IOException("Unable to create module directory.");
            }
        }

        File[] currentFiles = libdir.listFiles();
        int supf;

        // delete any currently downloaded files that are extraneous or
        // out-of-date (older or newer)
        for (int curf = 0; curf < currentFiles.length; curf++) {
            boolean found = false;
            // if it isn't a support file, delete it
            for (supf = 0; supf < supportFileNames.length; supf++) {
                if (currentFiles[curf].getName().equals(supportFileNames[supf])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // delete extraneous file
                if (debug) {
                    System.out.println("deleting extraneous file " + currentFiles[curf].getCanonicalPath());
                }
                currentFiles[curf].delete();
            } else {
                if (currentFiles[curf].lastModified() != supportFileDates[supf]) {
                    // delete out-of-date file (either more recent or older)
                    if (debug) {
                        System.out.println("deleting out-of-date file " + currentFiles[curf].getCanonicalPath());
                    }
                    currentFiles[curf].delete();
                }
            }
        }

        // figure out which support files are not in the currently downloaded
        // set and download them
        for (supf = 0; supf < supportFileNames.length; supf++) {
            if (!new File(libdir, supportFileNames[supf]).exists()) {
                // need to download it
                if (debug) {
                    System.out.print("downloading missing file " + supportFileNames[supf] + "...");
                }

                URL urlFile = new URL(server + contextPath + "/getFile.jsp?task=" + encode(lsid) + "&file="
                        + encode(supportFileNames[supf]));
                File file = downloadFile(urlFile, libdir, supportFileNames[supf]);
                file.setLastModified(supportFileDates[supf]);
            }
        }
        return libdir;
    }

    /**
     *
     * download a URL to a local file and return a File object for it
     *
     * @param url
     *            The url to download.
     * @param dir
     *            The directory to download the URL to.
     * @param filename
     *            The filename to download the URL to.
     */
    protected File downloadFile(URL url, File dir, String filename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        File file = null;
        GetMethod get = null;
        try {
            if (url.getHost().equals(documentBase.getHost()) && url.getPort() == documentBase.getPort()) {
                HttpClient client = new HttpClient();
                client.setState(new HttpState());
                // client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
                get = new GetMethod(decode(url.toString()));
                if (debug) {
                    System.out.println("Downloading " + get.getURI() + " using HttpClient");
                }

                get.addRequestHeader("Cookie", cookie);
                int statusCode = client.executeMethod(get);
                if (statusCode != HttpStatus.SC_OK) {
                    System.err.println("Method failed: " + get.getStatusLine());
                }
                is = get.getResponseBodyAsStream();
            } else {
                if (debug) {
                    System.out.println("Downloading " + url + " using Java classes");
                }
                URLConnection conn = url.openConnection();
                is = conn.getInputStream();
            }

            dir.mkdirs();
            file = new File(dir, filename);
            fos = new FileOutputStream(file);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {

                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {

                }
            }
            if (get != null) {
                get.releaseConnection();
            }
        }

        return file;
    }

    protected boolean validateCPU() throws IOException {
        String expected = (String) params.get(RunVisualizerConstants.CPU_TYPE);
        String taskName = (String) params.get(RunVisualizerConstants.NAME);
        String actual = System.getProperty("os.arch");
        // eg. "x86", "i386", "ppc", "alpha", "sparc"

        if (expected.equals("")) {
            return true;
        }
        if (expected.equals(ANY)) {
            return true;
        }
        if (expected.equalsIgnoreCase(actual)) {
            return true;
        }

        String intelEnding = "86"; // x86, i386, i586, etc.
        if (expected.endsWith(intelEnding) && actual.endsWith(intelEnding))
            return true;

        throw new IOException("Cannot run on this platform.  " + taskName + " requires a " + expected
                + " CPU, but this is a " + actual);
    }

    protected boolean validateOS() throws IOException {
        String expected = (String) params.get(RunVisualizerConstants.OS);
        String taskName = (String) params.get(RunVisualizerConstants.NAME);
        String actual = System.getProperty("os.name");
        // eg. "Windows XP", "Linux", "Mac OS X", "OSF1"

        if (expected.equals(""))
            return true;
        if (expected.equals(ANY))
            return true;
        if (expected.equalsIgnoreCase(actual))
            return true;

        String MicrosoftBeginning = "Windows"; // Windows XP, Windows ME,
        // Windows XP, Windows 2000, etc.
        if (expected.startsWith(MicrosoftBeginning) && actual.startsWith(MicrosoftBeginning))
            return true;

        throw new IOException("Cannot run on this platform.  " + taskName + " requires " + expected
                + " operating system, but this computer is running " + actual);
    }

    private static String decode(String s) {
        return decode(s, "UTF-8");
    }

    private static String decode(String s, String encoding) {
        try {
            return URLDecoder.decode(s, encoding);
        } catch (NoSuchMethodError e) {
            return URLDecoder.decode(s);
        } catch (java.io.UnsupportedEncodingException x) {
            return s;
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (NoSuchMethodError e) {
            return URLEncoder.encode(s);
        } catch (java.io.UnsupportedEncodingException x) {
            return s;
        }
    }

    private String getURLFileName(URL url) {
        String file = url.getFile();
        int queryIdx = file.lastIndexOf("?");
        if (queryIdx == -1)
            queryIdx = file.length();
        String baseName = file.substring(file.lastIndexOf("/", queryIdx) + 1);
        int j;

        if (file.indexOf("/jobResults") != -1) {
            return baseName;
        }

        if (baseName.indexOf("retrieveResults.jsp") != -1 && (j = file.lastIndexOf("filename=")) != -1) { // for
            // http://18.103.3.29:8080/gp/retrieveResults.jsp?job=1122&filename=all_aml_wv_xval.odf
            String temp = decode(file.substring(j + "filename=".length(), file.length()));
            return new java.util.StringTokenizer(temp, "&").nextToken();
        }

        if (baseName.indexOf("getFile.jsp") != -1 && (j = file.lastIndexOf("file=")) != -1) { // for
            // http://cmec5-ea2.broad.mit.edu:8080/gp/getFile.jsp?task=try.SOMClusterViewer.pipeline&file=ten.res

            String temp = decode(file.substring(j + "file=".length(), file.length()));

            int slashIdx = temp.lastIndexOf("/");
            if (slashIdx == -1) {
                slashIdx = temp.lastIndexOf("\\");
            }
            if (slashIdx >= 0) {
                temp = temp.substring(slashIdx + 1);
            }

            return new java.util.StringTokenizer(temp, "&").nextToken();

        }

        String path = url.getPath();
        path = path.substring(path.lastIndexOf("/") + 1);
        if (path == null || path.equals("")) {
            return "index";
        }
        return path;
    }

    // for input parameters which are URLs, and which are listed in the
    // DOWNLOAD_FILES input parameter, download
    // the URL to a local file and create a mapping between the URL and the
    // local filename for the command
    // line substitution

    protected HashMap downloadInputURLs() throws IOException {
        // create a mapping between downloadable files and their actual
        // (post-download) filenames
        HashMap hmDownloadables = new HashMap();
        StringTokenizer stDownloadables = new StringTokenizer((String) params
                .get(RunVisualizerConstants.DOWNLOAD_FILES), ",");

        String name = (String) params.get(RunVisualizerConstants.NAME);
        String prefix = (name.length() < 3 ? "dl" + name : name);
        File tempdir = null;
        try {
            tempdir = File.createTempFile(prefix, ".tmp");
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(applet, "Unable to create temp directory.");
            throw e1;
        }
        tempdir.delete();
        tempdir.mkdir();
        tempdir.deleteOnExit();

        while (stDownloadables.hasMoreTokens()) {
            String paramName = stDownloadables.nextToken();
            String paramURL = (String) params.get(paramName);
            paramURL = variableSubstitution(paramURL, hmDownloadables);
            if (paramURL.startsWith("getFile.jsp")) {
                paramURL = server + contextPath + "/" + paramURL;
            }
            String filename = null;
            URL url = null;
            try {
                paramURL = paramURL.replace(" ", "%20");
                url = new URL(paramURL);
                filename = getURLFileName(url);
            } catch (MalformedURLException e) {
                System.out.println(paramURL + " is malformed.");
                continue;
            }
            if (debug) {
                System.out.println("downloading URL " + paramURL + " to directory " + tempdir + " as " + filename);
            }
            try {
                File file = downloadFile(url, tempdir, filename);
                file.deleteOnExit();
                hmDownloadables.put(paramName, file);
            } catch (IOException ioe) {
                System.out.println("Error downloading URL " + paramURL);
            }

        }
        return hmDownloadables;
    }

    /**
     * replace all instances of "find" in "original" string and substitute
     * "replace" for them
     *
     * @param original
     *            String before replacements are made
     * @param find
     *            String to search for
     * @param replace
     *            String to replace the sought string with
     * @return String String with all replacements made
     * @author Jim Lerner
     */
    protected static final String replace(String original, String find, String replace) {
        StringBuffer res = new StringBuffer();
        int idx = 0;
        int i = 0;
        while (true) {
            i = idx;
            idx = original.indexOf(find, idx);
            if (idx == -1) {
                res.append(original.substring(i));
                break;
            } else {
                res.append(original.substring(i, idx));
                res.append(replace);
                idx += find.length();
            }
        }
        return res.toString();
    }

    protected Thread copyStream(final InputStream is, final PrintStream out) {
        // create thread to read from the a process' output or error stream
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                // copy inputstream to outputstream

                try {
                    while ((line = in.readLine()) != null) {
                        out.println(line);
                    }
                } catch (IOException ioe) {
                    System.err.println(ioe + " while reading from process stream");
                }
            }
        });
        copyThread.setDaemon(true);
        return copyThread;
    }
}