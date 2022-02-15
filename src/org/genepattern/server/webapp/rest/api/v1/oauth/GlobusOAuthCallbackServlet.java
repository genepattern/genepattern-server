package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Servlet implementation class GlobusOAuthCallbackServlet
 */
public class GlobusOAuthCallbackServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GlobusOAuthCallbackServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		// TODO Auto-generated method stub
	    final GpConfig gpConfig=ServerConfigurationFactory.instance();
	      
	    GlobusClient globusClient = new GlobusClient();
	    try {
            // Login to globus.  Get an access_token.  Throws an exception if login fails for any reason
            globusClient.login(servletRequest);

            // If the forward parameter is set, forward there
            String urlTargetPostLogin = servletRequest.getParameter("forward");

            // Attach the Globus payload if the forward parameter is defined
            if (urlTargetPostLogin != null) {
                String globusPayload = globusClient.jsonPayload(servletRequest);
                urlTargetPostLogin += "?globus=" + URLEncoder.encode(globusPayload, "UTF-8");
            }

            // If not, forward to the origin
            if (urlTargetPostLogin == null) {
                urlTargetPostLogin = (String) servletRequest.getSession().getAttribute("origin");
            }

            // If that doesn't work, forward to the base GenePattern URL
            if (urlTargetPostLogin == null) {
                urlTargetPostLogin  = gpConfig.getGenePatternURL().toString();
            }

            servletResponse.sendRedirect(urlTargetPostLogin);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new ServletException(e);
        }
	}
  
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
