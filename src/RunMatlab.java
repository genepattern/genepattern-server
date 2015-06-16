/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
/*
 * Created on Aug 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.io.*;

/**
 * @author genepattern
 * 
 * Runs a MATLAB function that exists in a directory passed in in the first
 * command line argument, with the remaining command-line args being passed to
 * the MATLAB function as String arguments.
 * 
 * Assumes that MATLAB is on the path.
 * 
 * operates by creating a startup.m file in the local working directory. MATLAB
 * reads this by default on startup. The startup.m it creates adds the libdir to
 * the path, calls the function (with args) and then quits matlab.
 * 
 * e.g. java RunMatlab c:\mystuff fooFunction one two
 * 
 * will write a startup.m that looks like this
 * 
 * addpath c:\mystuff fooFunction('one','two') quit()
 * 
 * and then exec matlab -nosplash -nodisplay
 */
public class RunMatlab {
// make a change
	public static void main(String[] args) throws Exception {
		createStartupFile(args);

		String[] cmdArray = new String[3];
		cmdArray[0] = "matlab";
		cmdArray[1] = "-nosplash";
		cmdArray[2] = "-nodisplay";

		Process p = Runtime.getRuntime().exec(cmdArray);

		InputStream outs = p.getInputStream();

		Thread stdoutReader = copyStream(p.getInputStream(), System.out);
		Thread stderrReader = copyStream(p.getErrorStream(), System.err);

		// drain the output and error streams
		stdoutReader.start();
		stderrReader.start();

		p.waitFor();
	}

	protected static void createStartupFile(String[] args) {
		try {
			File startupM = new File("startup.m");
			BufferedWriter bw = new BufferedWriter(new FileWriter(startupM));
			bw.write("addpath " + args[0]);
			bw.write("\n");
			bw.write(args[1]);
			bw.write("(");
			for (int i = 2; i < args.length; i++) {
				bw.write("'");
				bw.write(args[i]);
				bw.write("'");
				if ((i != (args.length - 1)) & (i != 0))
					bw.write(", ");
			}
			bw.write(")");
			bw.write("\n");
			bw.write("quit();");
			bw.flush();
			bw.close();
			startupM.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static Thread copyStream(final InputStream is,
			final PrintStream out) throws IOException {
		// create thread to read from the a process' output or error stream
		Thread copyThread = new Thread(new Runnable() {
			public void run() {
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
				String line;
				// copy inputstream to outputstream

				try {
					boolean bNeedsBreak;
					while ((line = in.readLine()) != null) {
						out.println(line);
					}
				} catch (IOException ioe) {
					System.err.println(ioe
							+ " while reading from process stream");
				}
			}
		});
		copyThread.setDaemon(true);
		return copyThread;
	}
}
