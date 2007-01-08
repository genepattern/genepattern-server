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


<%@ page import="org.genepattern.server.user.User,
		 org.genepattern.server.user.UserDAO,
		 org.genepattern.util.GPConstants"
	session="true" contentType="text/html" language="Java" %><% 
	
	User user = new UserDAO().findById((String)request.getAttribute(GPConstants.USERID));
    assert user != null;
    user.setSessionId(null);
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (Cookie c : cookies) {
            if (GPConstants.USERID.equals(c.getName())) {
                c.setMaxAge(0);
                c.setPath(request.getContextPath());
                response.addCookie(c);
                break;
            }
        }
    }
    request.removeAttribute(GPConstants.USERID);
    request.removeAttribute("userID");
    session.invalidate();

%>
<html>
<jsp:forward page="/pages/login.jsf" /> 
</html>
