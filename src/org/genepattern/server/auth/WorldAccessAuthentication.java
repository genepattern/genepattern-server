package org.genepattern.server.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WorldAccessAuthentication implements IAuthenticationPlugin {

    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
//        String existing_user = getUserIdFromSession(request);
//        if (existing_user != null) {
//            return existing_user;
//        }
        
        String gp_username = "WORLD_" + System.currentTimeMillis();
        //this.addUserIdToSession(request, gp_username);
        return gp_username;
    }

    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException {
        // Don't allow authentication from SOAP client
        return false;
    }

    public void requestAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // always automatically authenticate using 'world_<timestamp>'
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
    
//    private String getUserIdFromSession(HttpServletRequest request) {
//        if (request == null) {
//            return null;
//        }
//        HttpSession session = request.getSession();
//        if (session == null) {
//            return null;
//        }
//        return (String) session.getAttribute(GPConstants.USERID);
//    }
//    
//    private void addUserIdToSession(HttpServletRequest request, String gp_username) {
//        HttpSession session = request.getSession();
//        if (session == null) {
//            //TODO: log exception
//            return;
//        }
//        session.setAttribute(GPConstants.USERID, gp_username);
//        session.setAttribute("userID", gp_username); //TODO: replace all references to 'userID' with 'userid'
//    }


}
