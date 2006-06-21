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


package edu.mit.genome.gp;

import junit.framework.*;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.FileInputStream;

import edu.mit.wi.omnigene.framework.analysis.*;
import edu.mit.wi.omnigene.util.*;
import edu.mit.wi.omniview.analysis.*;
import edu.mit.wi.omnigene.framework.analysis.webservice.client.*;
/**
 *@author     jgould
 *@created    March 5, 2004
 */
public class TestCallTasksFromProperties extends TestCallTasks {

	final static String TASK_NAME_KEY = "gp.task.name";
	final static String DIFF_KEY = "gp.diff";
	String taskName;
	ArrayList params = new ArrayList();
	AnalysisJob job;
	Map outputFileNameToKnownGoodOutputMap = new HashMap();

	/**  whether to delete the output files produced by the job */
	boolean deleteOuputFiles = true;
	String propertyFile;


	public TestCallTasksFromProperties(String name) {
		super(name);
	}


	public static Test suite() {
		TestSuite suite = new TestSuite();
		String baseDir = "/Users/jgould/tests";
		File file = new File(baseDir);
		List q = new LinkedList();
		q.add(file);
		while(!q.isEmpty()) {
			File directory = (File) q.remove(0);
			File[] files = directory.listFiles(new java.io.FileFilter() {
				public boolean accept(File file) {
					return file.isDirectory() || file.getName().endsWith(".props");	
				}
			});
			for(int i = 0, length = files.length; i < length; i++) {
				if(files[i].isDirectory()) {
					q.add(files[i]);
				} else { 
					TestCallTasksFromProperties t = new TestCallTasksFromProperties("testFromProperties");
					try {
						t.propertyFile = files[i].getCanonicalPath();
						//System.out.println("Property file " + t.propertyFile);
					} catch(IOException ioe) {
						ioe.printStackTrace();
					}
					suite.addTest(t);
				}
			}
		}

		return suite;
	}


	/**
	 *  Submits a job and waits for its completion. After completion performs a
	 *  diff between the output files produces by the job and a known good output
	 *  file if specified in the properties file.
	 *
	 *@exception  Exception  If an error occurs
	 */
	protected void submit() throws Exception {
		AnalysisService svc = (AnalysisService) serviceMap.get(taskName);
		// call and wait for completion or error
		job = submitJob(svc, (ParameterInfo[]) params.toArray(new ParameterInfo[params.size()]));
		JobInfo jobInfo = waitForErrorOrCompletion(job);

		// look for successful completion (not an error)
		assertTrue("Finished".equalsIgnoreCase(jobInfo.getStatus()));
		assertNoStddErrFileGenerated(job);
		diff();
	}


	public void testFromProperties() throws Exception {
		_testFromProperties(propertyFile);
	}


	private void _testFromProperties(String propertyFile) throws Exception {
		Properties props = new Properties();
		FileInputStream fis = new FileInputStream(propertyFile);
		props.load(fis);

		for(Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
			String key = (String) e.nextElement();
			if(key.startsWith(DIFF_KEY)) {
				StringTokenizer st = new StringTokenizer(props.getProperty(key), ";");
				String outputFile = st.nextToken();
				String knownGoodFile = st.nextToken();
				outputFileNameToKnownGoodOutputMap.put(outputFile, knownGoodFile);
			} else if(key.equals(TASK_NAME_KEY)) {
				taskName = props.getProperty(key);
			} else {
				StringTokenizer st = new StringTokenizer(props.getProperty(key), ";");
				int count = st.countTokens();
				if(count == 2) {
					String fileName = st.nextToken().trim();
					String type = st.nextToken().trim();
					if(!type.equals("file")) {
						throw new IllegalArgumentException("Unknown type for key " + key);
					}
					ParameterInfo infile = new ParameterInfo(key, getDataFilePath(fileName), ParameterInfo.FILE_TYPE);
					infile.setAsInputFile();
					params.add(infile);
				} else if(count == 1) {
					params.add(new ParameterInfo(key, st.nextToken().trim(), ""));
				} else {
					throw new IllegalArgumentException("More tokens than expected for key " + key);
				}

			}
		}

		fis.close();
		submit();
	}



	/**
	 *  Performs a 'diff' between the known good output files and the outputs
	 *  produced by the run of this task
	 *
	 *@exception  Exception  If an error occurs.
	 */
	protected void diff() throws Exception {
		String[] resultFiles = getResultFiles(job);
		Map name2AxisName = new HashMap();
		for(int i = 0, length = resultFiles.length; i < length; i++) {
			String fileName = resultFiles[i];
			fileName = fileName.substring(fileName.indexOf("_") + 1, fileName.length());
			name2AxisName.put(fileName, resultFiles[i]);
		}

		for(Iterator it = outputFileNameToKnownGoodOutputMap.keySet().iterator(); it.hasNext(); ) {
			String outputFileName = (String) it.next();
			String knownGoodFileName = (String) outputFileNameToKnownGoodOutputMap.get(outputFileName);

			// find the output file in the tmp directory
			String axisName = (String) name2AxisName.get(outputFileName);
			String[] cmd = {"diff", "--brief", new File(axisName).getCanonicalPath(), new File(knownGoodFileName).getCanonicalPath()};

			Runtime rt = Runtime.getRuntime();
			Process process = rt.exec(cmd, null, null);
			process.waitFor();
			Thread outputReader = new StreamCopier(process.getInputStream(), System.out);
			Thread errorReader = new StreamCopier(process.getErrorStream(), System.err);
			outputReader.start();
			errorReader.start();
			int exitValue = process.exitValue();
			assertTrue("Differences found between " + outputFileName + " and " + knownGoodFileName, exitValue == 0);

		}
	}


	protected void tearDown() {
		if(deleteOuputFiles) {
			super.tearDown();
		}
	}


	/**
	 *  A thread that is used to capture that standard out and standard err of a
	 *  process.
	 *
	 *@author     jgould
	 *@created    March 5, 2004
	 */
	static class StreamCopier extends Thread {

		InputStream is = null;
		PrintStream os = null;



		public StreamCopier(InputStream is, PrintStream os) {
			this.is = is;
			this.os = os;
			this.setDaemon(true);
		}


		/**  Main processing method for the StreamCopier object */
		public void run() {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String line;

			try {
				while((line = in.readLine()) != null) {
					os.println(line);
					os.flush();
					// show it to the user ASAP
				}

			} catch(IOException ioe) {
				System.err.println(ioe + " while reading from process stream");
			}
		}
	}
}

