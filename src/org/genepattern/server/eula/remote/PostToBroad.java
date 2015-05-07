/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula.remote;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.InitException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Remote record of EULA by posting to the Broad server. Here is the draft spec for the remote service.
 * 
 * Public url: http://gpeulas.broadinstitute.org/eulas/
 * Private url: http://vgweb01.broadinstitute.org:3000/eulas/ (behind firewall)
 * 
<pre>
HTTP POST http://vgweb01.broadinstitute.org:3000/eulas/
    { "eula"=>{
          "gp_user_id" => "<user>",       //required
          "task_lsid" => "<lsid>",        //required 
          "task_name" => "<moduleName>",  //optional
          "email" => "<email>",           //optional
          "gp_url" => "<GenePatternURL>"  //optional
      }
    }
</pre>
  * For testing, here is one way to POST directly to the remote server, from the command line. Thanks, Jon.
  * Use rest-client, which is a ruby gem that does HTTP requests. If you have Ruby and rubygems installed, 
  * all you need to do is type "gem install rest-client" from the command line to get it. 
  * Then go into the ruby console (type "irb") and enter these 2 commands:
<pre>
ruby > require "rest-client"
ruby > response = RestClient.post 
    "http://vgpweb01.broadinstitute.org:3000/eulas", 
    {"eula"=>{"gp_user_id" => "bistline", 
              "task_lsid" => "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00270:2", 
              "task_name" => "ssGSEAProjection", '
              "email" => "bistline@broadinstitute.org", 
              "gp_url" => "http://127.0.0.1:8080/gp"}}, 
    :content_type => :json, :accept => :json
</pre>    
 * 
 * @author pcarr
 */
public class PostToBroad {
    final static private Logger log = Logger.getLogger(PostToBroad.class);
    
    /**
     * The remote server responded with an HTTP response code indicating that it failed to 
     * record the eula.
     * 
     * Use the following response codes to indicate success: 200, 201, or 422.
     * 
     * @author pcarr
     */
    static class PostException extends Exception {
        private int statusCode;
        private String reason;

        public PostException(StatusLine status) {
            super("HTTP "+status.getStatusCode()+": "+status.getReasonPhrase());
            this.statusCode=status.getStatusCode();
            this.reason=status.getReasonPhrase();
        }
        
        public int getStatusCode() {
            return statusCode;
        }

        public String getReason() {
            return reason;
        }
    }
    
    private static boolean isSet(final String str) {
        if (str==null) {
            return false;
        }
        if (str.length()==0) {
            return false;
        }
        return true;
    }

    private String remoteUrl=null;
    private String gpUrl=null;
    private String gp_user_id=null;
    private String email=null;
    private EulaInfo eula=null;
    
    /**
     * The URL of the service to where we POST the eula agreement.
     * By default, it is hosted at the Broad Institute.
     * 
     * @param url
     */
    public void setRemoteUrl(final String url) {
        this.remoteUrl=url;
    }
    
    /**
     * the URL of this gp server, it's stored as a column in the remote DB
     * @param url
     */
    public void setGpUrl(final String url) {
        this.gpUrl=url;
    }

    /**
     * The GenePattern userId of the user who accepted the EULA.
     * 
     * @param gpUserId
     */
    public void setGpUserId(final String gpUserId) {
        if (!isSet(gpUserId)) {
            throw new IllegalArgumentException("userId not set: "+gpUserId);
        }
        this.gp_user_id=gpUserId;
    }

    /**
     * Optionally, set the email address of the user who accepted the EULA.
     * 
     * @param email
     */
    public void setEmail(final String email) {
        this.email=email;
    }

    /**
     * The eula details, which must include a valid moduleLsid.
     * Optionally, send the moduleName.
     * 
     * @param eulaInfo
     */
    public void setEulaInfo(final EulaInfo eulaInfo) {
        if (eulaInfo==null) {
            throw new IllegalArgumentException("eulaInfo==null");
        }
        this.eula=eulaInfo;
    }
    
    private String getJson() throws JSONException {
        Map<String,String> map=new HashMap<String,String>();
        map.put("gp_user_id", gp_user_id);
        map.put("task_lsid", eula.getModuleLsid());
        if (isSet(eula.getModuleName())) {
            map.put("task_name", eula.getModuleName());
        }
        if (isSet(email)) {
            map.put("email", email);
        }
        if (!isSet(gpUrl)) {
            log.error("gp_url is not initialized");
        }
        else {
            map.put("gp_url", gpUrl);
        }
        JSONObject eulaJson=new JSONObject();
        eulaJson.put("eula", map);
        return eulaJson.toString();
    }
    
    /**
     * POST the eula to the remote server. Use Apache HttpComponents library (http://hc.apache.org/).
     * 
     * @throws InitException, when there are initialization errors in the GP server before POSTing to remote server
     * @throws IOException, when there are IO errors during the POST to the remote server
     * @throws PostException, if the remote server returns an error
     */
    public void doPost() throws InitException, IOException, PostException {  
        if (!isSet(remoteUrl)) {
            throw new InitException("remoteUrl not set");
        }
        if (!isSet(gp_user_id)) {
            throw new InitException("gp_user_id not set");
        }
        if (!isSet(eula.getModuleLsid())) {
            throw new InitException("task_lsid not set");
        }
        if (!isSet(gpUrl)) {
            throw new InitException("gp_url is not set");
        }
        String json=null;
        try {
            json=getJson();
        }
        catch (JSONException e) {
            log.error(e);
            throw new InitException("Internal error saving to json: "+e.getLocalizedMessage());
        }
        StringEntity entity=null; 
        try {
            entity = new StringEntity(json, HTTP.UTF_8);
        }
        catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new InitException("Internal error intializing entity for POST: "+e.getLocalizedMessage());
        }
        entity.setContentType("application/json");
        HttpPost httppost = new HttpPost(remoteUrl);
        httppost.addHeader("accept", "application/json");
        httppost.setEntity(entity);
        HttpClient httpclient = new DefaultHttpClient();
        
        HttpResponse response=null;
        try {
            response = httpclient.execute(httppost);
        }
        catch (IOException e) {
            log.error(e);
            throw e;
        }
        StatusLine status=response.getStatusLine();
        final int code=status.getStatusCode();
        log.debug("status="+code);
        if (code==200 || code==201) {
            log.debug("success");
            return;
        }
        if (code==422) {
            log.debug("duplicate entry, good enough");
            return;
        }
        log.debug("remote server error: "+code+": "+status.getReasonPhrase());
        PostException e = new PostException(status);
        throw e;
    }
}
