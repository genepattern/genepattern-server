
package edu.mit.broad.gp.core;


import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.LocalTaskExecutor;
import org.genepattern.webservice.RunTaskException;
import org.genepattern.io.StorageUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 *@author     Joshua Gould
 *@created    May 18, 2004
 */
public class EclipseLocalTaskExecutor extends LocalTaskExecutor {
	private StreamGobbler errorGobbler;
	private StreamGobbler outGobbler;
	private Shell parent;


	public EclipseLocalTaskExecutor(Shell parent, TaskInfo taskInfo, Map paramName2ValueMap, String username, String gpURL) throws java.io.IOException, java.net.MalformedURLException {
		super(taskInfo, paramName2ValueMap, username, gpURL);
		this.parent = parent;
	}


	public static Properties loadGPProperties() {
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			File propsFile = new File(System.getProperty("user.home") + File.separator + "gp" + File.separator + "gp.properties");
			fis = new FileInputStream(propsFile);
			props.load(fis);
			fis.close();
		} catch(IOException ioe) {
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch(IOException x){}
			}
		}
		return props;
	}


	protected void startOutputStreamThread(Process proc) {
		try {
			outGobbler = new StreamGobbler(proc.getInputStream(), taskId + "-OUT");
			outGobbler.start();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}


	protected void startErrorStreamThread(Process proc) {
		try {
			errorGobbler = new StreamGobbler(proc.getErrorStream(), taskId + "-ERR");
			errorGobbler.start();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}


	public void exec() throws RunTaskException {
		new Thread() {
			public void run()  {
			    try {
				_exec();
			    } catch(Exception e) {
			        e.printStackTrace();
			    }
			}
		}.start();
	}
	
	private void _exec() throws Exception {
		try {
			super.exec();	
			errorGobbler.join();
			outGobbler.join();
			if(errorGobbler != null && errorGobbler.tmp_file != null && errorGobbler.tmp_file.length() > 0L) {
				try {
					final String error_text = StorageUtils.createStringFromContents(errorGobbler.tmp_file);
					String message = taskId + " error:\n" + error_text;
					showErrorDialog(message);
				}catch(IOException ioe) {
					    ioe.printStackTrace();	
				
				}
			}
		} catch(Exception e) {
		    e.printStackTrace();
			String message = "An error occurred while attempting to run " + taskId;
			if(e.getMessage() != null) {
				message += "\nCause: " + e.getMessage();
				showErrorDialog(message);
			}
			
		}
	}
	
	void showErrorDialog(final String message) {
	    Display d  = Display.getDefault();
		d.asyncExec(new Runnable() {
		    public void run() {
		        MessageDialog.openError(parent, "Error", message);
		    }
		});
	}



	/**
	 *@author     Joshua Gould
	 *@created    May 18, 2004
	 */
	static class StreamGobbler extends Thread {
		private final InputStream is;
		public final File tmp_file;


		StreamGobbler(final InputStream is, final String type) throws IOException {
			this.is = is;
			this.tmp_file = File.createTempFile(type, ".txt");
		}


		public void run() {
			try {
				final PrintWriter writer = new PrintWriter(new FileWriter(tmp_file));
				final InputStreamReader isr = new InputStreamReader(is);
				final BufferedReader br = new BufferedReader(isr);
				for(String line = br.readLine(); line != null; line = br.readLine()) {
					writer.println(line);
				}
				writer.close();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
	} 

}

