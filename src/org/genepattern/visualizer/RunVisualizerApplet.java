/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.visualizer;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.JTextArea;

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

    private boolean inited = false;
    public void init() {
        try {
            // ProcessBuilder is new in java 1.5
            Class.forName("java.lang.ProcessBuilder");
        } 
        catch (Throwable t) {
            JOptionPane.showMessageDialog(this,
                "Your browser is not using the correct version of Java for GenePattern. Java version 1.5 or higher is required.");
            inited = true;
            return;
        }

        for (int i = 0; i < wellKnownNames.length; i++) {
            setParameter(wellKnownNames[i], getParameter(wellKnownNames[i]));
        }
        try {
            setParameter(RunVisualizerConstants.JAVA_FLAGS_NAME, getParameter(RunVisualizerConstants.JAVA_FLAGS_VALUE));
            StringTokenizer stParameterNames = new StringTokenizer(getParameter(RunVisualizerConstants.PARAM_NAMES), ", ");
            while (stParameterNames.hasMoreTokens()) {
                String paramName = stParameterNames.nextToken();
                String paramValue = getParameter(paramName);
                setParameter(paramName, paramValue);
            }

            setSupportFileNames(getParameter(RunVisualizerConstants.SUPPORT_FILE_NAMES));
            setSupportFileDates(getParameter(RunVisualizerConstants.SUPPORT_FILE_DATES));
            exec();
        } 
        catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while launching the visualizer.");
        }
    }
    
    //debugging code
    public void start() {
        super.start();
    }
    
    public void stop() {
        super.stop();
    }
    
    public void paint(Graphics g) {
        int width = this.getSize().width;
        int height = this.getSize().height;
        
        g.setColor(Color.black);
        g.fillRect(0, 0, getSize().width - 1, getSize().height - 1);
        g.setColor(Color.white);
        g.drawString("This is a test!", 5, 15);
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
        if (supportFileNames.length != supportFileDates.length) {
            throw new Exception("Mismatched number of support file names and dates");
        }
        // make sure that all expected mandatory inputs are there
        for (int i = 0; i < wellKnownNames.length; i++) {
            if (params.get(wellKnownNames[i]) == null) {
                throw new Exception("Missing input parameter " + wellKnownNames[i]);
            }
        }
    }

}
