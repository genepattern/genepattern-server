/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.util.GPConstants;

public class UIBeanHelper {
    private static Logger log = Logger.getLogger(UIBeanHelper.class);

    private UIBeanHelper() {
    }

    public static Map getSessionMap() {
    return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    public static Map getRequestMap() {
    return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }

    public static FacesContext getFacesContext() {
    return FacesContext.getCurrentInstance();
    }
    
    public static ServletContext getServletContext() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        ServletContext servletContext = (ServletContext) externalContext.getContext();
        return servletContext;
    }

    public static ExternalContext getExternalContext() {
    return FacesContext.getCurrentInstance().getExternalContext();
    }

    public static HttpServletRequest getRequest() {
    FacesContext fc = FacesContext.getCurrentInstance();
    return fc != null ? (HttpServletRequest) getExternalContext().getRequest() : null;
    }

    public static HttpSession getSession() {
    return getRequest().getSession();
    }

    public static HttpSession getSession(boolean create) {
    return getRequest().getSession(create);
    }

    public static HttpServletResponse getResponse() {
    return (HttpServletResponse) getExternalContext().getResponse();
    }

    public static Object getManagedBean(String elExpression) {
    return getFacesContext().getApplication().createValueBinding(elExpression).getValue(getFacesContext());
    }

    public static void setInfoMessage(String summary) {
    getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }

    public static void setErrorMessage(String summary) {
    getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null));
    }

    public static void setInfoMessage(UIComponent component, String summary) {
    getFacesContext().addMessage(component.getClientId(getFacesContext()),
        new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }

    public static void printAttributes() {
    System.out.println("Attributes:");
    Enumeration en = getRequest().getAttributeNames();
    while (en.hasMoreElements()) {
        String name = (String) en.nextElement();
        System.out.print(name + " -> ");
        System.out.println(getRequest().getAttribute(name));

    }
    }

    public static void printParameters() {
    System.out.println("Parameters: ");
    Enumeration en = getRequest().getParameterNames();
    while (en.hasMoreElements()) {
        String name = (String) en.nextElement();
        System.out.print(name + " -> ");
        for (String value : getRequest().getParameterValues(name)) {
        System.out.print(value + " ");
        }
        System.out.println();
    }
    }

    public static String getUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }
        Object userid = session.getAttribute(GPConstants.USERID);
        if (userid instanceof String) {
            return (String) userid;
        }
        return null;
    }
    
    public static GpContext getUserContext() {
        return GpContext.getContextForUser(getUserId());
    }

    public static String encode(String s) {
    if (s == null) {
        return null;
    }
    try {
        return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        log.error(e);
        return s;
    }
    }
    
    /**
     * special case to url encode each part of a file path, without url encoding the slash ('/') characters,
     * so that it can be used as a reference to a file served by the data servlet.
     * @param file
     * @return
     */
    public static String encodeFilePath(String filePath) {
        if (filePath == null) {
            return null;
        }
        String encodedPath = UIBeanHelper.encode(filePath);
        encodedPath = encodedPath.replace("+", "%20");
        encodedPath = encodedPath.replace("%2F", "/");
        return encodedPath;
    }

    public static String decode(String s) {
    if (s == null) {
        return null;
    }
    try {
        return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        log.error(e);
        return s;
    }
    }

    /**
     * Gets the GenePatternURL or the server that the request came from. 
     * For example, http://localhost:8080/gp. 
     * Note if the GenePatternURL system property ends with a trailing '/', the slash is removed.
     * 
     * @return The server.
     */
    public static String getServer() {
        //Use GenePatternURL if it is set
        String server = System.getProperty("GenePatternURL", "");
        if (server != null && server.trim().length() > 0) {
            if (server.endsWith("/")) {
                server = server.substring(0, server.length() - 1);
            }
            return server;
        }
        //otherwise use the servlet request
        HttpServletRequest request = UIBeanHelper.getRequest();
        if (request != null) {
            String portStr = "";
            int port = request.getServerPort();
            if (port > 0) {
                portStr = ":"+port;
            }
            return request.getScheme() + "://" + request.getServerName() + portStr + request.getContextPath();
        }
        
        //TODO: handle this exception
        log.error("Invalid servername: GenePatternURL is null and UIBeanHelper.request is null!");
        return "http://localhost:8080/gp";
    }

}
