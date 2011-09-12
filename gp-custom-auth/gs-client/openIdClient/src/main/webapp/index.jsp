<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>GenomeSpace Authentication Provider</title>
    </head>
    <body>
        <h3>GenomeSpace Authentication Provider</h3>
        <%
        String cookieName = "gs-token";
        Cookie cookies [] = request.getCookies ();
        Cookie myCookie = null;
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies [i].getName().equals(cookieName)) {
                    myCookie = cookies[i];
                    break;
                }
            }
        }
        if (myCookie != null && myCookie.getValue() != null) {
                %>
                <em>GenomeSpace login token was found: <%= myCookie.getValue() %></em>
                <p/>Click for <a href="openIdClient?logout=true">OpenId Logout.</a>
                <%
        } else {
                %>
                <p/>Click for <a href="openIdClient">OpenId Login.</a>
                <%
        }
    %>
    </body>
</html>
