package org.genepattern.server.webapp.jsf;

import java.util.Enumeration;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public static HttpServletResponse getResponse() {
        return (HttpServletResponse) getExternalContext().getResponse();
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
        String referrer = request.getParameter("referrer");
        if (referrer == null || referrer.length() == 0) {
            referrer = request.getContextPath() + "/index.jsp";
        }
        return referrer;

    }

    public static String getUserId() {
        return (String) getRequest().getAttribute("userID");
    }

}
