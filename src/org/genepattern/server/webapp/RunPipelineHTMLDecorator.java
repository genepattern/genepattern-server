package org.genepattern.server.webapp;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.util.StringUtils;
import org.genepattern.util.GPConstants;
import org.genepattern.util.PropertyFactory;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;



public class RunPipelineHTMLDecorator implements RunPipelineOutputDecoratorIF {
	PrintStream out = System.out;
	Properties omnigeneProps = null;
	Properties genepatternProps = null;
	PipelineModel model = null;

	protected static String GET_PIPELINE_JSP = "pipelineDesigner.jsp?name=";
	protected static String GET_JOB_JSP = "getJobResults.jsp?jobID=";
	protected static String GET_TASK_JSP = "addTask.jsp?view=1&name=";
	protected static String GET_TASK_FILE = "retrieveResults.jsp?";
	public static final String STDOUT = "stdout";
	public static final String STDERR = "stderr";

	String URL = null;

	public void setOutputStream(PrintStream outstr){
		out = outstr;
	}

	public void beforePipelineRuns(PipelineModel model){
		this.model = model;
		try {
			PropertyFactory property = PropertyFactory.getInstance();
			omnigeneProps  = property.getProperties("omnigene.properties");
      	     		genepatternProps  = property.getProperties("genepattern.properties");
			URL = localizeURL(genepatternProps.getProperty("GenePatternURL"));

		} catch (Exception ioe){
			omnigeneProps = new Properties();
			genepatternProps  = new Properties();
		}
		
		String jobID = System.getProperty("jobID");
		String isSaved = System.getProperty("savedPipeline");
                // bug 592. Don't give link to pipeline if it is not saved

                if ("false".equalsIgnoreCase(isSaved)){
                       out.print("running " + model.getName() +".pipeline as ");
                } else {
                    out.print("running ");
		    if (model.getLsid().length() > 0) {
			    out.print("<a href=\""+ URL + GET_PIPELINE_JSP + model.getLsid()+ "\">");
		    }
		    out.print(model.getName() +".pipeline");
		    out.print("</a>");
		    out.print(" as ");
                }
                out.println("<a href=\""+URL + GET_JOB_JSP + jobID +"\">job #" + jobID + "</a> on " + (new Date()));
		out.println("<p>");

		out.print("Pipeline summary: ");
		int taskNum = 0;
		Vector vTasks = model.getTasks();
		for (Enumeration eTasks = vTasks.elements(); eTasks.hasMoreElements(); taskNum++) {
			JobSubmission jobSubmission = (JobSubmission)eTasks.nextElement();
			out.print("<a href=\""+URL + GET_TASK_JSP + jobSubmission.getLSID()+"\">");
			out.print(jobSubmission.getName());
			out.print("</a>");
			if (taskNum < (vTasks.size()-1)) out.print(", ");
		}
		out.println("<p>");

		// set up the form for zip results
		out.println("<form name=\"results\" action=\""+URL+"zipJobResults.jsp\">");
		out.println("<input type=\"hidden\" name=\"name\" value=\""+model.getName()+"\">");
		out.println("<input type=\"hidden\" name=\"jobID\" value=\""+jobID+"\">");

		String fileName = "pipelineDescription.html";
		out.println("<p><input type=\"checkbox\" value=\"" + fileName +"=" + jobID +"/" + fileName+"\" name=\"dl\" checked>");

		out.println("<a target=\"_blank\" href=\""+  URL+GET_TASK_FILE+"job="+jobID+"&filename="+fileName  +"\">" + fileName + "</a><p>");



	//	out.println(fileName+"=" + jobID+ "/" +fileName);
	//	out.println("\" name=\"dl\" checked><p>");
		


		// set up the table for task reporting
		out.println("<table width=\"90%\"><tr><td><u>step</u></td><td><u>name and parameters</u></td></tr>");
		out.flush();
	}


	/**
	 * called before a task is executed
	 *
	 * If this is for a visualizer, write out the applet code
	 */
	public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps){
		
		out.print("<tr><td valign=top width=20><nobr>" + idx + " of " + numSteps + "</nobr></td>");
		out.print("<td valign=top>"+jobSubmission.getName() + "(" );
		ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
		for (int i=0; i < parameterInfo.length; i++){
			ParameterInfo aParam = parameterInfo[i];
			boolean isInputFile = aParam.isInputFile();
			HashMap hmAttributes = aParam.getAttributes();
			String paramType = null;
			if (hmAttributes != null) paramType = (String)hmAttributes.get(ParameterInfo.TYPE);
			if (!isInputFile && !aParam.isOutputFile() && paramType != null && paramType.equals(ParameterInfo.FILE_TYPE)) {
				isInputFile = true;
			}
			isInputFile = (aParam.getName().indexOf("filename") != -1);

			out.print(aParam.getName());
			out.print("=");
			if (isInputFile){
				// convert from "localhost" to the actual host name so that 
				// it can be referenced from anywhere (eg. visualizer on non-local client)
				aParam.setValue(localizeURL(aParam.getValue()));
				out.print("<a href=\"");
				out.print(aParam.getValue());
				out.print("\">");

			}
			out.print(htmlEncode(aParam.getValue()));
	
			if (isInputFile){
				out.println("</a>");
			}


			if (i != (parameterInfo.length-1)) out.print(", ");
		}
		out.print(")");
		if (jobSubmission.isVisualizer()) 
			writeVisualizerAppletTag(jobSubmission);

		out.print("</td></tr>");	
		out.flush();
	}	

	// output the applet tag for a visdualizer
	public void writeVisualizerAppletTag(JobSubmission jobSubmission){
		// PUT APPLET HERE
		StringBuffer buff = new StringBuffer();
		buff.append(URL);
		buff.append("runVisualizer.jsp?name=");
		buff.append(jobSubmission.getLSID());
		buff.append("&userid=");
		buff.append(System.getProperty("userID", ""));
		ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
		for (int i=0; i < parameterInfo.length; i++){
			buff.append("&");
			ParameterInfo aParam = parameterInfo[i];
			try {
				buff.append(URLEncoder.encode(aParam.getName(), "utf-8"));
				buff.append("=");
				buff.append(URLEncoder.encode(aParam.getValue(), "utf-8"));
			} catch (UnsupportedEncodingException uee) {
				// ignore
			}
		}
		
		try {
			URL url = new URL(buff.toString());
			Object appletTag = url.getContent();
			BufferedReader reader =  new BufferedReader(new InputStreamReader((InputStream)appletTag));
			String line = null;
			while ((line = reader.readLine()) != null){
				out.println(line);
			}

		} catch (Exception mue) {
			out.println("Could not create applet tag " + mue);
			mue.printStackTrace();
		}
		out.flush();
	}


/**
 * called after a task execution is complete
 *
 * If this is for a visualizer, do nothing
 */
	public void recordTaskCompletion(JobInfo jobInfo, String name){
		
		ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
		StringBuffer sbOut = new StringBuffer();

	 	for(int j = 0; j < jobParams.length; j++){
		       	if(!jobParams[j].isOutputFile()){
				continue;
			}
			
			sbOut.setLength(0);
			String fileName = new File("../../" + jobParams[j].getValue()).getName();	
							
			sbOut.append("<tr><td></td><td><input type=\"checkbox\" value=\"");
			sbOut.append(name+"/"+fileName+"=" + jobInfo.getJobNumber()+ "/" +fileName);
			sbOut.append("\" name=\"dl\" ");
			sbOut.append("checked><a target=\"_blank\" href=\"");

			String outFileUrl = null;
			try {
				outFileUrl = URL+GET_TASK_FILE+"job="+jobInfo.getJobNumber()+"&filename="+URLEncoder.encode(fileName, "utf-8");
			} catch (UnsupportedEncodingException uee) {
				outFileUrl = URL+GET_TASK_FILE+"job="+jobInfo.getJobNumber()+"&filename="+fileName;
			}
                                
			sbOut.append(localizeURL(outFileUrl));
			try {
				fileName = URLDecoder.decode(fileName, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				// ignore
			}
			sbOut.append("\">"+htmlEncode(fileName)+"</a></td></tr>");
			out.println(sbOut.toString());
		}
	      out.flush();

	}


	public void afterPipelineRan(PipelineModel model){
		out.println("</table>");

		out.println("<center><input type=\"submit\" name=\"download\" value=\"download selected results\">&nbsp;&nbsp;");
		out.println("<a href=\"javascript:checkAll(this.form, true)\">check all</a> &nbsp;&nbsp;"); 
		out.println("<a href=\"javascript:checkAll(this.form, false)\">uncheck all</a></center><br><center>");
		out.println("<input type=\"submit\" name=\"delete\" value=\"delete selected results\"");
		out.println(" onclick=\"return confirm(\'Really delete the selected files?\')\">");
		out.println("</form>");
		out.flush();
	}



	public void showVisualizerApplet(JobSubmission jobSubmission, int idx, int numSteps){
	}

	protected String localizeURL(String original) {
		if (original == null) return "";
		String GENEPATTERN_PORT = "GENEPATTERN_PORT";
		String GENEPATTERN_URL = "GenePatternURL";
		String port = genepatternProps.getProperty(GENEPATTERN_PORT);
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GPConstants.LSID + GPConstants.RIGHT_DELIMITER, model.getLsid());
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GENEPATTERN_PORT + GPConstants.RIGHT_DELIMITER, port);
		original = StringUtils.replaceAll(original, GPConstants.LEFT_DELIMITER + GENEPATTERN_URL + GPConstants.RIGHT_DELIMITER, System.getProperty("GenePatternURL"));
		try {
			// one of ours?
			if (!original.startsWith("http://localhost:" + port) && !original.startsWith("http://127.0.0.1:" + port)) {
				return original;
			}
			URL org = new URL(original);
			String localhost = InetAddress.getLocalHost().getCanonicalHostName();
			if (localhost.equals("localhost")) {
				// MacOS X can't resolve localhost when unplugged from network
				localhost = "127.0.0.1";
			}
			URL url = new URL("http://" + localhost + ":" + port + org.getFile());
			return url.toString();
		} catch (UnknownHostException uhe) {
			return original;
		} catch (MalformedURLException mue) {
			return original;
		}
	}

    /**
     * escapes characters that have an HTML entity representation.
     * It uses a quick string -> array mapping to avoid creating thousands of temporary objects.
     * @param nonHTMLsrc String containing the text to make HTML-safe
     * @return String containing new copy of string with ENTITIES escaped
     */
    public static final String htmlEncode( String nonHTMLsrc ) {
	if (nonHTMLsrc == null) return "";
	StringBuffer res = new StringBuffer();
	int l = nonHTMLsrc.length();
	int idx;
	char c;
	for ( int i = 0; i < l; i++ ) {
		c = nonHTMLsrc.charAt( i );
		idx = entityMap.indexOf( c );
		if ( idx == -1 ) {
			res.append( c );
		}
		else {
			res.append( quickEntities[ idx ] );
		}
	}
	return res.toString();
    }

    /**
     * static lookup table for htmlEncode method
     * @see #htmlEncode(String)
     *
     */
    private static final String[][] ENTITIES = {
		/* We probably don't want to filter regular ASCII chars so we leave them out */
		{"&", "amp"},
		{"<", "lt"},
		{">", "gt"},
		{"\"", "quot"},

		{"\u0083", "#131"},
		{"\u0084", "#132"},
		{"\u0085", "#133"},
		{"\u0086", "#134"},
		{"\u0087", "#135"},
		{"\u0089", "#137"},
		{"\u008A", "#138"},
		{"\u008B", "#139"},
		{"\u008C", "#140"},
		{"\u0091", "#145"},
		{"\u0092", "#146"},
		{"\u0093", "#147"},
		{"\u0094", "#148"},
		{"\u0095", "#149"},
		{"\u0096", "#150"},
		{"\u0097", "#151"},
		{"\u0099", "#153"},
		{"\u009A", "#154"},
		{"\u009B", "#155"},
		{"\u009C", "#156"},
		{"\u009F", "#159"},

		{"\u00A0", "nbsp"},
		{"\u00A1", "iexcl"},
		{"\u00A2", "cent"},
		{"\u00A3", "pound"},
		{"\u00A4", "curren"},
		{"\u00A5", "yen"},
		{"\u00A6", "brvbar"},
		{"\u00A7", "sect"},
		{"\u00A8", "uml"},
		{"\u00A9", "copy"},
		{"\u00AA", "ordf"},
		{"\u00AB", "laquo"},
		{"\u00AC", "not"},
		{"\u00AD", "shy"},
		{"\u00AE", "reg"},
		{"\u00AF", "macr"},
		{"\u00B0", "deg"},
		{"\u00B1", "plusmn"},
		{"\u00B2", "sup2"},
		{"\u00B3", "sup3"},

		{"\u00B4", "acute"},
		{"\u00B5", "micro"},
		{"\u00B6", "para"},
		{"\u00B7", "middot"},
		{"\u00B8", "cedil"},
		{"\u00B9", "sup1"},
		{"\u00BA", "ordm"},
		{"\u00BB", "raquo"},
		{"\u00BC", "frac14"},
		{"\u00BD", "frac12"},
		{"\u00BE", "frac34"},
		{"\u00BF", "iquest"},

		{"\u00C0", "Agrave"},
		{"\u00C1", "Aacute"},
		{"\u00C2", "Acirc"},
		{"\u00C3", "Atilde"},
		{"\u00C4", "Auml"},
		{"\u00C5", "Aring"},
		{"\u00C6", "AElig"},
		{"\u00C7", "Ccedil"},
		{"\u00C8", "Egrave"},
		{"\u00C9", "Eacute"},
		{"\u00CA", "Ecirc"},
		{"\u00CB", "Euml"},
		{"\u00CC", "Igrave"},
		{"\u00CD", "Iacute"},
		{"\u00CE", "Icirc"},
		{"\u00CF", "Iuml"},

		{"\u00D0", "ETH"},
		{"\u00D1", "Ntilde"},
		{"\u00D2", "Ograve"},
		{"\u00D3", "Oacute"},
		{"\u00D4", "Ocirc"},
		{"\u00D5", "Otilde"},
		{"\u00D6", "Ouml"},
		{"\u00D7", "times"},
		{"\u00D8", "Oslash"},
		{"\u00D9", "Ugrave"},
		{"\u00DA", "Uacute"},
		{"\u00DB", "Ucirc"},
		{"\u00DC", "Uuml"},
		{"\u00DD", "Yacute"},
		{"\u00DE", "THORN"},
		{"\u00DF", "szlig"},

		{"\u00E0", "agrave"},
		{"\u00E1", "aacute"},
		{"\u00E2", "acirc"},
		{"\u00E3", "atilde"},
		{"\u00E4", "auml"},
		{"\u00E5", "aring"},
		{"\u00E6", "aelig"},
		{"\u00E7", "ccedil"},
		{"\u00E8", "egrave"},
		{"\u00E9", "eacute"},
		{"\u00EA", "ecirc"},
		{"\u00EB", "euml"},
		{"\u00EC", "igrave"},
		{"\u00ED", "iacute"},
		{"\u00EE", "icirc"},
		{"\u00EF", "iuml"},

		{"\u00F0", "eth"},
		{"\u00F1", "ntilde"},
		{"\u00F2", "ograve"},
		{"\u00F3", "oacute"},
		{"\u00F4", "ocirc"},
		{"\u00F5", "otilde"},
		{"\u00F6", "ouml"},
		{"\u00F7", "divid"},
		{"\u00F8", "oslash"},
		{"\u00F9", "ugrave"},
		{"\u00FA", "uacute"},
		{"\u00FB", "ucirc"},
		{"\u00FC", "uuml"},
		{"\u00FD", "yacute"},
		{"\u00FE", "thorn"},
		{"\u00FF", "yuml"},
		{"\u0080", "euro"}
	};

	private static String entityMap;
	private static String[] quickEntities;

	static {
		// Initialize some local mappings to speed it all up
		int l = ENTITIES.length;
		StringBuffer temp = new StringBuffer();

		quickEntities = new String[ l ];
		for ( int i = 0; i < l; i++ ) {
			temp.append( ENTITIES[ i ][ 0 ] );
			quickEntities[ i ] = "&" + ENTITIES[ i ][ 1 ] + ";";
		}
		entityMap = temp.toString();
	}

}