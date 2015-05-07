/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth.plugin.jopenid;


import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.expressme.openid.Association;
import org.expressme.openid.Authentication;
import org.expressme.openid.Endpoint;
import org.expressme.openid.OpenIdException;
import org.expressme.openid.OpenIdManager;

/**
 * This servlet handles authentication with OpenID providers.
 * <b>Important note</b>: This is demo code. Do thorough testing before deploying in a production environment.
 * 
 * Based on example source code provided by the JOpenID project (http://code.google.com/p/jopenid/).
 * 
 * @author pcarr
 */
public class JOpenIdAuthenticationServlet extends HttpServlet {
    //TODO: customize this for your server
    private static final String RETURN_TO = "http://127.0.0.1:8080/gp/openId";

    private static final long ONE_HOUR = 3600000L;
    private static final long TWO_HOUR = ONE_HOUR * 2L;
    public static final String ATTR_MAC = "openid_mac";
    private static final DateFormat nonceDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    OpenIdManager manager;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        manager = new OpenIdManager();
        manager.setReturnTo(RETURN_TO);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String op = request.getParameter("op");
        if (op==null) {
            handleSignOnCallback(request, response);
        }
        else if ("Google".equals(op)) {
            redirectToGoogle(request, response);
        }
        // Yahoo not implemented
        //else if ("Yahoo".equals(op)) {
        //    redirectToYahoo(request, response);
        //}
        else {
            throw new ServletException("Bad parameter op=" + op);
        }
    }
    
    /**
     * Redirect to OpenID provider sign on page.
     * 
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void redirectToGoogle(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Endpoint endpoint = manager.lookupEndpoint("Google");
        Association association = manager.lookupAssociation(endpoint);
        request.getSession().setAttribute(ATTR_MAC, association.getRawMacKey());
        String url = manager.getAuthenticationUrl(endpoint, association);
        response.sendRedirect(url);
    }
    
    private void handleSignOnCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // check nonce:
            checkNonce(request.getParameter("openid.response_nonce"));
            // get authentication:
            byte[] mac_key = (byte[]) request.getSession().getAttribute(ATTR_MAC);
            Authentication authentication = manager.getAuthentication(request, mac_key);
            String identity = authentication.getIdentity();
            String email = authentication.getEmail();
            
            // add these attributes to the session to ...
            // 1) indicate successful login
            request.getSession().setAttribute("jopenid.identity", identity);
            // 2) set the email address to be used when creating a new GenePattern account
            request.getSession().setAttribute("jopenid.email", email);
            
            //redirect to the originally requested page
            String targetURL = (String) request.getSession().getAttribute("origin");
            if (targetURL == null) {
                targetURL = "/gp";
            }
            response.sendRedirect(targetURL);
        }
        catch (Exception e) {
            //for debugging only, turn this off before deploying to production
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    private void checkNonce(String nonce) {
        // check response_nonce to prevent replay-attack:
        if (nonce==null || nonce.length()<20) {
            throw new OpenIdException("Verify failed.");
        }
        long nonceTime = getNonceTime(nonce);
        long diff = System.currentTimeMillis() - nonceTime;
        if (diff < 0) {
            diff = (-diff);
        }
        if (diff > ONE_HOUR) {
            throw new OpenIdException("Bad nonce time.");
        }
        if (isNonceExist(nonce)) {
            throw new OpenIdException("Verify nonce failed.");
        }
        storeNonce(nonce, nonceTime + TWO_HOUR);
    }

    private boolean isNonceExist(String nonce) {
        // TODO: check if nonce is in database:
        return false;
    }

    private void storeNonce(String nonce, long expires) {
        // TODO: store nonce in database:
    }

    private long getNonceTime(String nonce) {
        try {
            return nonceDateFormat.parse(nonce.substring(0, 19) + "+0000")
                    .getTime();
        }
        catch(ParseException e) {
            throw new OpenIdException("Bad nonce time.");
        }
    }

}