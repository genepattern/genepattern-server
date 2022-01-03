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
import java.util.ArrayList;
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
import org.genepattern.server.dm.UrlUtil;
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
		
	    // gp_action=sendToGlobus
	    boolean isSendToGlobus = "sendToGlobus".equalsIgnoreCase(request.getParameter("gp_action"));
	    if (isSendToGlobus) outwardTransferToGlobus(request, response);
	    else  	    inboundTransferFromGlobus(request, response);
	    return;
	    
	}

protected void inboundTransferFromGlobus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
    response.getWriter().append("Served at: ").append(request.getContextPath());
    GlobusClient globusClient = new GlobusClient();
    ArrayList<String> files = new ArrayList<String>();
    String destDir = null; // comes in as directory URL or absent
    String submissionId = null;
    try {
        // per https://docs.globus.org/api/helper-pages/browse-endpoint/#response
        // we should receive endpoint_id, path, folder[0..n] rel to path, file[0..n] rel to path, label
        // for now it should be a single file
        String endpointId = null;
        String path = null;
        String gp_user_id = null;
        String gp_session_id = null;
        String label = "Transfer Into GenePattern";
        try {
            endpointId = request.getParameter("endpoint_id");
            path = request.getParameter("path");
            destDir = request.getParameter("destDir");
            label = request.getParameter("label");
             
            Enumeration<String> e = (Enumeration<String>)request.getParameterNames();
            
            while (e.hasMoreElements()){
                String key = (String)e.nextElement();
                String val = request.getParameter(key);
                if (key.startsWith("file[")){
                    files.add(val);
                }
            }
            gp_user_id = request.getParameter("gp_username");
            gp_session_id = request.getParameter("gp_session_id");
        } catch (Exception ex){
            ex.printStackTrace();
        }
        
        reestablishGenepatternSession(request, gp_user_id, gp_session_id);
        
        
        if ((endpointId == null)||(path==null)||(files.size()==0)){
            // user probably hit the cancel button
            // so nothing to do here but go to the 
            // transfer complete page which itself does
            // nothing but close the extra window we opened
            // for the Globus UI 
            response.sendRedirect("/gp/GlobusTransferComplete.html");
            return;
        }
        
        try {
            for (int i=0; i<files.size();i++){
                submissionId = globusClient.startGlobusFileTransfer(request, endpointId, path, files.get(i), destDir, label);
            }
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
    if (files.size() > 0){
        response.sendRedirect("/gp/GlobusTransferComplete.html?submissionId="+submissionId +"&file="+UrlUtil.encodeURIcomponent(files.get(0))+"&destDir="+UrlUtil.encodeURIcomponent(destDir)+"&direction=inbound");
    } else {
        response.sendRedirect("/gp/GlobusTransferComplete.html");
    }
}


    
protected void outwardTransferToGlobus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    
    response.getWriter().append("Served at: ").append(request.getContextPath());
    GlobusClient globusClient = new GlobusClient();
    String fileToTransfer = null; // comes in as directory URL or absent
    String submissionId = null;
    String path = null;
    try {
        // per https://docs.globus.org/api/helper-pages/browse-endpoint/#response
        // we should receive endpoint_id, path, folder[0..n] rel to path, file[0..n] rel to path, label
        // for now it should be a single file
        String endpointId = null;
        
        String gp_user_id = null;
        String gp_session_id = null;
        String label = "From GenePattern";
        try {
            endpointId = request.getParameter("endpoint_id");
            path = request.getParameter("path");
            fileToTransfer = request.getParameter("gp_file");
            gp_user_id = request.getParameter("gp_username");
            gp_session_id = request.getParameter("gp_session_id");
            label = request.getParameter("label");
            
        } catch (Exception ex){
            ex.printStackTrace();
        }
        
        reestablishGenepatternSession(request, gp_user_id, gp_session_id);
        
        
        if ((endpointId == null)||(path==null)||(fileToTransfer == null)){
            // user probably hit the cancel button
            // so nothing to do here but go to the 
            // transfer complete page which itself does
            // nothing but close the extra window we opened
            // for the Globus UI 
            response.sendRedirect("/gp/GlobusTransferComplete.html");
            return;
        }
        
        
        try {
           
           submissionId = globusClient.transferFileToGlobus(request, endpointId, path, fileToTransfer, label);
  
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
    
    // pull the filename out of the file URL we were given
    int beginIndex = fileToTransfer.lastIndexOf("/")+1;
    String filename = fileToTransfer.substring(beginIndex);
    
    // redirect to a page to close the popup and call the parent window to tell it to look
    // for the new file to appear in the user's files tab
    response.sendRedirect("/gp/GlobusTransferComplete.html?submissionId="+submissionId +"&file="+UrlUtil.encodeURIcomponent(filename)+"&destDir="+UrlUtil.encodeURIcomponent(path)+"&direction=outbound");
    
}

private void reestablishGenepatternSession(HttpServletRequest request, String gp_user_id, String gp_session_id) throws Exception {
    // this was set before going to globus so we can identify the user in its response which will not have our cookies
    HttpSession oldSession = (HttpSession) request.getSession().getServletContext().getAttribute("globus_session_"+gp_user_id);
    String oldSessionId = (String) request.getSession().getServletContext().getAttribute("globus_session_id_"+gp_user_id);
    if (oldSession != null){
        if (oldSessionId.equals(gp_session_id)){
            Enumeration names = oldSession.getAttributeNames();
            while (names.hasMoreElements()){
                String name = (String)names.nextElement();
                Object val = oldSession.getAttribute(name);
                request.getSession().setAttribute(name, val);
                
            }
            // now we have moved to this session
            request.getSession().getServletContext().setAttribute("globus_session_"+gp_user_id, request.getSession());
            request.getSession().getServletContext().setAttribute("globus_session_id_"+gp_user_id, request.getSession().getId());
            
        }
    } else {
        throw new Exception("Could not establish the GenePattern user session for this Globus file transfer.");
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
