/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import org.genepattern.server.config.GpConfig;

public class RunVisualizer {
    protected static final String JAVA = "java";
    protected static final String JAVA_FLAGS = "java_flags";
    protected static final String ANY = "any";
    protected static final String leftDelimiter = "<";
    protected static final String rightDelimiter = ">";

    protected boolean debug = true;

    /** Applet parameters */
    protected Map params = null;
    /** array of module support files */
    protected String[] supportFileNames = null;
    /** array of last modified times for module support files */
    protected long[] supportFileDates = null;
    /** cookie to authenticate with GP server */
    protected String cookie;
    protected URL documentBase;
    protected String server;
    protected String contextPath;
    protected Applet applet;

    /** files to delete on process exit */
    protected List<File> filesToDelete = new ArrayList<File>();
    /** directory for downloaded input files */
    protected File tempdir;

    /**
     * @param params HashMap containing the following key/value pairs: 
     *   RunVisualizerConstants.NAME=name of visualizer task 
     *   RunVisualizerConstants.COMMAND_LINE=task command line (from TaskInfoAttributes)
     *   RunVisualizerConstants.DEBUG=1 if debug output to stdout is desired (otherwise omit)
     *   RunVisualizerConstants.OS=operating system choice from TaskInfoAttributes
     *   RunVisualizerConstants.CPU_TYPE=CPU choice from TaskInfoAttributes
     *   RunVisualizerConstants.LIBDIR=directory on server where the task lives
     *   RunVisualizerConstants.DOWNLOAD_FILES=CSV list of parameter names which are URLs that need downloading by client prior to execution 
     *   RunVisualizerConstants.LSID=LSID of visualizer task 
     *   
     *   PLUS all of the input parameters that the task requires (eg. input.filename, out.stub, etc.)
     * 
     * @param supportFileNames
     *                array of names (without paths) of required support files for this task
     * @param supportFileDates
     *                array of lastModified entries corresponding to each support file
     */
    public RunVisualizer(Map params, String[] supportFileNames, long[] supportFileDates, Applet applet) {
        this.params = params;
        this.supportFileNames = supportFileNames;
        this.supportFileDates = supportFileDates;
        this.cookie = applet.getParameter("browserCookie");
        this.documentBase = applet.getDocumentBase();
        this.applet = applet;
        
        String portStr = "";
        int port = documentBase.getPort();
        if (port>0) {
            portStr = ":"+port;
        }
        this.server = documentBase.getProtocol() + "://" + documentBase.getHost() + portStr;
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
        File libdir = null;
        LocalModuleDirectoryManager directoryManager = new LocalModuleDirectoryManager((String) params
                .get(RunVisualizerConstants.LSID), (String) params.get(RunVisualizerConstants.NAME));
        synchronized (getClass()) {
            try {
                directoryManager.init();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(applet, "An error occurred while downloading the module support files: "+e.getLocalizedMessage());
                return;
            }
            // libdir is where all of the support files are found on the client computer
            libdir = directoryManager.getDirectory();
            try {
                downloadSupportFiles(libdir);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(applet, "An error occurred while downloading the module support files: "+e.getLocalizedMessage());
                return;
            }
        }
        String libdirPath;
        try {
            libdirPath = libdir.getCanonicalPath();
        } catch (IOException e1) {
            libdirPath = libdir.getPath();
        }
        params.put(RunVisualizerConstants.LIBDIR, libdirPath + File.separator);

        final String java = GpConfig.getJavaProperty("java.home") + File.separator + 
            "bin" + File.separator + "java" + 
            (GpConfig.getJavaProperty("os.name").startsWith("Windows") ? ".exe" : "");
        params.put(JAVA, java);
        String javaFlags = (String) params.get(JAVA_FLAGS);
        if (javaFlags == null) {
            javaFlags = "";
        }
        params.put(JAVA_FLAGS, javaFlags);

        String portStr = "";
        int port = documentBase.getPort();
        if (port>0) {
            portStr = ""+port;
        }
        params.put("GENEPATTERN_PORT", portStr);

        // check OS and CPU restrictions of TaskInfoAttributes against this server
        try {
            validateCPU();
            validateOS();
        } 
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, e.getMessage());
            return;
        }

        try {
            String[] commandLine = doCommandLineSubstitutions();
            runCommand(commandLine);
        } 
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(applet, "An error occurred while running the visualizer: "+e.getLocalizedMessage());
            return;
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
                        if (debug) {
                            System.out.println("replacing URL " + (String) params.get(variableName)
                            + " with arg value: " + argValue);
                        }
                    } catch (IOException ioe) {
                        System.err.println(ioe + " while getting canonical path for "
                                + ((File) hmDownloadables.get(variableName)).getName());
                    }
                }
                var = replace(var, var.substring(start, end + rightDelimiter.length()), argValue);
            } else {
                System.err.println("Unable to find substitution for " + variableName);
                start = end + rightDelimiter.length();
            }
        }
        return var;
    }

    private void cleanup() {
        for (int i = 0; i < filesToDelete.size(); i++) {
            File f = (File) filesToDelete.get(i);
            f.delete();
        }
        tempdir.delete();
    }

    protected void runCommand(final String[] commandLine) {
        if (debug) {
            System.out.println(Arrays.asList(commandLine));
        }
        Thread t = new Thread() {
            public void run() {
                Process p = null;
                try {
                    p = Runtime.getRuntime().exec(commandLine);
                } catch (IOException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(applet, "An error occurred while running the visualizer: "+e1.getLocalizedMessage());
                    return;
                }
                // drain the output and error streams
                copyStream(p.getInputStream(), System.out);
                copyStream(p.getErrorStream(), System.err);

                try {
                    p.waitFor();
                } catch (InterruptedException e) {

                }
                cleanup();
            }
        };
        t.start();

    }

    private synchronized void downloadSupportFiles(File libdir) throws IOException {
        String lsid = (String) params.get(RunVisualizerConstants.LSID);
        if (supportFileNames != null) {
            for (int i = 0, length = supportFileNames.length; i < length; i++) {
                String filename = supportFileNames[i];
                boolean download = !new File(libdir, filename).exists()
                        || new Date(supportFileDates[i]).after(new Date(new File(libdir, filename).lastModified()));
                if (download) {
                    if (new File(libdir, filename).exists()) {
                        new File(libdir, filename).delete();
                    }
                    URL url = new URL(server + contextPath + "/getFile.jsp?task=" + encode(lsid) + "&file="
                            + encode(filename));
                    if (debug) {
                        System.out.println("downloading URL " + url + " to directory " + libdir + " as " + filename);
                    }
                    File f = downloadFile(url, libdir, filename);
                    f.setLastModified(supportFileDates[i]);
                }
            }
        }
    }

    /**
     * Download a URL to a local file and return a File object for it.
     * 
     * @param url, The url to download.
     * @param dir, The directory to download the URL to.
     * @param filename, The filename to download the URL to.
     */
    protected File downloadFile(URL url, File dir, String filename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            dir.mkdirs();
            file = new File(dir, filename);
            fos = new FileOutputStream(file);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } 
                catch (IOException e) {
                }
            }
        }
        return file;
    }

    protected boolean validateCPU() throws IOException {
        String expected = (String) params.get(RunVisualizerConstants.CPU_TYPE);
        String taskName = (String) params.get(RunVisualizerConstants.NAME);
        final String actual = GpConfig.getJavaProperty("os.arch");
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
        String actual = GpConfig.getJavaProperty("os.name");
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
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (NoSuchMethodError e) {
            return URLDecoder.decode(s);
        } catch (UnsupportedEncodingException x) {
            return s;
        }

    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (NoSuchMethodError e) {
            return URLEncoder.encode(s);
        } catch (UnsupportedEncodingException x) {
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
            // http://servername:8080/gp/retrieveResults.jsp?job=1122&filename=all_aml_wv_xval.odf
            String temp = decode(file.substring(j + "filename=".length(), file.length()));
            return new java.util.StringTokenizer(temp, "&").nextToken();
        }

        if (baseName.indexOf("getFile.jsp") != -1 && (j = file.lastIndexOf("file=")) != -1) { // for
            // http://servername:8080/gp/getFile.jsp?task=try.SOMClusterViewer.pipeline&file=ten.res

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

    /**
     * for input parameters which are URLs, and which are listed in the DOWNLOAD_FILES input parameter, download the URL
     * to a local file and create a mapping between the URL and the local filename for the command line substitution
     */
    protected HashMap downloadInputURLs() throws IOException {
        // create a mapping between downloadable files and their actual
        // (post-download) filenames
        HashMap hmDownloadables = new HashMap();
        StringTokenizer stDownloadables = new StringTokenizer((String) params
                .get(RunVisualizerConstants.DOWNLOAD_FILES), ",");

        String name = (String) params.get(RunVisualizerConstants.NAME);
        String prefix = (name.length() < 3 ? "dl" + name : name);
        this.tempdir = null;
        try {
            tempdir = File.createTempFile(prefix, ".tmp");
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(applet, "Unable to create temp directory.");
            throw e1;
        }
        tempdir.delete();
        tempdir.mkdir();

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
                filesToDelete.add(file);
                hmDownloadables.put(paramName, file);
            } catch (IOException ioe) {
                System.out.println("Error downloading URL " + paramURL);
            }

        }
        return hmDownloadables;
    }

    /**
     * replace all instances of "find" in "original" string and substitute "replace" for them
     * 
     * @param original
     *                String before replacements are made
     * @param find
     *                String to search for
     * @param replace
     *                String to replace the sought string with
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
        // create thread to read from the a process output or error stream
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        out.println(line);
                    }
                } catch (IOException ioe) {
                    System.err.println("Error reading from process stream.");
                }
            }
        });
        copyThread.setDaemon(true);
        copyThread.start();
        return copyThread;
    }
}
