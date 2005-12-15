<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


<%@ page import="org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.webapp.Base64Code,
		 java.net.URLEncoder,
		 java.net.InetAddress,
		 java.io.IOException"
	session="false" contentType="text/html" language="Java" info="login" %><%


response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String REFERRER = "referrer";
String userID = request.getParameter(GPConstants.USERID);
String referrer = request.getParameter(REFERRER);
if (referrer == null) referrer = request.getContextPath()+"/index.jsp";

// handle base64 encoding request from R client
if (userID != null && userID.length() > 0 && request.getParameter("getBasicAuthentication") != null) {
	String password = ""; // not used
	out.print(Base64Code.encode(userID + ":" + password));
	return;
}

// redirect to the fully-qualified host name to make sure that the one cookie that we are allowed to write is useful
try {
	String fqHostName = System.getProperty("fullyQualifiedHostName");
	if (fqHostName == null) fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
	if (fqHostName.equals("localhost")) fqHostName = "127.0.0.1";
	String serverName = request.getServerName();
	if (!fqHostName.equalsIgnoreCase(serverName)) {
		String queryString = request.getQueryString();
		if (queryString == null) {
			queryString = "";
			} else {
				queryString = "?" + queryString;
			}
		String fqAddress = request.getScheme() + "://" + fqHostName + ":" + request.getServerPort() + request.getRequestURI() + queryString;

		response.sendRedirect(fqAddress);
		return;
	}
} catch (IOException ioe) {
	ioe.printStackTrace();
}

if (userID != null && userID.length() > 0) {
	userID = "\"" + URLEncoder.encode(GenePatternAnalysisTask.replace(userID, "\"", "\\\""), "utf-8") + "\"";
	addUserIDCookies(response, request, userID);
	//request.getRequestDispatcher(referrer).forward(request, response);
	response.sendRedirect(referrer);
	return;
} else {
	addUserIDCookies(response, request, "");
}
// set request attribute to indicate to getUserID (in navbar) that user has signed out as of now,
// even though the request has a cookie indicating the user [was] still signed in.
request.setAttribute(GPConstants.USER_LOGGED_OFF, "1");

String origin = request.getParameter("origin");
if (origin == null) origin = "";
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern sign-in</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
</head>
<body onload=sf()>
<jsp:include page="navbar.jsp"></jsp:include>
<form name=loginForm target="_top">
Please enter your username to identify task ownership.  We recommend using your email address to ensure uniqueness, memorability, and consistency.<br>
Username: <input name=<%= GPConstants.USERID %> size="50">
<input type="hidden" name="<%= REFERRER %>" value="<%= origin %>">
<input type="submit" name="submit" value="sign in" class="little">

</form>
<script type="text/javascript">
function sf(){document.loginForm.userid.focus();}

</script>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<%! void addUserIDCookies(HttpServletResponse response, HttpServletRequest request, String userID) {
	// no explicit domain
	Cookie cookie4 = new Cookie(GPConstants.USERID, userID);
	cookie4.setPath(request.getContextPath());
	cookie4.setMaxAge(Integer.MAX_VALUE);
	response.addCookie(cookie4);
}
%>