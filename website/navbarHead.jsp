<jsp:useBean id="authorizationBean" scope="request" class="org.genepattern.server.webapp.jsf.AuthorizationBean" /> 
<jsp:useBean id="genepatternProperties" scope="application" class="org.genepattern.server.webapp.jsf.GenepatternProperties" /> 
<link href="<%=request.getContextPath()%>/css/style.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" language="javascript"> 
  var contextRoot = "<%=request.getContextPath()%>/";
  <% String username = (String) request.getAttribute("userid"); %>
  var javaGEInstallerURL = '<%= genepatternProperties.getProperties().get("JavaGEInstallerURL") %>';
  var createTaskAllowed = <%= authorizationBean.isCreateModuleAllowed(username) %>;
  var createPipelineAllowed = <%= authorizationBean.isCreatePipelineAllowed(username) %>;
  var createSuiteAllowed = <%= authorizationBean.isCreateSuiteAllowed(username) %>;

      
</script>
<script language="JavaScript" src="<%=request.getContextPath()%>/js/mainMenu.js"></script>
<script language="JavaScript" src="<%=request.getContextPath()%>/js/mm_menu.js"></script>
<script language="JavaScript1.2">MM_preloadImages('<%=request.getContextPath()%>/images/searchicon-1-over.gif','<%=request.getContextPath()%>/images/menuicon-over.gif');</script>


