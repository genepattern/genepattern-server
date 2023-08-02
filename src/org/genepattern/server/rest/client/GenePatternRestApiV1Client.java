package org.genepattern.server.rest.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.RedirectLocations;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;


public class GenePatternRestApiV1Client {
    public URL jobsUrl;
    public URL getTaskUrl;
    public URL addFileUrl;
    //context path is hard-coded
    private String gpContextPath="gp/";
    private String gpBaseUrl;
    private final String basicAuth;

    //return "https://cloud.genepattern.org/";
    //return "https://beta.genepattern.org/";
    //return "https://gp.indiana.edu/";
    //    return "http://127.0.0.1:8180/";
  
    
    
    /**
     * Example creation of a JSONObject to PUT into the /jobs resource on the GP server.
     * Upload data files when necessary. For each file input parameter, if it's a local file, upload it and save the URL.
     * Use that url as the value when adding the job to GP.
     * 
     * <pre>
       {"lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4", 
        "params": [
             {"name": "input.filename", 
              "values": 
                ["ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct"]
             }
         ]
       }
     * </pre>
     */
    private JsonObject exampleJobInputJsonObject() throws Exception {
        final TaskObj taskInfo=this.getTaskObj("PreprocessDataset");
        
        final String lsid=taskInfo.getLsid();
        final JsonObject obj=new JsonObject();
        obj.addProperty("lsid", lsid);
        final JsonArray paramsJsonArray=new JsonArray();
        
        
        JsonObject paramObj = createParameterJsonObject("floor", "15");
       
        //boolean isFileParam = taskInfo.isFileParam("input.filename");
        Object value2 = this.uploadFileIfNecessary(true, "/Users/liefeld/Desktop/all_aml_copy/all_aml_train.gct");
        JsonObject paramObj2 = createParameterJsonObject("input.filename", value2);

        paramsJsonArray.add(paramObj);
        paramsJsonArray.add(paramObj2);
        
        obj.add("params", paramsJsonArray);
        return obj;
    }


    public JsonObject createParameterJsonObject(String pname, Object value) {
        JsonObject paramObj=new JsonObject();
        paramObj.addProperty("name", pname);
        JsonArray valuesArr=new JsonArray();
        valuesArr.add(new JsonPrimitive(value.toString()));
        paramObj.add("values", valuesArr);
        return paramObj;
    }
    
    
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("Starting");
        // enter your own username and password for testing.  I won't be checking that part in... ;)
        //GenePatternRestApiV1Client inst = new GenePatternRestApiV1Client("https://gp.indiana.edu/","","");
        GenePatternRestApiV1Client inst = new GenePatternRestApiV1Client("http://127.0.0.1:8180/","","");
        
//        JsonObject job = inst.exampleJobInputJsonObject();
//        URI JobStatusUri = inst.submitJob(job);
//        System.out.println("Job submitted");
//        
//        JsonObject status = inst.waitForCompletion(JobStatusUri);
//        System.out.println("Job finished " + status);
//        
//        File dir = new File("/Users/liefeld/Desktop/tmp/");
//        System.out.println("Getting files now");
//        inst.getAllOutputFiles(status, dir);
        System.out.println("get status");
        inst.getJobStatus(559);
        System.out.println("adding comment");
          inst.addComment(559, "Test from api");
        
          //inst.getOutputFile("http://127.0.0.1:8180/gp/jobResults/467/all_aml_train.preprocessed.gct", dir, "rest_download.txt");
    }
    
    public GenePatternRestApiV1Client(String baseUrl, String user, String pass) throws MalformedURLException {
        
        this.gpBaseUrl = validateBaseUrl(baseUrl);
        this.jobsUrl = initEndpointUrl("/jobs");
        this.getTaskUrl=initEndpointUrl( "/tasks");
        this.addFileUrl=initEndpointUrl("/data/upload/job_input");
      
        basicAuth="Basic "+new String(Base64.encodeBase64((user + ":"+pass).getBytes()));
    }

    
    protected String validateBaseUrl(String baseUrl){
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        if (!baseUrl.endsWith(gpContextPath)) {
            baseUrl += gpContextPath;
        }
        return baseUrl;
    }
    
    protected URL initEndpointUrl(String endpoint) throws MalformedURLException {       
        return new URL(gpBaseUrl +"rest/v1"+endpoint);
    }

    public JsonObject waitForCompletion(URI jobStatusUri) throws Exception {
        
        return waitForCompletion(jobStatusUri, 30);
    }
  
    public JsonObject waitForCompletion(URI jobStatusUri, int pollingInterval) throws Exception {
        boolean isFinished = false;
        
        JsonObject currentStatus = getJobStatus(jobStatusUri);
        
        while (!isFinished){
            Thread.currentThread().sleep(pollingInterval * 1000);
            currentStatus = getJobStatus(jobStatusUri);
            isFinished = currentStatus.getAsJsonObject("status").get("isFinished").getAsBoolean();
            
        }    
        return currentStatus;
    }
    
    public boolean deleteJob(String jobNumber) throws Exception {
        return deleteJob(new Integer(jobNumber));
        
    }
    
    public boolean deleteJob(int jobNumber) throws Exception {
        // "https://beta.genepattern.org/gp/rest/v1/jobs/61173/delete"
        URI deleteUri = new URI(this.jobsUrl + "/" + jobNumber+ "/delete");
        final HttpClient client = HttpClients.createDefault();
        HttpDelete delCall = new HttpDelete(deleteUri);
        delCall = this.setAuthHeaders(delCall);
        delCall = this.setJsonHeaders(delCall);
        
        HttpResponse response;
        try {
            response = client.execute(delCall);
            System.out.println(response.toString());
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, delete "+jobNumber, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, delete "+jobNumber, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        } else {
            success=false;
        }
        return success;
    }
    public boolean terminateJob(String jobNumber) throws Exception {
        return deleteJob(new Integer(jobNumber));
        
    }
    
    public boolean terminateJob(int jobNumber) throws Exception {
        // "https://beta.genepattern.org/gp/rest/v1/jobs/61173/delete"
        URI deleteUri = new URI(this.jobsUrl + "/" + jobNumber+ "/terminate");
        final HttpClient client = HttpClients.createDefault();
        HttpDelete delCall = new HttpDelete(deleteUri);
        delCall = this.setAuthHeaders(delCall);
        delCall = this.setJsonHeaders(delCall);
        
        HttpResponse response;
        try {
            response = client.execute(delCall);
            System.out.println(response.toString());
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, delete "+jobNumber, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, delete "+jobNumber, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        } else {
            success=false;
        }
        return success;
    }
   
    
    
    public JsonObject getJobStatus(int jobNumber) throws Exception {
        // "https://beta.genepattern.org/gp/rest/v1/jobs/61173"
        URI statusUri = new URI(this.jobsUrl + "/" + jobNumber);
        return getJobStatus(statusUri);
    }
    
    public JsonObject getJobStatus(String jobNumber) throws Exception {
        // "https://beta.genepattern.org/gp/rest/v1/jobs/61173"
        Integer.parseInt(jobNumber); // just for validation
        URI statusUri = new URI(this.jobsUrl + "/" + jobNumber);
        return getJobStatus(statusUri);
    }
    
    public JsonObject getJobStatus(URI jobStatusUri) throws Exception {
        
        System.out.println("Get status on: "+ jobStatusUri);
        final HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(jobStatusUri);
        get = this.setAuthHeaders(get);
        get = this.setJsonHeaders(get);
        
        HttpResponse response;
        try {
            response = client.execute(get);
           
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, GET "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, GET "+jobsUrl, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            
            success=true;
        } else {
            success=false;
        }
        if (!success) {
            
            String message="GET "+jobStatusUri.toString() +" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            System.out.println(message);
            throw new Exception(message);
        }
      
       
        JsonParser parser = new JsonParser();
        JsonObject jobStatusObj = parser.parse(new InputStreamReader(response.getEntity().getContent())).getAsJsonObject();
        //System.out.println("Status json: " + jobStatusObj.toString());
        return jobStatusObj;
    }
    
    
    public File getOutputFile(String outFileHref, File outputDir, String fileName) throws Exception  {
        
        // "https://beta.genepattern.org/gp/jobResults/61168/all_aml_train.cvt.gct"
        // need to add ?download=true to this
        final HttpClient client = HttpClients.createDefault();
        URI uri = new URIBuilder(outFileHref).addParameter("download", "true").build();

        
        HttpGet fileGet = new HttpGet(uri);
        
        fileGet = this.setAuthHeaders(fileGet);
        
        // NOTE for downloading files its necessary to set the user agent
        //     message.setHeader("User-Agent", "GenePatternRest");
        // which we do in the setAuthheaders() function.  If this is not done then instead of
        // getting the file, you will get the contents of the login page
        
        HttpResponse response;
        try {
            System.out.println("Getting >" + outFileHref+"< ");
            HttpContext context = new BasicHttpContext();
            response = client.execute(fileGet, context);
            if (response.getStatusLine().getStatusCode() >= 400){
                // S3 redirects can fail because of the basic auth headers so
                // if we get a 400 error, try again going to the redirect 
                // without setting basic auth first
                RedirectLocations locations = (RedirectLocations) context.getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
                if (locations != null) {
                    URI finalUrl = locations.getAll().get(locations.getAll().size() - 1);
                    System.out.println("Redirected to " + finalUrl.toASCIIString());
                    fileGet = new HttpGet(finalUrl);
                    response = client.execute(fileGet, context);
                }
            }
            System.out.println(response.toString());
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        HttpEntity entity = response.getEntity();
        
        InputStream is = entity.getContent();
        
        if (fileName == null) fileName = outFileHref.substring(outFileHref.lastIndexOf("/"));
        File outFile = new File(outputDir, fileName);
        File outParentDir = outFile.getParentFile();
        outParentDir.mkdirs();
        FileOutputStream fos = new FileOutputStream(outFile);

        int inByte;
        while ((inByte = is.read()) != -1) {
            fos.write(inByte);
        }

        is.close();
        fos.close(); 
        
        return outFile;
    }
    
    public void getAllOutputFiles(int jobId, File dir) throws Exception {
        JsonObject status = getJobStatus(jobId);
        getAllOutputFiles( status,  dir);
        
    }
    
    
    public void getAllOutputFiles(JsonObject status, File dir) throws Exception {
        
        JsonArray outputFiles = status.getAsJsonArray("outputFiles");
        for (int i=0; i < outputFiles.size();i++){
            String outFileUrl = outputFiles.get(i).getAsJsonObject().get("link").getAsJsonObject().get("href").getAsString();
            String name = outFileUrl.substring(outFileUrl.lastIndexOf('/')+1);
            this.getOutputFile(outFileUrl, dir, name);
        }
        
    }

    public JsonObject addComment(String jobNo, String comment) throws Exception{
        return addComment(Integer.parseInt(jobNo), comment);
    }

    public JsonObject addComment(int jobNo, String comment) throws Exception{
        final HttpClient client = HttpClients.createDefault();
        
        URI commentUri = new URI(this.jobsUrl + "/" + jobNo + "/comments/add");
        HttpPost post = new HttpPost(commentUri);
        // for this call it cannot set the user-agent to GenePatternAPI since its meant to be used from the js web browser
        post.setHeader("Authorization", basicAuth);
      
        ArrayList<BasicNameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("text", comment));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(form, Consts.UTF_8);

      
        post.setEntity(entity);
            
        
        HttpResponse response;
        try {
            response = client.execute(post);
            System.out.println("RESPONSE WAS: " + response.toString());
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        final JsonObject success;
        //when adding a job, expecting a status code of ...
        //   200, OK
        //   201, created
        //   202, accepted
        if (statusCode >= 200 && statusCode < 300) {
            System.out.println("Successs");
           
            JsonParser parser = new JsonParser();
            success = parser.parse(new InputStreamReader(response.getEntity().getContent())).getAsJsonObject();
           
        }
        else {
            success=null;
        }
        if (success == null) {
            
            String message="POST addComment failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            System.out.println(message);
            throw new Exception(message);
        }
        return success;
    }
    
    
    public URI submitJob(JsonObject job) throws Exception {
        final HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(jobsUrl.toExternalForm());
        post = this.setAuthHeaders(post);
        post = this.setJsonHeaders(post);
        try {
            post.setEntity(new StringEntity(job.toString()));
        }
        catch (UnsupportedEncodingException e) {
            throw new Exception("Error preparing HTTP request, POST "+jobsUrl, e);
        }

        HttpResponse response;
        try {
            response = client.execute(post);
            System.out.println(response.toString());
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        catch (IOException e) {
            throw new Exception("Error executing HTTP request, POST "+jobsUrl, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        //when adding a job, expecting a status code of ...
        //   200, OK
        //   201, created
        //   202, accepted
        if (statusCode >= 200 && statusCode < 300) {
            System.out.println("Successs");
            success=true;
        }
        else {
            success=false;
        }
        if (!success) {
            
            String message="POST "+jobsUrl.toExternalForm()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            System.out.println(message);
            throw new Exception(message);
        }
        
        String jobLocation=null;
        Header[] locations=response.getHeaders("Location");
        if (locations.length > 0) {
            jobLocation=locations[0].getValue();
        }
        if (jobLocation==null) {
            final String message="POST "+jobsUrl.toExternalForm()+" failed! Missing required response header: Location";
            System.out.println(message);
            throw new Exception(message);
        }
        URI jobUri;
        try {
            jobUri = new URI(jobLocation);
            System.out.println(jobUri);
            return jobUri;
        }
        catch (URISyntaxException e) {
            final String message="POST "+jobsUrl.toExternalForm()+" failed!";
            System.out.println(message);
            throw new Exception(message, e);
        }
    }
    
    
    
    
    
    
    public URL uploadFileIfNecessary(final boolean isFileParam, final String value) throws Exception {
        if (value==null) {
            // null arg, should be ignored
            return null;
        }
        try {
            URL url=new URL(value);
            return url;
        }
        catch (MalformedURLException e) {
            //expecting this
        }
        
        //make rest api call to gp server
        if (isFileParam) {
            File localFile=new File(value);
            if (!localFile.exists()) {
                //file does not exist, must be a server file path
                return null;
            }
        
            return uploadFile(localFile);
        }
        return null;
    }
    
    
    private URL uploadFile(File localFile) throws Exception {
        if (localFile==null) {
            throw new IllegalArgumentException("localFile==null");
        }
        if (!localFile.exists()) {
            throw new Exception("File does not exist: "+localFile.getAbsolutePath());
        }
        if (localFile.isDirectory()) {
            throw new Exception("File is a directory: "+localFile.getAbsolutePath());
        }
        
        final HttpClient client = HttpClients.createDefault();
        
        String urlStr=addFileUrl.toExternalForm();
        final String encFilename;
        try {
            encFilename=URLEncoder.encode(localFile.getName(), "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new Exception(e);
        }
        
        urlStr+="?name="+encFilename; 
        System.out.println("upload to " + urlStr);
        HttpPost post = new HttpPost(urlStr);
        post = this.setAuthHeaders(post);
        post = this.setJsonHeaders(post);
        FileEntity entity = new FileEntity(localFile, ContentType.DEFAULT_BINARY);
        post.setEntity(entity);
        HttpResponse response = null;
        try {
            response=client.execute(post);
            System.out.println("--> Upload response: " + response);
        }
        catch (ClientProtocolException e) {
            throw new Exception(e);
        }
        catch (IOException e) {
            throw new Exception(e);
        }
        System.out.println(response);
        int statusCode=response.getStatusLine().getStatusCode();
        if (statusCode>=200 && statusCode <300) {
            Header[] locations=response.getHeaders("Location");
            if (locations != null && locations.length==1) {
                System.out.println(locations);
                String location=locations[0].getValue();
                try {
                    return new URL(location);
                }
                catch (MalformedURLException e) {
                    throw new Exception(e);
                }
            }
        }
        else {
            throw new Exception("Error uploading file '"+localFile.getAbsolutePath()+"', "+
                    statusCode+": "+response.getStatusLine().getReasonPhrase());
        }
        throw new Exception("Unexpected error uploading file '"+localFile.getAbsolutePath()+"'");
    }
    

    
    public TaskObj getTaskObj(final String taskNameOrLsid) throws Exception {
        final String urlStr= getTaskUrl.toExternalForm()+"/"+taskNameOrLsid;
        URI taskUri;
        try {
            taskUri = new URI(urlStr);
            System.out.println("Task uri "+ taskUri);
        }
        catch (URISyntaxException e) {
            throw new Exception("URI syntax exception in "+urlStr, e);
        }
        
        JsonObject jsonObject=this.readJsonObjectFromUri(taskUri);
        return new TaskObj.Builder().fromJsonObject(jsonObject).build();
    }
    
    
    /**
     * 
     * @param relativePath, e.g. getJson('/rest/v1/config/gp-version')
     * @return
     * @throws URISyntaxException 
     * @throws GpUnitException 
     */
    public JsonObject getJson(final String relativePath) throws URISyntaxException, Exception {
        String uriStr=gpBaseUrl;
        if (!uriStr.endsWith("/")) {
            uriStr += "/";
        }
        uriStr += gpContextPath+relativePath;
        final URI uri=new URI(uriStr);
        final JsonObject json=readJsonObjectFromUri(uri);
        return json;
    }

   
    public <T extends HttpMessage> T setAuthHeaders(final T message) {
        //for basic auth, use a header like this
        //Authorization: Basic sdfgsdfgsdfgsdfgsdfg
       
        message.setHeader("Authorization", basicAuth);
        message.setHeader("User-Agent", "GenePatternRest");
        
        return message;
    }
    
    public <T extends HttpMessage> T setJsonHeaders(final T message) {
        //for basic auth, use a header like this
        //Authorization: Basic sgsgsfgsdfgsdfgsfgsfd
       
        message.setHeader("Content-type", "application/json");
        message.setHeader("Accept", "application/json");
        
        return message;
    }

    

    /**
     * GET the JSON representation of the contents at the given URI.
     * This is a general purpose helper method for working with the GenePattern REST API.
     * 
     * @param uri
     * @return
     * @throws GpUnitException
     */
    protected JsonObject readJsonObjectFromUri(final URI uri) throws Exception {
        final HttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(uri);
        get = setAuthHeaders(get);
        get = setJsonHeaders(get);
        
        final HttpResponse response;
        try {
            response=client.execute(get);
        }
        catch (ClientProtocolException e) {
            throw new Exception("Error getting contents from uri="+uri, e);
        }
        catch (IOException e) {
            throw new Exception("Error getting contents from uri="+uri, e);
        }
        final int statusCode=response.getStatusLine().getStatusCode();
        final boolean success;
        if (statusCode >= 200 && statusCode < 300) {
            success=true;
        }
        else {
            success=false;
        }
        if (!success) {
            // for debugging
            for(final Header header : response.getAllHeaders()) {
                String str=header.toString();
                System.out.println("    "+str);
            }
            String message="GET "+uri.toString()+" failed! "+statusCode+": "+response.getStatusLine().getReasonPhrase();
            throw new Exception(message);
        }
        
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            final String message="GET "+uri.toString()+" failed! The response should contain an entity";
            throw new Exception(message);
        }

        BufferedReader reader=null;
        try {
            reader=new BufferedReader(
                    new InputStreamReader( response.getEntity().getContent() )); 
            JsonObject jsonObject=readJsonObject(reader);
            return jsonObject;
        }
        catch (IOException e) {
            final String message="GET "+uri.toString()+", I/O error handling response";
            throw new Exception(message, e);
        }
        catch (Exception e) {
            final String message="GET "+uri.toString()+", Error parsing JSON response";
            throw new Exception(message, e);
        }
        catch (Throwable t) {
            final String message="GET "+uri.toString()+", Unexpected error reading response";
            throw new Exception(message, t);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    final String message="GET "+uri.toString()+", I/O error closing reader";
                    throw new Exception(message, e);
                }
            }
        }
    }

    /**
     * Helper class which creates a new JsonObject by parsing the contents from the
     * given Reader.
     * 
     * @param reader, an open and initialized reader, for example from an HTTP response.
     *     The calling method must close the reader.
     * @return
     * @throws GpUnitException
     */
    protected JsonObject readJsonObject(final Reader reader) throws Exception {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement=parser.parse(reader);
        if (jsonElement == null) {
            throw new Exception("JsonParser returned null JsonElement");
        }
        return jsonElement.getAsJsonObject();
    }

}
