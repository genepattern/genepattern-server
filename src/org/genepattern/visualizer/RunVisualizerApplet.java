package org.genepattern.visualizer;

import java.applet.Applet;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.StringTokenizer;


public class RunVisualizerApplet extends Applet {

	// get all of the applet input parameters and create a HashMap of them that can be used to
	// invoke the visualizer in a non-applet-specific manner

	HashMap params = new HashMap();
	String[] supportFileNames = new String[0];
	long[] supportFileDates = new long[0];
	String[] wellKnownNames = { RunVisualizerConstants.NAME, RunVisualizerConstants.COMMAND_LINE, 
				    RunVisualizerConstants.DEBUG, RunVisualizerConstants.OS, 
				    RunVisualizerConstants.CPU_TYPE, RunVisualizerConstants.LIBDIR,
				    RunVisualizerConstants.DOWNLOAD_FILES, RunVisualizerConstants.LSID };
	URL source = null;

	public void init() {

	    try {
		source = getDocumentBase();
		for (int i = 0; i < wellKnownNames.length; i++) {
			setParameter(wellKnownNames[i], getParameter(wellKnownNames[i]));
		}
		StringTokenizer stParameterNames = new StringTokenizer(getParameter(RunVisualizerConstants.PARAM_NAMES), ", ");
		while (stParameterNames.hasMoreTokens()) {
			String paramName = stParameterNames.nextToken();
			String paramValue = getParameter(paramName);
			setParameter(paramName, paramValue);
		}

		setSupportFileNames(getParameter(RunVisualizerConstants.SUPPORT_FILE_NAMES));
		setSupportFileDates(getParameter(RunVisualizerConstants.SUPPORT_FILE_DATES));
		if (getParameter(RunVisualizerConstants.NO_RUN) == null) {
			run();
		} else {
			showStatus("No run flag set");
		}
	    } catch (Throwable t) {
		t.printStackTrace();
		try {
			getAppletContext().showDocument(new URL(source, "/gp/runVisualizer.jsp?" + 
							RunVisualizerConstants.NAME + "=" + params.get(RunVisualizerConstants.LSID) + 
							"&message=" + URLEncoder.encode(t.getMessage() + 
							" while attempting to run " + (String)params.get(RunVisualizerConstants.NAME))));
		} catch (MalformedURLException mue) { /* ignore */ }
	    }
	}

	public void setParameter(String name, String value) {
		params.put(name, value);
	}

	public void setSupportFileNames(String csvNames) {
		StringTokenizer stFileNames = new StringTokenizer(csvNames, ",");
		supportFileNames = new String[stFileNames.countTokens()];
		int f = 0;
		while (stFileNames.hasMoreTokens()) {
			supportFileNames[f] = stFileNames.nextToken();
			f++;
		}

	}

	public void setSupportFileDates(String csvDates) throws NumberFormatException {
		StringTokenizer stFileDates = new StringTokenizer(csvDates, ",");
		supportFileDates = new long[supportFileNames.length];
		int f = 0;
		while (stFileDates.hasMoreTokens()) {
			supportFileDates[f] = Long.parseLong(stFileDates.nextToken());
			f++;
		}
	}
	
	public void run() throws Exception {
		validateInputs();
		showStatus("starting " + params.get(RunVisualizerConstants.NAME));
		RunVisualizer visualizer = new RunVisualizer(params, source, supportFileNames, supportFileDates);
		visualizer.run();
	}

	protected void validateInputs() throws Exception {
		// make sure that each support file has an associated length
		if (supportFileNames.length != supportFileDates.length) throw new Exception("Mismatched number of support file names and dates");
		// make sure that all expected mandatory inputs are there
		for (int i = 0; i < wellKnownNames.length; i++) {
			if (params.get(wellKnownNames[i]) == null) throw new Exception("Missing input parameter " + wellKnownNames[i]);
		}
	}

	public void destroy() {
		showStatus("RunVisualizerApplet.destroy");
		super.destroy();
	}
	
	public void start() {
		showStatus("RunVisualizerApplet.start");
		super.start();
	}
	
	public void stop() {
		showStatus("RunVisualizerApplet.stop");
		super.stop();
	}

}
