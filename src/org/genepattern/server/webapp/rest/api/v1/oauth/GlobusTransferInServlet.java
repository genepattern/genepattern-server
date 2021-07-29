package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
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

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

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
		 
		try {
		final GpConfig gpConfig=ServerConfigurationFactory.instance();
        GpContext context = GpContext.getServerContext();
		
		// per https://docs.globus.org/api/helper-pages/browse-endpoint/#response
		// we should receive endpoint_id, path, folder[0..n] rel to path, file[0..n] rel to path, label
		// for now it should be a single file
		
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()){
            String key = (String)e.nextElement();
            String val = request.getParameter(key);
            System.out.println("==> " + key + " = " + val);
            
        }
		String endpointId = null;
		String path = null;
		String file = null;
		
		String label = null;
        try {
		 endpointId = request.getParameter("endpoint_id");
         path = request.getParameter("path");
         file = request.getParameter("file[0]");
         label = request.getParameter("label");
        
         
         } catch (Exception ex){
            ex.printStackTrace();
        }
		
		
		try {
            startGlobusFileTransfer(request, gpConfig, context, endpointId, path, file);
        }
        catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            response.getWriter().append("\nERROR "+ e1.getMessage());
            return;
        }
		} catch (Exception ex){
		    ex.printStackTrace();
		    response.getWriter().append("\nERROR ").append(ex.getMessage());
		}
		
		// XXX TODO need to have a redirect to somewhere in GP
		response.getWriter().append("\nDONE ").append(request.getContextPath());
		
	}

    private void startGlobusFileTransfer(HttpServletRequest request,  final GpConfig gpConfig, GpContext context, String endpointId, String path, String file) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        // we also need the token for making the transfer call
		String transferToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY);
		String accessToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY);
        String user_id = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_USER_ID_ATTR_KEY);
	       
		// and finally we need to know where to put this in the user's GP file system.
		// for now we will drop it in their top level directory after the globus transfer
		// XXX TODO: allow the user to specify where to put the files
		
		// so to transfer in we need to know our endpoint ID
		String myEndpointID = gpConfig.getGPProperty(context, OAuthConstants.OAUTH_LOCAL_ENPOINT_ID, "eb7230ac-d467-11eb-9b44-47c0f9282fb8");
        String submissionId = getSubmissionId(transferToken);

        JsonObject transferObject = new JsonObject();
        transferObject.addProperty("DATA_TYPE", "transfer");
        transferObject.addProperty("submission_id", submissionId);
        transferObject.addProperty("source_endpoint", endpointId);
        transferObject.addProperty("destination_endpoint", myEndpointID);
        
        JsonObject transferItem = new JsonObject();
        transferItem.addProperty("DATA_TYPE", "transfer_item");
        transferItem.addProperty("recursive", false);
        transferItem.addProperty("verify_checksum", true);
        transferItem.addProperty("source_path", path + file);
        transferItem.addProperty("destination_path", "genepattern/users/"+ user_id +"/globus/"+file);
       
        transferObject.add("DATA", transferItem);
        
        URL url = new URL(transferAPIBaseUrl+"/transfer");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization","Bearer "+ transferToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = transferObject.toString().getBytes("utf-8");
            os.write(input, 0, input.length);           
        }
        
        JsonElement transferResponse =  getJsonResponse(connection);
        String taskId = transferResponse.getAsJsonObject().getAsJsonPrimitive("task_id").getAsString();
        
        System.out.println("Transfer response: " + taskId + " --  " + transferResponse.getAsString());
        
        
        // wait to see if the transfer is complete
        String status = checkTransferStatus(taskId, transferToken);
        while ("ACTIVE".equals(status)){
            Thread.currentThread().sleep(10000);
            status = checkTransferStatus(taskId, transferToken);
        }
        
        
    }

    private String getSubmissionId(String transferToken) throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(transferAPIBaseUrl+"/submission_id");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    
		connection.setRequestProperty("Authorization","Bearer "+ transferToken);
		connection.setRequestMethod("GET");
		//connection.setRequestProperty("Accept", "application/json");
		
		JsonElement submissionResponse =  getJsonResponse(connection);
		String dataType = submissionResponse.getAsJsonObject().get("DATA_TYPE").getAsString();
		
		if (!"submission_id".equals(dataType)){
		    throw new IOException("Error getting submission ID.  Got a '"+ dataType + "' but expected a 'submissionId' "+ submissionResponse.getAsShort());
		}
		
		return submissionResponse.getAsJsonObject().get("value").getAsString();
    }

    private JsonElement getJsonResponse(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim());
        }
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(response.toString());
        return je;
    }

    
    public String checkTransferStatus(String taskId, String transferToken) throws IOException{
        String status = "ACTIVE";
        URL url = new URL(transferAPIBaseUrl+"/task/"+taskId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization","Bearer "+ transferToken);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setReadTimeout(20000);
        
        JsonElement submissionResponse =  getJsonResponse(connection);
        
        String transferStatus = submissionResponse.getAsJsonObject().get("status").getAsString();
        
        return transferStatus;
    }
    
    
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
