<%@ page import="
		 java.net.URLEncoder,
		 org.genepattern.server.util.AuthorizationManager" %>
<%
	String username= (String)request.getAttribute("userID"); // get userID but don't force login if not defined
	AuthorizationManager authorizationManager = new AuthorizationManager();


%>
<font size=-2>
<font size='-1'><b>Tasks and Pipelines</b></font><br>

<%
	if (authorizationManager.isAllowed("addTask.jsp", username)){
%>
&nbsp;&nbsp;&nbsp;<a href="addTask.jsp">create task</a><br>
<% } %>
&nbsp;&nbsp;&nbsp;<a href="pipelineDesigner.jsp">create pipeline</a><br>

<%	if ((authorizationManager.checkPermission("createTask", username)) || (authorizationManager.checkPermission("createPipeline", username))){ %>
&nbsp;&nbsp;&nbsp;<a href="addZip.jsp">import</a><br>
<% } %>

<%	if (authorizationManager.isAllowed("taskCatalog.jsp", username)){ %>
&nbsp;&nbsp;&nbsp;<a href="taskCatalog.jsp">install/update</a><br>
<% } %>
<%	if (authorizationManager.isAllowed("deleteTask.jsp", username)){ %>
&nbsp;&nbsp;&nbsp;<a href="deleteTask.jsp">delete</a><p>
<% } %>

<font size='-1'><b>Suites</b></font><br>
&nbsp;&nbsp;&nbsp;<a href="editSuite.jsp">create suite</a><br>
&nbsp;&nbsp;&nbsp;<a href="addZip.jsp">import</a><br>
&nbsp;&nbsp;&nbsp;<a href="suiteCatalog.jsp">install/update suites</a><br>
&nbsp;&nbsp;&nbsp;<a href="suiteCatalog.jsp">delete suite</a>
<p>

<font size='-1'><b>Server Administration</b></font><br>
&nbsp;&nbsp;&nbsp;<a href="zipJobResults.jsp">job results</a><br>
<%	if (authorizationManager.isAllowed("adminServer.jsp", username)){ %>
&nbsp;&nbsp;&nbsp;<a href="adminServer.jsp">modify settings</a><br>
<% } %>



<p>
<font size='-1'><b>Documentation</b></font><br>
&nbsp;&nbsp;&nbsp;<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/" target="_new" >User's Manual/Tutorial</a><br>
&nbsp;&nbsp;&nbsp;<a href="http://www.broad.mit.edu/cancer/software/genepattern/doc/relnotes/current/" >Release notes </a><br>
&nbsp;&nbsp;&nbsp;<a href="http://www.broad.mit.edu/cancer/software/genepattern/faq/">FAQ</a><br>
&nbsp;&nbsp;&nbsp;<a href="http://www.broad.mit.edu/cancer/software/genepattern/datasets/" >Public datasets</a><br>
&nbsp;&nbsp;&nbsp;<a href="getTaskDoc.jsp" >Task documentation</a><br>
&nbsp;&nbsp;&nbsp;<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_fileformats.html" target="_new" >Common file formats</a>
<p>


<font size='-1'><b>Resources</b></font><br>

&nbsp;&nbsp;&nbsp;	<a href="<%= System.getProperty("JavaGEInstallerURL") %>?version=<%= System.getProperty("GenePatternVersion") %>&server=<%= URLEncoder.encode("http://" + request.getServerName() + ":" + request.getServerPort()) %>" class='navbarlinksmall'>Install</a> graphical client<br>
&nbsp;&nbsp;&nbsp;	<a href="mailto:gp-users-join@broad.mit.edu?body=Just send this!" class='navbarlinksmall'>Subscribe to gp-users mailing list</a><br>
&nbsp;&nbsp;&nbsp;	<a href="mailto:gp-help@broad.mit.edu" class='navbarlinksmall'>Report bugs</a><br>
&nbsp;&nbsp;&nbsp;	<a href="http://www.broad.mit.edu/cancer/software/genepattern/forum/" class='navbarlinksmall'>User Forum</a>

<p>



<font size='-1'><b>Programming Libraries</b></font><br>
&nbsp;&nbsp;&nbsp;Java:  <a href="downloads/GenePattern.zip">.zip</a>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_java.html" target="_blank">doc</a>
	<br>
&nbsp;&nbsp;&nbsp;R: 	<a href="downloads/GenePattern_0.1-0.zip">.zip</a> &nbsp;
	<a href="downloads/GenePattern_0.1-0.tar.gz">.tar.gz</a> &nbsp;
	<a href="GenePattern.R">source</a>
	<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_R.html" target="_blank">doc</a>
<br>

&nbsp;&nbsp;&nbsp;MATLAB:	<a href="downloads/GenePatternMatlab.zip">.zip</a>	<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_MATLAB.html" target="_blank">doc</a>
<p>
</font>