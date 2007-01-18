
<!-- top band with the logo -->
<div id="topband" class="topband">
  <a href="index.jsp"target="_top"> 
    <img src="<%=request.getContextPath()%>/images/GP-logo.gif" alt="GenePattern Portal" width="296" height="77" border="0" /> 
  </a>
</div>

	
<!-- horizontal navigation band -->
    <script language="JavaScript1.2" type="text/javascript">
    	var agt = navigator.userAgent.toLowerCase();
    	var isSafari = agt.indexOf("safari") != -1;
    	var x = isSafari ? -90 : 0;
    	var y =  isSafari ? 10 : 18;
    	mmLoadMenus();
    </script>
    <div id="navband1" class="navband1" style="cursor: pointer;">
        <nobr>
       
        <a name="link17" id="link6" 
           onclick="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')" 
           onmouseover="MM_showMenu(window.mm_menu_tasks,x,y,null,'link17')" 
           onmouseout="MM_startTimeout();">Tasks &amp; Pipelines</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link14" id="link9" 
           onclick="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')" 
           onmouseover="MM_showMenu(window.mm_menu_suites,x,y,null,'link14')" 
           onmouseout="MM_startTimeout();">Suites</a> &#160;&#160;&#160;&#160;&#160;&#160;
        <a name="link15" id="link10" 
           onclick="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')" 
           onmouseover="MM_showMenu(window.mm_menu_jobResults,x,y,null,'link15')" 
           onmouseout="MM_startTimeout();">Job Results</a> &#160;&#160;&#160;&#160;&#160;&#160;  
        <a name="link11" id="link1" 
           onclick="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')" 
           onmouseover="MM_showMenu(window.mm_menu_documentation,x,y,null,'link11')" 
           onmouseout="MM_startTimeout();">Documentation</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link12" id="link3" 
           onclick="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')" 
           onmouseover="MM_showMenu(window.mm_menu_resources,x,y,null,'link12')" 
           onmouseout="MM_startTimeout();">Resources</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link2" id="link5" 
           onclick="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')" 
           onmouseover="MM_showMenu(window.mm_menu_downloads,x,y,null,'link2')" 
           onmouseout="MM_startTimeout();">Downloads</a> &#160;&#160;&#160;&#160;&#160;&#160; 
        <a name="link13" id="link4" 
           onclick="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')" 
           onmouseover="MM_showMenu(window.mm_menu_administration,x,y,null,'link13')" 
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