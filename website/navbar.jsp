
<%
String cp = request.getContextPath();
%>
<link href="<%=cp%>/css/style.css" rel="stylesheet" type="text/css" />

<script language="JavaScript" src="<%=cp%>/js/menu.js"></script>
<script language="JavaScript" src="<%=cp%>/js/mm_menu.js"></script>
<script language="JavaScript1.2">MM_preloadImages('<%=cp%>/images/searchicon-1-over.gif','<%=cp%>/images/menuicon-over.gif');</script>

<script language="JavaScript1.2">mmLoadMenus();</script>
<!-- top band with the logo -->
<div id="topband" class="topband"><a href="index.html"
	target="_top"> <img src="<%=cp%>/images/GP-logo.gif"
	alt="GenePattern Portal" width="296" height="77" border="0" /> </a></div>
<!-- horizontal navigation band -->
<div id="navband1" class="navband1"><font class="navband1-on">
<a href="index.html"> Tasks &amp; Pipelines </a>
&#160;&#160;&#160;&#160;&#160;&#160; </font> <a href="job_results.html">
Job Results </a> &#160;&#160;&#160;&#160;&#160;&#160; <a
	href="create_pipeline.html"> Create Pipeline </a>
&#160;&#160;&#160;&#160;&#160;&#160; <a href="documentation.html"
	name="link2" id="link1"
	onmouseover="MM_showMenu(window.mm_menu_0925165148_0,0,14,null,'link2')"
	onmouseout="MM_startTimeout();"> Documentation </a>
&#160;&#160;&#160;&#160;&#160;&#160; <a href="resources.html"
	name="link4" id="link3"
	onmouseover="MM_showMenu(window.mm_menu_0925171310_0,0,14,null,'link4')"
	onmouseout="MM_startTimeout();"> Resources </a>
&#160;&#160;&#160;&#160;&#160;&#160; <a href="downloads.html"
	name="link6" id="link5"
	onmouseover="MM_showMenu(window.mm_menu_0925171536_0,0,14,null,'link6')"
	onmouseout="MM_startTimeout();"> Downloads </a></div>
<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
		<!-- 
		<c:if test="${!empty leftNav}">
			<td width="242" rowspan="2" valign="top" class="tasks" id="tasks">
			<ui:insert name="leftNav" /></td>
		</c:if>
		
		<c:if test="${!empty centerNav}">
			<ui:insert name="centerNav" />
		</c:if>
		
		
		<c:if test="${!empty rightNav}">
			<td width="203" rowspan="2" valign="top" class="recentjobs"
				id="recentjobs"><ui:insert name="rightNav" /></td>
			<
		</c:if>
		 -->
	</tr>
	<!-- main content area.  -->
	<tr>
		<td valign="top" class="maincontent" id="maincontent">
		</body>
		</html>