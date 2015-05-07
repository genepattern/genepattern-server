/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RunR is a simple interface that translates a command line into something that the R interpreter can handle. The goal
 * is to start R running, have it read in a script, and then begin execution of a particular method in the script with
 * specified arguments. R is not natively capable of doing this. So the RunR class invokes R, then feeds it a series of
 * commands via the standard input stream and copies the results to its own stdout and stderr output streams.
 * 
 * @author Jim Lerner
 * @author Joshua Gould
 */

public class RunR {

    private static final int R_SOURCE = 0;

    private static final int R_METHOD = 1;

    private static final int R_ARGS = 2;

    private static final int MIN_ARGS = R_ARGS;

    private OutputStream stdin = null;

    private Process process;

    /**
     * Invoke the R interpreter, create a few lines of input to feed to the stdin input stream of R, and spawn two
     * threads that copy stdout and stderr from R to this process' version of the same.
     * 
     * @param args
     *                Command line parameters. <br />
     *                args[0] R script file to source <br />
     *                args[1] R method to invoke (usually defined within script file or referring to something within
     *                it) <br />
     *                args[2] beginning of zero or more arguments to the method being invoked <br />
     *                System property R_HOME also needs to point to R_HOME directory
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

	String R_HOME = System.getProperty("R_HOME");
	if (R_HOME == null || R_HOME.equals("")) {
	    System.err.println("R_HOME is not set");
	    System.exit(0);
	}

	String[] rFlags = null;
	String rFlagsProp = System.getProperty("r_flags");

	String rSuppressTxtFile = System.getProperty("R_suppress");
	List<String> rSuppressLines = new ArrayList<String>();
	if (rSuppressTxtFile != null && new File(rSuppressTxtFile).exists()) {
	    BufferedReader br = null;
	    try {
		br = new BufferedReader(new FileReader(rSuppressTxtFile));
		String s;
		while ((s = br.readLine()) != null) {
		    if (!s.equals("")) {
			rSuppressLines.add(s);
		    }
		}
	    } catch (IOException e) {
		System.err.println("Error reading file " + rSuppressTxtFile + ".");
	    } finally {
		if (br != null) {
		    try {
			br.close();
		    } catch (IOException e) {
		    }
		}
	    }
	}

	if (rFlagsProp != null && !rFlagsProp.equals("")) {
	    rFlags = System.getProperty("r_flags").split(" ");
	} else {
	    rFlags = new String[0];
	}
	List<String> commandLineList = new ArrayList<String>(Arrays.asList(new String[] { R_HOME + "/bin/R" }));
	commandLineList.addAll(Arrays.asList(rFlags));
	commandLine = commandLineList.toArray(new String[0]);

	try {
	    Runtime.getRuntime().addShutdownHook(new Thread() {
		@Override
		public void run() {
		    if (process != null) {
			process.destroy();
		    }
		}
	    });
	    process = Runtime.getRuntime().exec(commandLine, null, null);

	    // create threads to read from the command's stdout and stderr streams
	    Thread outputReader = new StreamCopier(process.getInputStream(), System.out, rSuppressLines);
	    Thread errorReader = new StreamCopier(process.getErrorStream(), System.err, rSuppressLines);

	    // drain the output and error streams
	    outputReader.start();
	    errorReader.start();
	    stdin = process.getOutputStream();

	    if (args[R_SOURCE].startsWith("http:") || args[R_SOURCE].startsWith("https:")) {
		args[R_SOURCE] = "url(\"" + args[R_SOURCE] + "\")";
	    } else {
		args[R_SOURCE] = "\"" + fixPath(args[R_SOURCE]) + "\"";
	    }
	    sendCmd("source(" + args[R_SOURCE] + ")\n");
	    sendCmd("result <- " + args[R_METHOD]);
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
	    stdin.close();

	    // wait for all output before attempting to send it back to the client
	    outputReader.join();
	    errorReader.join();
	    // the process will be dead by now
	    process.waitFor();
	} catch (Exception e) {
	    System.err.println(e + " while running R command " + Arrays.asList(commandLine));
	    if (process != null) {
		process.destroy();
	    }
	}
    }

    /**
     * Writes a string to stdin of the R process
     * 
     * @param command
     *                string to send to R
     * 
     * 
     */
    protected void sendCmd(String command) throws IOException {
	stdin.write(command.getBytes());
    }

    /**
     * convert Windows path separators to Unix, which R prefers!
     * 
     * @param path
     *                path to convert to Unix format
     * @return String path with delimiters replaced
     * 
     * 
     */
    protected String fixPath(String path) {
	return path.replace('\\', '/');
    }

    static class StreamCopier extends Thread {
	private InputStream is;
	private PrintStream os;
	private List<String> linesToIgnore;

	/**
	 * Creates a new thread that copies stdout or stderr to a PrintStream.
	 */
	public StreamCopier(InputStream is, PrintStream os, List<String> linesToIgnore) {
	    this.is = is;
	    this.os = os;
	    this.setDaemon(true);
	    this.linesToIgnore = linesToIgnore;

	}

	@Override
	public void run() {
	    BufferedReader in = new BufferedReader(new InputStreamReader(is));
	    String line;
	    try {
		while ((line = in.readLine()) != null) {
		    boolean skip = false;
		    for (String ignore : linesToIgnore) {
			if (line.startsWith(ignore)) {
			    skip = true;
			    break;
			}
		    }
		    if (!skip) {
			os.println(line);
		    }

		}

	    } catch (IOException ioe) {
		System.err.println(ioe + " while reading from process stream.");
	    }
	}
    }
}
