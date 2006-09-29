
<%
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie c : cookies) {
        if ("userID".equals(c.getName())) {
            c.setMaxAge(0);
            c.setPath(request.getContextPath());
            response.addCookie(c);
            break;
        }
    }
}
request.removeAttribute("userID");
session.invalidate();

%>
<jsp:forward page="/pages/login.jsf"></jsp:forward>

