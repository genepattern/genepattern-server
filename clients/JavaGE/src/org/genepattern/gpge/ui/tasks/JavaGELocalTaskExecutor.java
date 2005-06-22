package org.genepattern.gpge.ui.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;

import org.genepattern.webservice.LocalTaskExecutor;
import org.genepattern.webservice.TaskExecException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 * @created May 18, 2004
 */
public class JavaGELocalTaskExecutor extends LocalTaskExecutor {
	private StreamGobbler errorGobbler;
	private String taskName;

	public JavaGELocalTaskExecutor(
			TaskInfo taskInfo, Map paramName2ValueMap, String userName,
			String server) throws java.io.IOException,
			WebServiceException {
		super(taskInfo, paramName2ValueMap, userName, server);
		this.taskName = taskInfo.getName();
	}

	public static Properties loadGPProperties() {
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			File propsFile = new File(System.getProperty("user.home")
					+ File.separator + "gp" + File.separator + "gp.properties");
			fis = new FileInputStream(propsFile);
			props.load(fis);
			fis.close();
		} catch (IOException ioe) {
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException x) {
				}
			}
		}
		return props;
	}

	protected void startOutputStreamThread(Process proc) {
      try {
			new StreamGobbler(proc.getInputStream(), System.out).start();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	protected void startErrorStreamThread(Process proc) {
	try {
			errorGobbler = new StreamGobbler(proc.getErrorStream(), System.err);
			errorGobbler.start();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void exec() throws TaskExecException {
		try {
			super.exec();
		} catch (Exception e) {
			String message = "An error occurred while attempting to run "
					+ taskName;
			if (e.getCause() instanceof org.apache.axis.AxisFault) {
				org.apache.axis.AxisFault fault = (org.apache.axis.AxisFault) e
						.getCause();
				javax.xml.namespace.QName noService = new javax.xml.namespace.QName(
						"http://xml.apache.org/axis/", "Server.NoService");
				if (fault.getFaultCode().equals(noService)) {
					message += "\nCause: Cannot run visualizers when connecting to an old server.";
				}
			} 
			org.genepattern.gpge.GenePattern.showErrorDialog(message);
		}
	}

	/**
	 * @author Joshua Gould
	 * @created May 18, 2004
	 */
	static class StreamGobbler extends Thread {
		private final InputStream is;   
		private PrintStream ps;
		
	StreamGobbler(final InputStream is, PrintStream ps)
				throws IOException {
			this.is = is;
			this.ps = ps;
		}

		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				for (String line = br.readLine(); line != null; line = br
						.readLine()) {
				    ps.println(line);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	} // end StreamGobbler

}