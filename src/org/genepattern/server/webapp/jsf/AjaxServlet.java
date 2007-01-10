package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.util.EmailNotificationManager;

/**
 * Servlet implementation class for Servlet: SnpViewerServlet
 * 
 */
public class AjaxServlet extends javax.servlet.http.HttpServlet implements
        javax.servlet.Servlet {
    private static Logger log = Logger.getLogger(AjaxServlet.class);

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
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String elExpression = request.getParameter("el");
            executeMethod(elExpression, request, response);
        } catch (Throwable t) {
            log.error(t);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Map parameterMap = request.getParameterMap();
            String[] elExpression = (String[]) parameterMap.get("el");
	      if (elExpression != null){
	            executeMethod(elExpression[0], request, response);
		} else {
			executeCommand(parameterMap, request, response);
		}
        } catch (Throwable t) {
            log.error(t);
        }
    }

    protected void executeMethod(String elExpression,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        LifecycleFactory lcFactory = (LifecycleFactory) FactoryFinder
                .getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        Lifecycle lifecycle = lcFactory
                .getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);

        FacesContextFactory factory = (FacesContextFactory) FactoryFinder
                .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        FacesContext fc = factory.getFacesContext(getServletContext(), request,
                response, lifecycle);
        elExpression = "#{" + elExpression + "}";
        Object value = fc.getApplication().createValueBinding(elExpression)
                .getValue(fc);
        
        log.info("Executing: " + elExpression);

        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache");
        if (value != null) {
            response.getWriter().write(value.toString());
        }
        response.getWriter().flush();
        response.getWriter().close();

    }

	/**
	 * execute some command that is not JSF EL
	 */

	protected void executeCommand(Map parameterMap, HttpServletRequest request, HttpServletResponse response) throws Exception{
	  	
		String cmd = ((String[]) parameterMap.get("cmd"))[0];
		String value= ""; // nothing to return

		if ("notifyEmailJobCompletion".equalsIgnoreCase(cmd)){
			// setup for polling, put any return into value
			String jobID = ((String[]) parameterMap.get("jobID"))[0];
			String userID= ((String[]) parameterMap.get("userID"))[0];
			EmailNotificationManager.getInstance().addWaitingUser(userID, jobID);
		} else if ("cancelEmailJobCompletion".equalsIgnoreCase(cmd)){
			String jobID = ((String[]) parameterMap.get("jobID"))[0];
			String userID= ((String[]) parameterMap.get("userID"))[0]; 		
			EmailNotificationManager.getInstance().removeWaitingUser(userID, jobID);
		}
		
		
 		response.setContentType("text/html");
       	response.setHeader("Cache-Control", "no-cache");
       	if (value != null) {
           	response.getWriter().write(value);
        }
        response.getWriter().flush();
        response.getWriter().close();
	}


}