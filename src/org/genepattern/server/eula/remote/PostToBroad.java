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
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.eula.EulaInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Remote record of EULA by posting to the Broad server. Here is the draft spec for the remote service.
<pre>
HTTP POST http://vgpweb01.broadinstitute.org:3000/eulas/
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

    static class MyException extends Exception {
        public MyException(String s) {
            super(s);
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

    //the URL of the remote server, to where we record the eula agreements, 
    //    by default it is hosted at the Broad Institute
    private String remoteUrl="http://vgpweb01.broadinstitute.org:3000/eulas";

    //the URL if this gp server, it's stored as a column in the remote DB
    private String gpUrl=null;
    private String gp_user_id=null;
    private String email=null;
    private String task_name=null;
    private String task_lsid=null;
    //private EulaInfo eula=null;
    
    public void setRemoteUrl(final String url) {
        this.remoteUrl=url;
    }
    
    public void setGpUrl(final String url) {
        this.gpUrl=url;
    }

    public void setGpUserId(final String gpUserId) {
        this.gp_user_id=gpUserId;
    }

    public void setEmail(final String email) {
        this.email=email;
    }

    public void setEulaInfo(final EulaInfo eulaInfo) {
        if (eulaInfo==null) {
            throw new IllegalArgumentException("eulaInfo==null");
        }
        this.task_name=eulaInfo.getModuleName();
        this.task_lsid=eulaInfo.getModuleLsid();
    }

    public void setTaskName(final String taskName) {
        this.task_name=taskName;
    }

    public void setTaskLsid(final String taskLsid) {
        this.task_lsid=taskLsid;
    }
    
    private String getJson() throws JSONException {
        Map<String,String> map=new HashMap<String,String>();
        map.put("gp_user_id", gp_user_id);
        map.put("task_lsid", task_lsid);
        if (isSet(task_name)) {
            map.put("task_name", task_name);
        }
        if (isSet(email)) {
            map.put("email", email);
        }
        if (!isSet(gpUrl)) {
            log.error("gpUrl is not initialized");
        }
        else {
            map.put("gp_url", gpUrl);
        }
        JSONObject eulaJson=new JSONObject();
        eulaJson.put("eula", map);
        return eulaJson.toString();
    }
    
    /**
     * Use Apache HttpComponents library (http://hc.apache.org/).
     * 
     * @throws Exception when,
     *      a) initialization errors, in GP server code, before making POST to remote server, or
     *      b) IOExceptions thrown during the POST
     *      c) anything other than a 2xx or 422 (duplicate) response code after the POST
     */
    public void postRemoteRecord() throws IllegalArgumentException, MyException {  
        if (!isSet(remoteUrl)) {
            throw new IllegalArgumentException("remoteUrl not set");
        }
        if (!isSet(gp_user_id)) {
            throw new IllegalArgumentException("gp_user_id not set");
        }
        if (!isSet(task_lsid)) {
            throw new IllegalArgumentException("task_lsid not set");
        }
        if (!isSet(gpUrl)) {
            gpUrl=ServerConfiguration.instance().getGenePatternURL().toString();
        }
        String json=null;
        try {
            json=getJson();
        }
        catch (JSONException e) {
            log.error(e);
            throw new MyException("Internal error formatting json for record: "+e.getLocalizedMessage());
        }
        StringEntity entity=null; 
        try {
            entity = new StringEntity(json, HTTP.UTF_8);
        }
        catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new MyException("Internal error intializing entity for record: "+e.getLocalizedMessage());
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
            throw new MyException(": "+e.getLocalizedMessage());
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
        throw new MyException("remote server error: "+code+": "+status.getReasonPhrase());
    }
}
