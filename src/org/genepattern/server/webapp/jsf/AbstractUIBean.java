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
import javax.servlet.http.*;

import org.genepattern.util.GPConstants;

public abstract class AbstractUIBean {

    protected Map getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    protected Map getRequestMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }
    
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    protected ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }

    protected HttpServletRequest getRequest() {
        HttpServletRequest request = (HttpServletRequest) getExternalContext().getRequest();
        return request;
    }

    protected HttpServletResponse getResponse() {
        return (HttpServletResponse) getExternalContext().getResponse();
    }
  
    
    protected void setInfoMessage(String summary) {
        getFacesContext().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }



    protected void setInfoMessage(UIComponent component, String summary) {
        getFacesContext().addMessage(component.getClientId(getFacesContext()), new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null));
    }

    protected void printAttributes() {
        System.out.println("Attributes:");
        Enumeration en = getRequest().getAttributeNames();
        while(en.hasMoreElements()) {
            String name = (String) en.nextElement();
            System.out.print(name + " -> ");
            System.out.println(getRequest().getAttribute(name));
            
        }
    }

    protected void printParameters() {
        System.out.println("Parameters: ");
        Enumeration en = getRequest().getParameterNames();
        while(en.hasMoreElements()) {
            String name  = (String) en.nextElement();
            System.out.print(name + " -> ");
            for(String value : getRequest().getParameterValues(name)) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }



}
