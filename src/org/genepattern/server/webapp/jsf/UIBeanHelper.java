package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.util.GPConstants;

public class UIBeanHelper {

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

    public static ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }

    public static HttpServletRequest getRequest() {
        HttpServletRequest request = (HttpServletRequest) getExternalContext().getRequest();
        return request;
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

    public static String getReferrer(HttpServletRequest request) {
        String referrer = (String) request.getSession().getAttribute("origin");
        request.getSession().removeAttribute("origin");
        if (referrer == null || referrer.length() == 0) {
            referrer = request.getContextPath() + "/index.jsp";
        }
        return referrer;

    }

    public static String getUserId() {
        return (String) getRequest().getAttribute("userID");
    }

    public static boolean isLoggedIn() {
        return getUserId() != null;
    }

    public static void logout() {
        Cookie[] cookies = UIBeanHelper.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("userID".equals(c.getName())) {
                    c.setMaxAge(0);
                    c.setPath(getRequest().getContextPath());
                    UIBeanHelper.getResponse().addCookie(c);
                    break;
                }
            }
        }
        UIBeanHelper.getRequest().removeAttribute("userID");
        UIBeanHelper.getSession().invalidate();
    }

    /**
     * 
     * @param request
     * @param response
     * @param username
     * @param sessionOnly
     *            whether the login cookie should be set for the session only
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static void setUserAndRedirect(String username, boolean sessionOnly) throws UnsupportedEncodingException,
            IOException {

        Cookie cookie = new Cookie("userID", username);
        cookie.setPath(getRequest().getContextPath());
        if (!sessionOnly) {
            cookie.setMaxAge(Integer.MAX_VALUE);
        }
        UIBeanHelper.getResponse().addCookie(cookie);
        String referrer = UIBeanHelper.getReferrer(getRequest());
        UIBeanHelper.getResponse().sendRedirect(referrer);
    }

}
