package org.genepattern.server.webapp.rest.api.v1.oauth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.genepattern.server.webapp.rest.api.v1.oauth.OAuthConstants;

/**
 * Servlet implementation class GlobusTestServlet
 */
public class GlobusTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GlobusTestServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
		
		// Do a test of calling globus to get some information from it using the access token we got at login
		//  We got it with 
		String urlBase = "https://transfer.api.globusonline.org/v0.10";
		String accessToken = (String)request.getSession().getAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY) ;
		String authHeaderValue = "Bearer " + accessToken;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		/**
		 * First list any existing tasks (transfers)
		 */
        HttpURLConnection con1 = (HttpURLConnection) (new URL(urlBase+"/task_list")).openConnection();
        con1.setRequestProperty("Authorization", authHeaderValue);         
        con1.setRequestMethod("GET");
        con1.setRequestProperty("Content-Type", "application/json; utf-8");    
  		response.getWriter().append("\n");
		response.getWriter().append(gson.toJson(getJsonResponse(con1)));
		response.getWriter().flush();
		
		/**
		 * Now lets see what endpoints are available for me
		 * 
		 */
		 HttpURLConnection con2 = (HttpURLConnection) (new URL(urlBase+"/endpoint_search?filter_scope=all&filter_fulltext=tutorial")).openConnection();
         con2.setRequestProperty("Authorization", authHeaderValue);         
         con2.setRequestMethod("GET");
         con2.setRequestProperty("Content-Type", "application/json; utf-8");    
         response.getWriter().append("\n");
         JsonElement endpoints = getJsonResponse(con2);
         response.getWriter().append(gson.toJson( endpoints));
	     response.getWriter().flush();
         
         
         
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

	
	private JsonElement getJsonResponse(HttpURLConnection con) throws UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
        StringBuilder response = new StringBuilder();
        String responseLine = null;
        System.out.println("----------------");
        while ((responseLine = br.readLine()) != null) {
              response.append(responseLine.trim());
              System.out.println(responseLine);
        }
        System.out.println("----------------");
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(response.toString());
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        
        return je;
    }
	
	
	
}
