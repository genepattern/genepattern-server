<%--
  ~ Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.  --%>

<%@ page import="org.genepattern.server.webapp.jsf.AboutBean" %>
<% AboutBean aboutBean = new AboutBean(); %>
<% String username = (String) request.getAttribute("userid"); %>
<!-- top band with the logo -->
<div id="topband" class="topband">
    <a href="index.jsp" target="_top">
        <img src="<%=request.getContextPath()%>/images/GP-logo.gif" alt="GenePattern Portal" width="229" height="48" border="0" />
    </a>
</div>

<!-- horizontal navigation band -->
<div id="navband1" class="navband1 ddsmoothmenu" style="white-space: nowrap; display:none;">
	 <ul>
	     <li><a href="/gp/pages/index.jsf">Modules &#38; Pipelines</a>
	         <ul>
	             <li class="createPrivatePipelineAllowed"><a href="JavaScript:Menu.denyIE('/gp/pipeline/index.jsf');">New Pipeline</a></li>
	             <li class="createTaskAllowed"><a href="JavaScript:Menu.denyIE('/gp/modules/creator.jsf');">New Module</a></li>
	             <li class="createTaskAllowed"><a href="/gp/pages/taskCatalog.jsf">Install From Repository</a></li>
	             <li><a href="/gp/pages/importTask.jsf">Install From ZIP</a></li>
	             <li><a href="/gp/pages/manageTasks.jsf">Manage</a></li>
	         </ul>
	     </li>
	     <li><a href="/gp/pages/manageSuite.jsf">Suites</a>
	         <ul>
	             <li class="createPrivateSuiteAllowed"><a href="/gp/pages/createSuite.jsf">New Suite</a></li>
	             <li class="createPublicSuiteAllowed"><a href="/gp/pages/suiteCatalog.jsf">Install From Repository</a></li>
	             <li><a href="/gp/pages/importTask.jsf?suite=1">Install From ZIP</a></li>
	             <li><a href="/gp/pages/manageSuite.jsf">Manage</a></li>
	         </ul>
	     </li>
         <li><a href="/gp/pages/index.jsf?jobResults=userId%3D${requestScope.userID}">Job Results</a>
             <ul>
                 <li><a href="/gp/pages/index.jsf?jobResults=userId%3D${requestScope.userID}">Results Summary</a></li>
             </ul>
         </li>
	     <li><a href="/gp/pages/index.jsf?splash=resources">Resources</a>
	         <ul>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/contact')">Mailing List</a></li>
	             <li><a href="<%=aboutBean.getContactUs()%>">Report Bugs</a></li>
	             <li><a href="<%=aboutBean.getContactUs()%>">Contact Us</a></li>
                 <li><a href="/gp/pages/downloadProgrammingLibaries.jsf">Programming Languages</a></li>
                 <li><a href="JavaScript:Menu.go('https://www.genepattern.org/datasets/')">Public Datasets</a></li>
                 <li><a href="JavaScript:Menu.go('http://www.gparc.org/')">GParc</a></li>
	         </ul>
	     </li>
	     <li class="adminServerAllowed"><a href="/gp/pages/serverSettings.jsf">Administration</a>
	         <ul>
	             <li><a href="/gp/pages/serverSettings.jsf">Server Settings</a></li>
	         </ul>
	     </li>
	     <li><a href="/gp/pages/index.jsf">Help</a>
	         <ul>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/tutorial')">Tutorial</a></li>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/video-tutorials')">Video Tutorial</a></li>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/user-guide')">User Guide</a></li>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/programmers-guide')">Programmers Guide</a></li>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/file-formats-guide')">File Formats</a></li>
	             <li><a href="JavaScript:Menu.go('https://github.com/genepattern/genepattern-server/releases/latest')">Release Notes</a></li>
	             <li><a href="JavaScript:Menu.go('https://www.genepattern.org/FAQ')">FAQ</a></li>
	             <li><a href="/gp/pages/about.jsf">About</a></li>
	         </ul>
	     </li>
	 </ul>
	 <br style="clear: left"/>
</div>
<script type="text/javascript">
   	if (userLoggedIn) {
   		jq("#navband1").ready(function() {
			Menu.initNavMenu();
		});
   	}
   	
	// Stub out old embeded javascript calls
    function MM_swapImage() {
        return true;
    }
    function MM_swapImgRestore() {
        return true;
    }
</script>

<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <!-- main content area.  -->
        <tr>
            <td valign="top" class="maincontent" id="maincontent">
