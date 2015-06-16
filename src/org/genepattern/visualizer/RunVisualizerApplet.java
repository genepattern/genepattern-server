/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.visualizer;

import java.applet.Applet;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

public class RunVisualizerApplet extends Applet {

    // get all of the applet input parameters and create a HashMap of them that
    // can be used to
    // invoke the visualizer in a non-applet-specific manner

    private Map params = new HashMap();

    private String[] supportFileNames = new String[0];

    private long[] supportFileDates = new long[0];

    private String[] wellKnownNames = { RunVisualizerConstants.NAME, RunVisualizerConstants.COMMAND_LINE,
	    RunVisualizerConstants.OS, RunVisualizerConstants.CPU_TYPE, RunVisualizerConstants.DOWNLOAD_FILES,
	    RunVisualizerConstants.LSID };

    public void init() {
	try {
	    Class.forName("java.lang.ProcessBuilder"); // ProcessBuilder is new
	    // in java 1.5
	} catch (Throwable t) {
	    JOptionPane
		    .showMessageDialog(this,
			    "Your browser is not using the correct version of Java for GenePattern. Java version 1.5 or higher is required.");
	    return;

	}

	for (int i = 0; i < wellKnownNames.length; i++) {
	    setParameter(wellKnownNames[i], getParameter(wellKnownNames[i]));
	}
	try {
	    setParameter(RunVisualizerConstants.JAVA_FLAGS_NAME, getParameter(RunVisualizerConstants.JAVA_FLAGS_VALUE));
	    StringTokenizer stParameterNames = new StringTokenizer(getParameter(RunVisualizerConstants.PARAM_NAMES),
		    ", ");
	    while (stParameterNames.hasMoreTokens()) {
		String paramName = stParameterNames.nextToken();
		String paramValue = getParameter(paramName);
		setParameter(paramName, paramValue);
	    }

	    setSupportFileNames(getParameter(RunVisualizerConstants.SUPPORT_FILE_NAMES));
	    setSupportFileDates(getParameter(RunVisualizerConstants.SUPPORT_FILE_DATES));
	    exec();
	} catch (Throwable t) {
	    t.printStackTrace();
	    JOptionPane.showMessageDialog(this, "An error occurred while launching the visualizer.");
	}
    }

    public String getParameter(String name) {
	String value = super.getParameter(name);
	if (value == null) {
	    return null;
	}
	try {
	    return URLDecoder.decode(value, "UTF-8");
	} catch (UnsupportedEncodingException e) {
	    return value;
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

    public void exec() throws Exception {
	validateInputs();
	RunVisualizer visualizer = new RunVisualizer(params, supportFileNames, supportFileDates, this);
	visualizer.exec();
    }

    protected void validateInputs() throws Exception {
	// make sure that each support file has an associated length
	if (supportFileNames.length != supportFileDates.length)
	    throw new Exception("Mismatched number of support file names and dates");
	// make sure that all expected mandatory inputs are there
	for (int i = 0; i < wellKnownNames.length; i++) {
	    if (params.get(wellKnownNames[i]) == null)
		throw new Exception("Missing input parameter " + wellKnownNames[i]);
	}
    }

}
