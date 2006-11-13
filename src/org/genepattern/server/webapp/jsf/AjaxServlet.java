package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.el.ValueBinding;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class for Servlet: SnpViewerServlet
 * 
 */
public class AjaxServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public AjaxServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        // TODO Auto-generated method stub
        System.out.println("AjaxServlet initialized");
        super.init();
    }

    /*
     * (non-Java-doc)
     * 
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map parameters = request.getParameterMap();
        Enumeration params = request.getParameterNames();
        System.out.println("Get");

        while (params.hasMoreElements()) {
            String name = (String) params.nextElement();
            Object value = parameters.get(name);
            System.out.println(name + " -> " + parameters.get(name));
        }
    }

    /**
     * 
     * 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

        Map parameterMap = request.getParameterMap();
        String elExpression = ((String[]) parameterMap.get("el"))[0];

        LifecycleFactory lcFactory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        Lifecycle lifecycle = lcFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);

        FacesContextFactory factory = (FacesContextFactory) FactoryFinder
                .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        FacesContext fc = factory.getFacesContext(getServletContext(), request, response, lifecycle);


        Object value = fc.getApplication().createValueBinding("#{" + elExpression + "}").getValue(fc);

        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache");
        if (value != null) {
            response.getWriter().write(value.toString());
        }
        response.getWriter().flush();
        response.getWriter().close();

    }
}