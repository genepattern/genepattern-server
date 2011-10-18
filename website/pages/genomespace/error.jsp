<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevent caching at the proxy server
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link href="../../skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="../../skin/favicon.ico" rel="shortcut icon">
<title>GenomeSpace OpenId Error</title>
<style>
TD.little { font-size: 9pt }
</style>
<jsp:include page="../../navbarHead.jsp"/>
</head>
<body>
<jsp:include page="../../navbar.jsp"/>
        <h1>GenomeSpace OpenId Error</h1>
        <%
		String message = (String) request.getSession().getAttribute("gsOIcClientMessage");
        String token = (String) request.getSession().getAttribute("gs-token");
        String username = (String) request.getSession().getAttribute("gs-username");

        if (message != null && message.length() > 0) {
            %>
            <b><%= message %></b>
            <p/>
            <%
        }
        if (token == null || token.length() == 0) {
            %>
            <em>Not logged in.</em>
            <%
        } else {
            %>
            <em>User <%= username %> is logged in.</em>
            <%
        }

        %>
<br>
<jsp:include page="../../footer.jsp"/>
</body>
</html>
