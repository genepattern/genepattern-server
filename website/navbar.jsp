
<%
String cp = request.getContextPath();
%>
<link href="<%=cp%>/css/style.css" rel="stylesheet" type="text/css" />

<script language="JavaScript" src="<%=cp%>/js/mainMenu.js"></script>
<script language="JavaScript" src="<%=cp%>/js/mm_menu.js"></script>
<script language="JavaScript1.2">MM_preloadImages('<%=cp%>/images/searchicon-1-over.gif','<%=cp%>/images/menuicon-over.gif');</script>

<script language="JavaScript1.2">mmLoadMenus();</script>
<!-- top band with the logo -->
<div id="topband" class="topband">
  <a href="index.jsp"target="_top"> 
    <img src="<%=cp%>/images/GP-logo.gif" alt="GenePattern Portal" width="296" height="77" border="0" /> 
  </a>
</div>

<script type="text/javascript" language="javascript"> 
  var contextRoot = "<%=cp%>";
</script>
	
<!-- horizontal navigation band -->
<div id="navband1" class="navband1"
    <div id="navband1" class="navband1" style="cursor: pointer;">
        <nobr>
        <a name="link17" id="link6" 
           onclick="MM_showMenu(window.mm_menu_tasks,0,18,null,'link17')" 
           onmouseover="MM_showMenu(window.mm_menu_tasks,0,18,null,'link17')" 
           onmouseout="MM_startTimeout();">Tasks</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link16" id="link8" 
           onclick="MM_showMenu(window.mm_menu_pipelines,0,18,null,'link16')" 
           onmouseover="MM_showMenu(window.mm_menu_pipelines,0,18,null,'link16')" 
           onmouseout="MM_startTimeout();">Pipelines</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link14" id="link9" 
           onclick="MM_showMenu(window.mm_menu_suites,0,18,null,'link14')" 
           onmouseover="MM_showMenu(window.mm_menu_suites,0,18,null,'link14')" 
           onmouseout="MM_startTimeout();">Suites</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link15" id="link10" 
           onclick="MM_showMenu(window.mm_menu_jobResults,0,18,null,'link15')" 
           onmouseover="MM_showMenu(window.mm_menu_jobResults,0,18,null,'link15')" 
           onmouseout="MM_startTimeout();">Job Results</a> &#160;&#160;&#160;&#160;&#160;&#160;  
        <a name="link11" id="link1" 
           onclick="MM_showMenu(window.mm_menu_documentation,0,18,null,'link11')" 
           onmouseover="MM_showMenu(window.mm_menu_documentation,0,18,null,'link11')" 
           onmouseout="MM_startTimeout();">Documentation</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link12" id="link3" 
           onclick="MM_showMenu(window.mm_menu_resources,0,18,null,'link12')" 
           onmouseover="MM_showMenu(window.mm_menu_resources,0,18,null,'link12')" 
           onmouseout="MM_startTimeout();">Resources</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link2" id="link5" 
           onclick="MM_showMenu(window.mm_menu_downloads,0,18,null,'link2')" 
           onmouseover="MM_showMenu(window.mm_menu_downloads,0,18,null,'link2')" 
           onmouseout="MM_startTimeout();">Downloads</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link13" id="link4" 
           onclick="MM_showMenu(window.mm_menu_administration,0,18,null,'link13')" 
           onmouseover="MM_showMenu(window.mm_menu_administration,0,18,null,'link13')" 
           onmouseout="MM_startTimeout();">Administration</a>     
        </nobr>
    </div>
<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>

	</tr>
	<!-- main content area.  -->
	<tr>
		<td valign="top" class="maincontent" id="maincontent">