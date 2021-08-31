package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.util.LSID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet implementation class GlobusTransferInServlet
 */
public class GlobusTransferInServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	public static String transferAPIBaseUrl = "https://transfer.api.globusonline.org/v0.10" ;
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GlobusTransferInServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		GlobusClient globusClient = new GlobusClient();
		 
		try {
			// per https://docs.globus.org/api/helper-pages/browse-endpoint/#response
    		// we should receive endpoint_id, path, folder[0..n] rel to path, file[0..n] rel to path, label
    		// for now it should be a single file
    		
            Enumeration<String> e = request.getParameterNames();
            while (e.hasMoreElements()){
                String key = (String)e.nextElement();
                String val = request.getParameter(key);
                System.out.println("==> " + key + " = " + val);
                
            }
    		String endpointId = null;
    		String path = null;
    		String file = null;
    		String gp_user_id = null;
    		try {
    		    endpointId = request.getParameter("endpoint_id");
    		    path = request.getParameter("path");
    		    file = request.getParameter("file[0]");
    		    gp_user_id = request.getParameter("gp_user_id");
    		} catch (Exception ex){
    		    ex.printStackTrace();
    		}
    		
    		
    		// this was set on login
    		// request.getSession().getServletContext().setAttribute("globus_session_"+userId, request.getSession());
    		HttpSession oldSession = (HttpSession) request.getSession().getServletContext().getAttribute("globus_session_"+gp_user_id);
    		if (oldSession != null){
    		    Enumeration names = oldSession.getAttributeNames();
    		    while (names.hasMoreElements()){
    		        String name = (String)names.nextElement();
    		        Object val = oldSession.getAttribute(name);
    		        request.getSession().setAttribute(name, val);
    		        
    		    }
    		    // now we have moved to this session
    		    request.getSession().getServletContext().setAttribute("globus_session_"+gp_user_id, request.getSession());
                
    		}
    		
    		
    		if ((endpointId == null)||(path==null)||(file==null)){
    		    // user probably hit the cancel button
    		    // so nothing to do here but go to the 
    		    // transfer complete page which itself does
    		    // nothing but close the extra window we opened
    		    // for the Globus UI 
    		    response.sendRedirect("/gp/GlobusTransferComplete.html");
    		    return;
    		}
    		
    		
     		try {
                String taskId = globusClient.startGlobusFileTransfer(request, endpointId, path, file);
                
                // a new thread is automatically started to poll for completion
                
                // now the transfer is complete, move the file from the globus drop 
                // to the user's top level directory, and push to an external File Manager
                // if appropriate 
                // XXX TODO: allow the user to specify where to put the files
                   
            }  catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                response.getWriter().append("\nERROR "+ e1.getMessage());
                return;
            }
		} catch (Exception ex){
		    ex.printStackTrace();
		    response.getWriter().append("\nERROR ").append(ex.getMessage());
		}
		
		// redirect to a page to close the popup and call the parent window to tell it to look
		// for the new file to appear in the user's files tab
		response.sendRedirect("/gp/GlobusTransferComplete.html");
	}

 

    
    

    
 
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
