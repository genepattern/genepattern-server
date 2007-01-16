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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * RunR is a simple interface that translates a command line into something that
 * the R interpreter can handle. The goal is to start R running, have it read in
 * a script, and then begin execution of a particular method in the script with
 * specified arguments. R is not natively capable of doing this. So the RunR
 * class invokes R, then feeds it a series of commands via the standard input
 * stream and copies the results to its own stdout and stderr output streams.
 * 
 * @author Jim Lerner
 */

public class RunR extends Thread {

    static final int R_SOURCE = 0;

    static final int R_METHOD = 1;

    static final int R_ARGS = 2;

    static final int MIN_ARGS = R_ARGS;

    OutputStream stdin = null;

    InputStream is = null;

    PrintStream os = null;

    boolean DEBUG = (System.getProperty("DEBUG") != null);

    /**
     * Invoke the R interpreter, create a few lines of input to feed to the
     * stdin input stream of R, and spawn two threads that copy stdout and
     * stderr from R to this process' version of the same.
     * 
     * @param args
     *            Command line parameters. <br />
     *            args[0] R script file to source <br />
     *            args[1] method to invoke (usually defined within script file
     *            or referring to something within it) <br />
     *            args[2] beginning of zero or more arguments to the method
     *            being invoked
     * 
     * @author Jim Lerner
     */
    public static void main(String[] args) {
        if (args.length < MIN_ARGS) {
            System.err
                    .println("Insufficient arguments to RunR.  Must have source filename, method name, and optional args.");
            System.exit(0);
        }

        new RunR(args);
    }

    public RunR(String[] args) {
        String[] commandLine = null;
        boolean bWindows = System.getProperty("os.name").startsWith("Windows");
        /*
         * find R_HOME:
         * 
         * 1. java -DR_HOME=path_to_R_home 2. environment variable 3. assume it
         * is in the path
         * 
         */
        String R_HOME = System.getProperty("R_HOME");
        if (R_HOME == null) {
            Hashtable htEnv = getEnv();
            R_HOME = (String) htEnv.get("R_HOME");
        }

        String[] rFlags = null;
        String rFlagsProp = System.getProperty("r_flags");

        if (rFlagsProp != null && !rFlagsProp.equals("")) {
            rFlags = System.getProperty("r_flags").split(" ");
        } else {
            rFlags = new String[0];
        }
        List<String> commandLineList = null;
        if (bWindows) {
            if (R_HOME == null) { // assume Rterm is in path
                commandLineList = new ArrayList<String>(Arrays.asList(new String[] { "cmd", "/c", "Rterm" }));
            } else {
                commandLineList = new ArrayList<String>(Arrays.asList(new String[] { "cmd", "/c",
                        R_HOME + "\\bin\\Rterm" }));

            }
        } else {
            if (R_HOME == null) { // assume R is in path
                commandLineList = new ArrayList<String>(Arrays.asList(new String[] { "R" }));
            } else {
                commandLineList = new ArrayList<String>(Arrays.asList(new String[] { R_HOME + "/bin/R" }));
            }
        }
        commandLineList.addAll(Arrays.asList(rFlags));
        commandLine = commandLineList.toArray(new String[0]);
       
        try {

            final Process process = Runtime.getRuntime().exec(commandLine, null, null);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    process.destroy();
                }
            });

            if (DEBUG) {
                System.setOut(new PrintStream(new FileOutputStream("r_log.html")));
            }
            // create threads to read from the command's stdout and stderr
            // streams
            Thread outputReader = streamCopier(process.getInputStream(), System.out);
            Thread errorReader = streamCopier(process.getErrorStream(), System.err);

            // drain the output and error streams
            outputReader.start();
            errorReader.start();

            stdin = process.getOutputStream();
            if (DEBUG) {
                System.out.println("<html><head><title>" + (args[R_METHOD] != null ? args[R_METHOD] : args[R_SOURCE])
                        + "</title>");
                System.out.println("<script language=\"Javascript\">");
                System.out.println("function checkAll(frm, bChecked) {");
                System.out.println("\tfrm = document.forms[\"results\"];");
                System.out.println("\tfor (i = 0; i < frm.elements.length; i++) {");
                System.out.println("\t\tif (frm.elements[i].type != \"checkbox\") continue;");
                System.out.println("\t\tfrm.elements[i].checked = bChecked;");
                System.out.println("\t}");
                System.out.println("}");
                System.out.println("</script>");
                System.out.println("</head>\n<body>\n<pre>");
            }

            if (args[R_SOURCE].startsWith("http:") || args[R_SOURCE].startsWith("https:")) {
                args[R_SOURCE] = "url(\"" + args[R_SOURCE] + "\")";
            } else {
                args[R_SOURCE] = "\"" + fixPath(args[R_SOURCE]) + "\"";
            }
            sendCmd("source(" + args[R_SOURCE] + ")\n");

            sendCmd("files <- " + args[R_METHOD]);
            sendCmd("(");
            boolean hasQuotes = false;
            for (int i = R_ARGS; i < args.length; i++) {
                if (i > R_ARGS) {
                    sendCmd(", ");
                }
                hasQuotes = (args[i].indexOf("\"") != -1);
                sendCmd((!hasQuotes ? "\"" : "") + fixPath(args[i]) + (!hasQuotes ? "\"" : ""));
            }
            sendCmd(")\n");

            sendCmd("q(save=\"no\")\n");
            if (DEBUG) {
                System.out.println("</pre>");
            }
            stdin.close();

            // wait for all output before attempting to send it back to the
            // client
            outputReader.join();
            errorReader.join();

            // the process will be dead by now
            process.waitFor();
        } catch (IOException ioe) {
            System.err.println(ioe + " while running R command " + Arrays.asList(commandLine));
        } catch (InterruptedException ie) {
            System.err.println(ie + " while running R command " + Arrays.asList(commandLine));
        }
    }

    /**
     * write a string to stdin of the R process
     * 
     * @param command
     *            string to send to R
     * @author Jim Lerner
     * 
     */
    protected void sendCmd(String command) throws IOException {
        if (DEBUG)
            System.out.print(command);
        stdin.write(command.getBytes());
    }

    /**
     * convert Windows path separators to Unix, which R prefers!
     * 
     * @param path
     *            path to convert to Unix format
     * @return String path with delimiters replaced
     * @author Jim Lerner
     * 
     */
    protected String fixPath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * copies one of the output streams from R to this process' output stream
     * 
     * @param is
     *            InputStream to read from (from R)
     * @param os
     *            PrintStream to write to (stdout of this process)
     * @author Jim Lerner
     * 
     */
    protected Thread streamCopier(InputStream is, PrintStream os) throws IOException {
        return new RunR(is, os);
    }

    // launch a new thread (an instance of this) that will copy stdout or stderr
    // to a PrintStream
    public RunR(InputStream is, PrintStream os) {
        this.is = is;
        this.os = os;
        this.setDaemon(true);
    }

    // Runnable for streamCopier thread
    public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = in.readLine()) != null) {

                if (line.startsWith("> ") || line.startsWith("+ "))
                    continue;
                boolean bNeedsBreak = (line.length() > 0 && (line.indexOf("<") == -1 || line.indexOf("<-") != -1));
                os.print(line);
                if (bNeedsBreak) {
                    if (DEBUG) {
                        os.println("<br>");
                    }
                }
                os.flush(); // show it to the user ASAP
            }

        } catch (IOException ioe) {
            System.err.println(ioe + " while reading from process stream");
        }
    }

    /**
     * Here's a tricky/nasty way of getting the environment variables despite
     * System.getenv() being deprecated. TODO: find a better (no-deprecated)
     * method of retrieving environment variables in platform-independent
     * fashion. The environment is used <b>almost </b> as is, except that the
     * directory of the task's files is added to the path to make execution work
     * transparently. This is equivalent to the <libdir>substitution variable.
     * Some of the applications will be expecting to find their support files on
     * the path or in the same directory, and this manipulation makes it
     * transparent to them.
     * 
     * <p>
     * Implementation: spawn a process that performs either a "sh -c set" (on
     * Unix) or "cmd /c set" on Windows.
     * 
     * @author Jim Lerner
     * @return Hashtable of environment variable name/value pairs
     * 
     */
    public Hashtable getEnv() {
        Hashtable<String, String> envVariables = new Hashtable<String, String>();
        int i;
        String key;
        String value;
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        BufferedReader in = null;

        try {
            Process getenv = Runtime.getRuntime().exec(isWindows ? "cmd /c set" : "sh -c set");
            in = new BufferedReader(new InputStreamReader(getenv.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                i = line.indexOf("=");
                if (i == -1) {
                    continue;
                }
                key = line.substring(0, i);
                value = line.substring(i + 1);
                envVariables.put(key, value);
            }
        } catch (IOException ioe) {
            System.err.println(ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
        return envVariables;
    }
}