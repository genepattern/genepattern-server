<%--
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
--%>

<%@ page import="org.genepattern.server.webapp.jsf.AuthorizationHelper" %>
<link href="<%=request.getContextPath()%>/css/style.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" language="javascript">
  var contextRoot = "<%=request.getContextPath()%>/";
  <% String username = (String) request.getAttribute("userid"); %>
  var createTaskAllowed = <%= AuthorizationHelper.createModule(username) %>;
  var createPublicPipelineAllowed = <%= AuthorizationHelper.createPipeline(username) %>;
  var createPublicSuiteAllowed = <%= AuthorizationHelper.createSuite(username) %>;
  var createPrivatePipelineAllowed = <%= AuthorizationHelper.createPipeline(username) %>;
  var createPrivateSuiteAllowed = <%= AuthorizationHelper.createSuite(username) %>;

</script>
<script language="JavaScript" src="<%=request.getContextPath()%>/js/mainMenu.js"></script>
<script language="JavaScript" src="<%=request.getContextPath()%>/js/mm_menu.js"></script>
<script language="JavaScript1.2">MM_preloadImages('<%=request.getContextPath()%>/images/searchicon-1-over.gif','<%=request.getContextPath()%>/images/menuicon-over.gif');</script>
