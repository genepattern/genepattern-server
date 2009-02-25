<%--
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
--%>

<% String username = (String) request.getAttribute("userid"); %>
<!-- top band with the logo -->
<div id="topband" class="topband">
  <a href="index.jsp"target="_top">
    <img src="<%=request.getContextPath()%>/images/GP-logo.gif" alt="GenePattern Portal" width="229" height="48" border="0" />
  </a>
</div>


<!-- horizontal navigation band -->
    <script language="JavaScript1.2" type="text/javascript">
    	var agt = navigator.userAgent.toLowerCase();
        var isSafari2 = agt.indexOf("safari/4") != -1;
    	var x = isSafari2 ? -90 : 0;
    	var y =  isSafari2 ? 10 : 18;
    	mmLoadMenus();
    </script>
    <div id="navband1" class="navband1" style="cursor: pointer;" style="white-space: nowrap;">
       <a name="link17" id="link6"
           href="<%=request.getContextPath()%>/pages/index.jsf"
           onclick="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')"
           onmouseover="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')"
           onmouseout="MM_startTimeout();">Modules &amp; Pipelines</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link14" id="link9"
           href="<%=request.getContextPath()%>/pages/manageSuite.jsf"
           onclick="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')"
           onmouseover="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')"
           onmouseout="MM_startTimeout();">Suites</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link15" id="link10" href="<%=request.getContextPath()%>/jobResults"
           onclick="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')"
           onmouseover="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')"
           onmouseout="MM_startTimeout();">Job Results</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link12" id="link3"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=resources"
           onclick="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')"
           onmouseover="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')"
           onmouseout="MM_startTimeout();">Resources</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link2" id="link5"
           href="<%=request.getContextPath()%>/pages/index.jsf?splash=downloads"
           onclick="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')"
           onmouseover="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')"
           onmouseout="MM_startTimeout();">Downloads</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <% if(org.genepattern.server.webapp.jsf.AuthorizationHelper.adminServer(username)) { %>
        	<a name="link13" id="link4"
        	    href="<%=request.getContextPath()%>/pages/serverSettings.jsf"
           		onclick="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')"
           		onmouseover="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')"
           		onmouseout="MM_startTimeout();">Administration</a>&#160;&#160;&#160;&#160;&#160;&#160;
		<% } %>
        <a name="link11" id="link1"
           href="<%=request.getContextPath()%>/pages/index.jsf"
           onclick="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')"
           onmouseover="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')"
           onmouseout="MM_startTimeout();">Help</a>
   </div>

<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>

	</tr>
	<!-- main content area.  -->
	<tr>
		<td valign="top" class="maincontent" id="maincontent">
