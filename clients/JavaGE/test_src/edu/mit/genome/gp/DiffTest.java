package edu.mit.genome.gp;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.parser.ParserDelegator;
import java.net.URL;


import edu.mit.wi.omnigene.framework.analysis.*;
import edu.mit.wi.omnigene.framework.webservice.*;
import edu.mit.wi.omnigene.util.*;
import edu.mit.wi.omniview.analysis.*;
import edu.mit.wi.omnigene.framework.analysis.webservice.client.*;
import edu.mit.broad.util.*;

public class DiffTest extends TestCallTasks {
	
	public DiffTest(String name) {
		super(name);	
	}
	
	public void test() throws Exception {
		runPipeline("test.pipeline");
	}

	
	private void runPipeline(String name) throws Exception {
		AnalysisService svc = (AnalysisService)serviceMap.get(name);
		logDebug("Testing " + svc.getTaskInfo().getName());
		// set up parameters
		ParameterInfo params[] = new ParameterInfo[0];

		// call and wait for completion or error
		AnalysisJob job = submitJob(svc, params);
		JobInfo jobInfo = waitForErrorOrCompletion(job, 40, 2000);
		logDebug("Job # " + jobInfo.getJobNumber());
		// look for successful completion (not an error)
		assertTrue("Status not Finished", "Finished".equalsIgnoreCase(jobInfo.getStatus()));
	//	assertNoStddErrFileGenerated(job);
		String[] files = getResultFiles(job);
		ParserDelegator delegator = new ParserDelegator();
		FileReader reader = new FileReader(files[0]); 
		MyCallBack cb = new MyCallBack();
		delegator.parse(reader, cb, true);
	}


	class MyCallBack extends HTMLEditorKit.ParserCallback {
		
		
		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {

		}


		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			try {
				if(t == HTML.Tag.A) {
					String s = (String)a.getAttribute(HTML.Attribute.HREF);
					if(s.indexOf("retrieveResults.jsp") > 0) {
						//http://localhost:8080/gp/retrieveResults.jsp?filename=ovaryrows.res&dirName=375
						// download from server
						URL url = new URL(s);
						InputStream is = url.openStream();
						int fileNameIndex = s.indexOf("filename=");
						int dirNameIndex = s.indexOf("&dirName=");
						String fileName = s.substring(fileNameIndex, dirNameIndex);
						if(fileName.indexOf("gp.diff") >= 0) {
							Properties props = new Properties();
							props.load(is);
							is.close();
							String exitValue = props.getProperty("exit.value");
							String file1 = props.getProperty("file.1");
							String file2 = props.getProperty("file.2");
							logDebug("Performing a diff between " + file1 + " and " + file2);
							assertTrue("Differences found between " + file1 + " and " + file2 + ".", exitValue.equals("0"));
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

	}

}

