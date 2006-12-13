<!-- /*
The Broad Institute
SOFTWARE COPYRIGHT NOTICE AGREEMENT
This software and its documentation are copyright (2003-2006) by the
Broad Institute/Massachusetts Institute of Technology. All rights are
reserved.

This software is supplied without any warranty or guaranteed support
whatsoever. Neither the Broad Institute nor MIT can be responsible for its
use, misuse, or functionality.
*/ -->
<%@ page import="org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.disk.DiskFileItemFactory,
                 org.apache.commons.fileupload.servlet.ServletFileUpload,
                 org.apache.commons.io.FilenameUtils,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.util.GPConstants,
                 org.genepattern.util.StringUtils,
                 org.genepattern.server.webservice.server.local.*,
                 org.genepattern.webservice.JobInfo,
                 org.genepattern.webservice.OmnigeneException,
                 org.genepattern.webservice.ParameterFormatConverter,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
				 org.genepattern.server.user.User,
				 org.genepattern.server.user.UserDAO,
                 java.io.File,
                 java.io.UnsupportedEncodingException,
                 java.net.InetAddress,
                 java.net.MalformedURLException,
                 java.net.URL,
                 java.net.URLDecoder,
                 java.net.URLEncoder,
                 java.text.DateFormat,
                 java.text.ParseException,
                 java.text.SimpleDateFormat,
                 java.util.ArrayList,
                 java.util.Date,
                 java.util.GregorianCalendar,
                 java.util.HashMap,
                 java.util.Iterator,
                 java.util.*"
         session="false" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

%>
<html>
<head>
    <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
    <link href="css/style.css" rel="stylesheet" type="text/css">
    <link href="css/style-jobresults.css" rel="stylesheet" type="text/css">
    <link href="skin/favicon.ico" rel="shortcut icon">
    <title>GenePattern - Run Task Results</title>
	<script language="Javascript" src="js/prototype.js"></script>
	<script language="Javascript" src="js/commons-validator-1.3.0.js"></script>
	<script language="Javascript" src="js/genepattern.js"></script>

</head>
<body>

<jsp:include page="navbar.jsp"/>

<%
    String userID = null;
	String userEmail = null;
	String jobId = null;
    try {
        ServletFileUpload fub = new ServletFileUpload(new DiskFileItemFactory());
        HashMap htFilenames = new HashMap(); // map between form field name and filesystem name

        // create a dir for the input files
        File tempDir = File.createTempFile("runTaskPipeline", null);
        tempDir.delete();
        tempDir.mkdir();
        String tmpDirName = tempDir.getName();
        HashMap requestParameters = new HashMap();
        HashMap nameToFileItemMap = new HashMap();
        
        if (fub.isMultipartContent(request)) {
        	 List params = fub.parseRequest(request);
             
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            FileItem fi = (FileItem) iter.next();
            nameToFileItemMap.put(fi.getFieldName(), fi);
        }
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            FileItem fi = (FileItem) iter.next();
            if (!fi.isFormField()) {
                String fieldName = fi.getFieldName();
                String fileName = fi.getName();
                if (fileName == null || fileName.trim().equals("")) {
                    FileItem shadow = (FileItem) nameToFileItemMap.get(fieldName + "_url");
                    if (shadow != null) {
                        fileName = shadow.getString();
                    }
                }
                
                               
                if (fileName != null && !fileName.trim().equals("")) {
                    try {
                        new URL(fileName);
                        // don't bother trying to save a file that is a URL, retrieve it at execution time instead
                        htFilenames.put(fieldName, fileName);
                    } catch (MalformedURLException mfe) {
                    	File oldFile = new File(fileName);
                    	
                        fileName = FilenameUtils.getName(fileName);
                        File file = new File(tempDir, fileName);
                        if (file.exists()) {
                            if (fileName.length() < 3) {
                                fileName += "tmp";
                            }
                            file = File.createTempFile(fileName, FilenameUtils.getExtension(fileName), tempDir);
                        } 
                        try {
                           fi.write(file);
                           // deal with reload files that are not uploaded and so for which
                           // the write leaves an empty file
                           if (file.length() == 0){
                        	   file = oldFile;
                           }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        htFilenames.put(fieldName, file.getCanonicalPath());
                    }
                }
            } else {
                requestParameters.put(fi.getFieldName(), fi.getString());
            }
        } // loop over files
        } else {
        	for (Enumeration en = request.getParameterNames(); en.hasMoreElements(); ){
        		String k = (String)en.nextElement();
        		String v = request.getParameter(k);
        		requestParameters.put(k,v);
        	}
        }
        
        //http://cp21e-789.broad.mit.edu:8080/gp/getInputFile.jsp?file=Axis62355.att_all_aml_train.res
        String lsid = URLDecoder.decode((String) requestParameters.get("taskLSID"));
        String taskName = (String) requestParameters.get("taskName");
        if (lsid == null) {
            lsid = taskName;
        }
        userID = (String) request.getAttribute(GPConstants.USERID);
		try {
			User user = (new UserDAO()).findById(userID);
			userEmail = user.getEmail();
			if ((userEmail == null) || (userEmail.length() == 0)){
				userEmail = userID;
			}
		} catch (Exception e){
			userEmail = userID;
		}


        // set up the call to the analysis engine
        String server = request.getScheme() + "://" + InetAddress.getLocalHost().getCanonicalHostName() + ":" + System.getProperty("GENEPATTERN_PORT");
   
               
        LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
        
        TaskInfo task = GenePatternAnalysisTask.getTaskInfo(lsid, userID);
        if (task == null) {
            out.println("Unable to find task " + lsid);
            return;
        }
        ParameterInfo[] parmInfos = task.getParameterInfoArray();
        int nParams = 0;
        if (parmInfos != null) {
            nParams = parmInfos.length;
        } else {
            parmInfos = new ParameterInfo[0];
        }
        ArrayList missingReqParams = new ArrayList();
        for (int i = 0; i < nParams; i++) {
            ParameterInfo pinfo = parmInfos[i];
            String value;
            if (pinfo.isInputFile()) {
                value = (String) htFilenames.get(pinfo.getName());
                if (value == null) {
                    pinfo.getAttributes().put(ParameterInfo.TYPE, "");
                }
                if (value != null) {
                    try {
                        new URL(value);
                        HashMap attrs = pinfo.getAttributes();
                        attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                        attrs.remove(ParameterInfo.TYPE);
                    } catch (MalformedURLException mfe) {
                    }
                }
            } else {
                value = (String) requestParameters.get(pinfo.getName());
            }

            //
            // look for missing required params
            //
            if ((value == null) || (value.trim().length() == 0)) {
                HashMap pia = pinfo.getAttributes();
                boolean isOptional =
                        ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
                                .length() > 0;
                if (!isOptional) {
                    missingReqParams.add(pinfo);
                }
            }
            pinfo.setValue(value);
        }
        if (missingReqParams.size() > 0) {
            System.out.println("" + missingReqParams);
            request.setAttribute("missingReqParams", missingReqParams);
            (request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
        return;
    }   
               
    JobInfo job = analysisClient.submitJob(task.getID(), parmInfos);
    jobId = "" + job.getJobNumber();

%>
<script language="Javascript">

     function checkAll(cb) {
        var frm = document.forms["results"];
        var bChecked = cb.checked;
        for (i = 0; i < frm.elements.length; i++) {
            if (frm.elements[i].type != "checkbox") continue;
            frm.elements[i].checked = bChecked;
        }
    }

    function deleteCheckedFiles(){
	 var frm = document.forms["results"];
       var really = confirm('Really delete the checked files?');
       if (!really) return;
	cmd = frm.elements['cmdElement'];
	cmd.name="delete";
	cmd.value="true";
 	frm.submit();

    }
    function downloadCheckedFiles(){
	 var frm = document.forms["results"];
	
	cmd = frm.elements['cmdElement'];
	cmd.name="download";
	cmd.value="true";
	frm.submit();

    }

    function suppressEnterKey(event){
// do nothing?
	}


   function setEmailNotification(jobId){
		var cb = document.getElementById('emailCheckbox');
		var ue = document.getElementById("userEmail");
		var valid = jcv_checkEmail(ue.value); 
		if (!valid){
			var em = prompt("Email on completion to?:");
			if (em == null){
				cb.checked = false;
				return;
			} else {
				ue.value = em;
				valid = jcv_checkEmail(ue.value); 
				if (!valid){
					cb.checked = false;
					alert(ue.value + ' is not a valid email address');
					return;
				}
			}
		}

 	  	if (cb.checked) {
			requestEmailNotification(ue.value, jobId);
	 	} else {
			cancelEmailNotification(ue.value, jobId);
		}
   }

 
function toggleLogs() {
	var cb = document.getElementById('logCheckbox');
	var visible = cb.checked;
	
 	var frm = document.forms["results"];
       
	divObj = document.getElementById('executionLogDiv');
	cbdivObj = document.getElementById('executionLogCBDiv');
	cbObj = frm.executionLogCB;
	
	divObj.style.color='#EFEFEF';

	if(!visible) {
		divObj.style.display = "none";
		divObj.visibility=false;
		cbdivObj.style.display = "none";
		cbdivObj.visibility=false;

	} else {
		divObj.style.display = "";
		divObj.visibility=true;
		cbdivObj.style.display = "";
		cbdivObj.visibility=true;

	}
	
}


</script>

<table width="100%"  border="0" cellspacing="0" cellpadding="0">
<tr>
      <td valign="top" class="maintasknav" id="maintasknav">

	    <input type="checkbox" id="emailCheckbox" onclick="setEmailNotification(<%=jobId%>);" value="checkbox"/>email notification&nbsp;&nbsp;&nbsp;&nbsp;
		<input type="hidden" id="userEmail" value="<%= userEmail %>"/>



		<input name="showLogs" id="logCheckbox" type="checkbox" onclick="toggleLogs()"  value="showLogs" checked="checked" />
show execution logs</td>
        </tr>
</table>
 
<table width="100%"  border="0" cellpadding="0" cellspacing="0" class="barhead-task">
     <tr>
        <td><%=requestParameters.get("taskName")%> Status </td>
     </tr>
</table>

    
<table width='100%' cellpadding="0">
    <tr>
	  <td width="50px">
  <input name="stopCmd" id="stopCmd" type="button" value="stop..." onclick="stopJob(this, <%= job.getJobNumber()%>)" class="little">
	  </td>
        <td>
            Running <a href="addTask.jsp?view=1&name=<%=requestParameters.get("taskLSID")%>"><%=
            requestParameters.get("taskName")%>
        </a> as job # <a href="getJobResults.jsp?jobID=<%=job.getJobNumber() %>"><%=job.getJobNumber() %>
        </a> on <%=new Date()%>
	  </td>
    </tr>

<tr><td>&nbsp;</td></tr>
</table>

<table width="100%"  border="0" cellspacing="0" cellpadding="0">
            <tr class="smalltype">
              <td >&nbsp;</td>
              <td valign="top"><nobr>
                <div align="center"><a href="#"  onclick="downloadCheckedFiles()">download</a> | <a href="#"  onclick="deleteCheckedFiles()">delete</a> </div>

              </nobr></td>
              <td>&nbsp;</td>
              <td><!-- <a href="link">open all</a> | <a href="link">close all</a> --></td>
            </tr>
            <tr class="tableheader-row">
              <td>step</td>

              <td><div align="center">
                    <input name="checkbox" type="checkbox" value="checkbox" checked="true" onclick="checkAll(this)" />
               
              </div></td>
              <td>&nbsp;</td>
              <td>name and parameters </td>
            </tr>

<tr class="task-title">
    <td>1</td>
    <td colspan=2>&nbsp;</td>

    <td colspan=1>
        <%=requestParameters.get("taskName")%> 
    </td>
    </tr>
    <tr><td/><td/><td/><td>
        <%


            ParameterInfo[] formalParameterInfoArray = null;
            try {
                formalParameterInfoArray = ParameterFormatConverter.getParameterInfoArray(task.getParameterInfo());
                if (formalParameterInfoArray == null) {
                    formalParameterInfoArray = new ParameterInfo[0];
                }
            } catch (OmnigeneException oe) {
            }
            for (int i = 0; i < parmInfos.length; i++) {
                ParameterInfo pinfo = parmInfos[i];
                ParameterInfo formalPinfo = pinfo;
                for (int j = 0; j < formalParameterInfoArray.length; j++) {
                    ParameterInfo pi = formalParameterInfoArray[j];
                    if (pi.getName().equals(pinfo.getName())) {
                        formalPinfo = pi;
                        break;
                    }
                }
                String value = pinfo.getValue();
                boolean isURL = false;
                try {
                    new URL(value);
                    isURL = true;
                } catch (MalformedURLException mfe) {
                }
                out.println(pinfo.getName().replace('.', ' '));
                out.println("=");
                if (pinfo.isInputFile()) {
                    String htmlValue = StringUtils.htmlEncode(pinfo.getValue().trim());
                    if (isURL) {
                        out.println("<a href='" + htmlValue + "'>" + htmlValue + "</a>");
                    } else {
                    	// replace full path with just the name
                    	File f = new File(value);
                    	value = f.getName();
                    	htmlValue = StringUtils.htmlEncode(value.trim());
                        out.println("<a href='getFile.jsp?task=&file=" +
                                URLEncoder.encode(tmpDirName + "/" + value, "UTF-8") + "'>" + htmlValue + "</a>");
                    }
                } else if (isURL) {
                    out.println("<a href='" + value + "'>" + value + "</a>");
                } else {
                    String display = pinfo.getUIValue(formalPinfo);
                    out.println(StringUtils.htmlEncode(display));
                }
                if (i != (parmInfos.length - 1)) {
                    out.println(", ");
                }
            }
        %>
        <br></td>
</tr>
<tr><td colspan=4>&nbsp;</td></tr>

<form id="results" name="results" action="zipJobResults.jsp">
    <input type="hidden" name="name" value="<%=task.getName()%>"/>
    <input type="hidden" name="jobID" value="<%=jobId%>"/>
    <input type="hidden" name="cmdElement" value=""/>



    <%
     
        out.flush();
        String status = "started";
        while (!(status.equalsIgnoreCase("ERROR") || (status
                .equalsIgnoreCase("Finished")))) {
            Thread.sleep(500);         
            job = analysisClient.checkStatus(job.getJobNumber());
       
            if (job != null)  status = job.getStatus();
            
        }

        // after task completes jobInfo is the same as job
        JobInfo jobInfo = job;
        ParameterInfo[] jobParams = jobInfo.getParameterInfoArray();
           

        StringBuffer sbOut = new StringBuffer();
        for (int j = 0; j < jobParams.length; j++) {
            if (!jobParams[j].isOutputFile()) {
                continue;
            }
			boolean executionLog = false;
            sbOut.setLength(0);
			
            String fileName = new File("../../" + jobParams[j].getValue())
                    .getName();
    		if (fileName.equals(GPConstants.TASKLOG)) executionLog = true;        
	
			
			
			sbOut.append("<tr><td>&nbsp;</td><td align='center'>");

			if (executionLog){
				sbOut.append("<span id='executionLogCBDiv'>");
			}

			sbOut.append("<input type=\"checkbox\" value=\"");
			
            sbOut.append(""+jobInfo.getJobNumber() + "/" + fileName);
            sbOut.append("\" name=\"dl\" ");
			if (executionLog){
				sbOut.append("id='executionLogCB' ");
			}

            sbOut.append("checked>");
			if (executionLog){
				sbOut.append("</span>");
			}

			sbOut.append("</td><td/><td>");
			if (executionLog){
				sbOut.append("<span id='executionLogDiv'>");
			}

			sbOut.append("<a target=\"_blank\" href=\"");
            String outFileUrl;
            try {
                outFileUrl = "retrieveResults.jsp?job=" + jobInfo.getJobNumber() + "&filename=" +
                        URLEncoder.encode(fileName, "utf-8");
            } catch (UnsupportedEncodingException uee) {
                outFileUrl = "retrieveResults.jsp?job=" + jobInfo.getJobNumber() + "&filename=" + fileName;
            }
            sbOut.append(outFileUrl);
            try {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                // ignore
            }
            
            sbOut.append("\">" + StringUtils.htmlEncode(fileName) + "</a>");
			if (executionLog){
				sbOut.append("</span>");
			}
			sbOut.append("</td></tr>");
            out.println(sbOut.toString());
            
        }
    out.println("<tr><td colspan=4>&nbsp;</td></tr>");

     out.println("<tr class=\"smalltype\"><td/><td colspan=1  valign='top'><nobr><div align='center'><a href='#' onclick=\"downloadCheckedFiles()\">download</a> | <a href='#' onclick=\"deleteCheckedFiles()\" >delete</a> </div></nobr></td><td/><td/></tr>");


           out.flush();
        
         out.println("</form></td></tr>");
 out.println("<tr><td colspan=4>&nbsp;</td></tr>");



    %>

</table>
<script language="Javascript">
	document.getElementById("stopCmd").disabled=true;
	document.getElementById("stopCmd").visibility=false;

 
    var emailCB = document.getElementById("emailCheckbox");
    emailCB.enabled=false;
    
</script>
<%
    GregorianCalendar purgeTOD = new GregorianCalendar();
    try {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        GregorianCalendar gcPurge = new GregorianCalendar();
        gcPurge.setTime(dateFormat.parse(System.getProperty("purgeTime", "23:00")));
        purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, gcPurge.get(GregorianCalendar.HOUR_OF_DAY));
        purgeTOD.set(GregorianCalendar.MINUTE, gcPurge.get(GregorianCalendar.MINUTE));
    } catch (ParseException pe) {
        purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, 23);
        purgeTOD.set(GregorianCalendar.MINUTE, 0);
    }
    purgeTOD.set(GregorianCalendar.SECOND, 0);
    purgeTOD.set(GregorianCalendar.MILLISECOND, 0);
    int purgeInterval;
    try {
        purgeInterval = Integer.parseInt(System.getProperty("purgeJobsAfter", "-1"));
    } catch (NumberFormatException nfe) {
        purgeInterval = 7;
    }
    purgeTOD.add(GregorianCalendar.DATE, purgeInterval);
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
%>
</center>

<table with="100%">
<tr><td  class="purge_notice">
    These job results are scheduled to be purged from the server on <%= df.format(purgeTOD.getTime()).toLowerCase() %>
</td></tr>
</table>

    <%
        } catch (Throwable e) {
            e.printStackTrace();
            out.println("An error occurred.");
        }
    %>
    <jsp:include page="footer.jsp"/>

</body>
</html>

