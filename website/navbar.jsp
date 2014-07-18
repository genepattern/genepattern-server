<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

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
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/gp_mail.html')">Mailing List</a></li>
	             <li><a href="/gp/pages/contactUs.jsf">Report Bugs</a></li>
	             <li><a href="/gp/pages/contactUs.jsf">Contact Us</a></li>
                 <li><a href="/gp/pages/downloadProgrammingLibaries.jsf">Programming Languages</a></li>
                 <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/datasets/')">Public Datasets</a></li>
                 <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/software/gparc/')">GParc</a></li>
	         </ul>
	     </li>
	     <li class="adminServerAllowed"><a href="/gp/pages/serverSettings.jsf">Administration</a>
	         <ul>
	             <li><a href="/gp/pages/serverSettings.jsf">Server Settings</a></li>
	         </ul>
	     </li>
	     <li><a href="/gp/pages/index.jsf">Help</a>
	         <ul>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_tutorial.html')">Tutorial</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/desc/videos')">Video Tutorial</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_web_client.html')">User Guide</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_programmer.html')">Programmers Guide</a></li>
	             <li><a href="/gp/getTaskDoc.jsp">Module Documentation</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/gp_guides/file-formats')">File Formats</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/current/')">Release Notes</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.broadinstitute.org/cancer/software/genepattern/doc/faq/')">FAQ</a></li>
	             <li><a href="/gp/pages/about.jsf">About</a></li>
	         </ul>
	     </li>
	     <li class="genomeSpaceMenu">
	     	<a href="/gp/pages/index.jsf"><img src="/gp/pages/genomespace/genomespace_icon.gif" class="genomeSpaceIcon" alt="GenomeSpace"></img>GenomeSpace</a>
	         <ul>
	             <li class="genomeSpaceLoggedOut"><a href="/gp/pages/genomespace/signon.jsf">Login</a></li>
	             <li class="genomeSpaceLoggedIn"><a href="/gp/pages/genomespace/signon.jsf">Logout</a></li>
	             <li class="genomeSpaceLoggedOut"><a href="JavaScript:Menu.go('http://genomespace.org/register')">Register</a></li>
	             <li class="genomeSpaceLoggedIn"><a href="/gp/pages/genomespace/privateTool.jsf">Add as Private Tool</a></li>
	             <li><a href="JavaScript:Menu.go('https://gsui.genomespace.org/jsui/')">GenomeSpace UI</a></li>
	             <li><a href="JavaScript:Menu.go('http://www.genomespace.org/')">About</a></li>
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
