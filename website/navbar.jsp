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
<script language="JavaScript1.2" type="text/javascript">
    var agt = navigator.userAgent.toLowerCase();
    var isSafari2 = agt.indexOf("safari/4") != -1;
    var x = isSafari2 ? -90 : 0;
    var y = isSafari2 ? 10 : 18;
</script>
<script type="text/javascript">
    if (userLoggedIn) {
        Menu.buildNavMenu();
    }
</script>

<!-- begin content area. this area contains three columns in an adjustable table, including tasks & pipeline, the center working space, and recent jobs. -->
<div id="content" class="content">
    <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <!-- main content area.  -->
        <tr>
            <td valign="top" class="maincontent" id="maincontent">
