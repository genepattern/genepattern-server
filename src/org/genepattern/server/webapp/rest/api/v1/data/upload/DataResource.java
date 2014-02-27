package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.webapp.FileDownloader;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genepattern.server.webapp.rest.api.v1.Util;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.genepattern.server.webservice.server.ProvenanceFinder;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;

/**
 * RESTful implementation of the /data resource.
 * 
 * Notes:
 *     http://127.0.0.1:8080/gp/rest/application.wadl
 *     http://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something
 *     http://jersey.576304.n2.nabble.com/Jersey-and-recursive-paths-td5285298.html
 *     http://neopatel.blogspot.com/2011/10/jersey-rest-api-html-documentation.html
 *     http://jamesaimonetti.com/2012/01/26/curl-stripping-newlines-from-your-csv-or-other-file/
 *     http://marakana.com/s/post/1221/designing_a_beautiful_rest_json_api_video
 *     
 *     Interesting comment here, http://weblogs.java.net/blog/ljnelson/archive/2010/04/28/pushing-jersey-limit
 *         "... we had the requirement to offer up our API via JAX-RS-compliant web services 
 *          (I don't want to say REST, because I really really really really don't want to go down that rathole; that's what rest-discuss is for)."
 *      
 * 
 * @author pcarr
 *
 */
@Path("/v1/data")
public class DataResource {
    final static private Logger log = Logger.getLogger(DataResource.class);
    
    final static public String PROP_MAX_FILE_SIZE= DataResource.class.getPackage().getName()+"max_file_size";
    /**
     * The default max number of bytes that the upload servlet accepts in the request body.
     *     1 Gb, 1074000000
     *     2 Gb, 2147000000
     */
    final static public long MAX_FILE_SIZE_DEFAULT=1074000000L;

    /**
     * Add a new file to be used as input to a job. Return the URI for the uploaded file in the 'Location'
     * header of the response.
     *
     * TODO: Implement more validation 
     *     - when the file will cause the current user's disk quota to be exceeded (as configured by the GP server)
     *     - when the file cannot be written because the OS disk quota is exceeded
     *     - when the file write operation fails because of a server timeout
     *     - other system errors, for example, we require access to the GP DB so that we can record the file record
     * 
     * Requires authentication with a valid gp user id.
     * 
     * Expected response codes:
     *     ?, this method requires authentication, if there is not a valid gp user logged in, respond with basic authentication request.
     *     
     *     201 - Created
     *     
     *     411 - Length required
     *     413 - Request entity too large
     *     500 - Internal server error
     * 
     * Example usage:
     * <pre>
       curl -u test:test -X POST --data-binary @all_aml_test.cls http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input?name=all_aml_test.cls
     * </pre>
     * 
     * @param request
     * @param filename
     * @param in
     * 
     * @return an HTTP response
     */
    @POST
    @Path("/upload/job_input") 
    public Response handlePostJobInputInBody(
            final @Context HttpServletRequest request,
            final @HeaderParam("Content-Length") String contentLength,
            final @QueryParam("name") String filename,
            final InputStream in) 
    {
        try { 
            final GpContext userContext=Util.getUserContext(request);
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext); 
            final GpFilePath gpFilePath=writeJobInputFile(userContext, in, filename, maxNumBytes);
            final String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (WebApplicationException e) {
            //propagate these up to the calling method, for standard REST API error handling
            throw e;
        }
        catch (Throwable t) {
            log.error(t);
            //all others convert to internal server error
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Add a new file to the uploads directory of the current user, specifically when you 
     * want to use the file as a job input file in a subsequent call to add a job.
     * 
     * This method creates a new resource.
     * 
     * Example usage:
     * <pre>
       curl -X POST --form file=@all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input_form
       </pre>
     * The '-X POST' is redundant when using the '--form' option. This will work also.
     * <pre>
       curl --form file=@all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input_form
     * </pre>
     * 
     * @param request
     * @param fileDetail
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload/job_input_form") 
    public Response handlePostJobInputMultipartForm(
            final @Context HttpServletRequest request,
            final @HeaderParam("Content-Length") String contentLength,
            final @FormDataParam("file") InputStream in,
            final @FormDataParam("file") FormDataContentDisposition fileDetail) 
    {
        try {
            final GpContext userContext=Util.getUserContext(request);        
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext);
            final GpFilePath gpFilePath=writeJobInputFile(userContext, in, fileDetail.getFileName(), maxNumBytes);
            final String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Return the requested file, attaching the headers necessary for the browser to handle the file as a download
     * @param request
     * @param response
     * @param path
     * @return
     */
    @GET
    @Path("/download/{path:.+}")
    public Response getFile(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) {
        try {
            ServletContext servletContext = request.getSession().getServletContext();

            String user = (String) request.getSession().getAttribute(GPConstants.USERID);
            GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj("<GenePatternURL>" + path);
            File file = filePath.getServerFile();

            FileDownloader.serveFile(servletContext, request, response, true, FileDownloader.ContentDisposition.ATTACHMENT, file);

            return Response.ok().build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Delete the specified file
     * @param request
     * @param path
     * @return
     */
    @DELETE
    @Path("/delete/{path:.+}")
    public Response deleteFile(@Context HttpServletRequest request, @PathParam("path") String path) {
        try {
            String user = (String) request.getSession().getAttribute(GPConstants.USERID);
            GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj("<GenePatternURL>" + path);

            if (path.startsWith("/users")) { // If this is a user upload
                boolean deleted = DataManager.deleteUserUploadFile(user, filePath);

                if (deleted) {
                    return Response.ok().entity("Deleted " + filePath.getName()).build();
                }
                else {
                    return Response.status(500).entity("Could not delete " + filePath.getName()).build();
                }
            }
            else if (path.startsWith("/jobResults")) { // If this is a job result file
                LocalAnalysisClient analysisClient = new LocalAnalysisClient(user);
                int jobNumber = Integer.parseInt(((JobResultFile) filePath).getJobId());
                analysisClient.deleteJobResultFile(jobNumber, jobNumber + "/" + filePath.getName());

                return Response.ok().entity("Deleted " + filePath.getName()).build();
            }
            else { // Other files not implemented
                return Response.status(500).entity("Delete not implemented for this file type: " + filePath.getName()).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Create the specified subdirectory
     * Subdirectory creation in this way is not recursive
     * @param request
     * @param path
     * @return
     */
    @PUT
    @Path("/createDirectory/{path:.+}")
    public Response createDirectory(@Context HttpServletRequest request, @PathParam("path") String path) {
        try {
            String user = (String) request.getSession().getAttribute(GPConstants.USERID);
            GpContext userContext = GpContext.getContextForUser(user);
            GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj("<GenePatternURL>" + path.replaceAll(" ", "%20"));

            // Init the file object
            String parentDirectoryPath = null;
            if (filePath.getRelativeFile().getPath().contains("/")) { // Special case for files in the root directory
                parentDirectoryPath = filePath.getRelativeFile().getParentFile().getPath();
            }
            else {
                parentDirectoryPath = "./";
            }
            File relativePath = DataManager.initSubdirectory(parentDirectoryPath, filePath.getName());

            // Check for reserved names
            boolean isTmpDir = DataManager.isTmpDir(userContext, relativePath);
            if (isTmpDir) {
                return Response.status(500).entity("Reserved name. Could not create " + filePath.getName()).build();
            }

            // Create the directory
            boolean success = DataManager.createSubdirectory(userContext, relativePath);

            if (success) {
                return Response.ok().entity("Created " + filePath.getName()).build();
            }
            else {
                return Response.status(500).entity("Could not create " + filePath.getName()).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Add a file to the uploads directory of the current user. Example usage,
     * <pre>
     * curl -X PUT --data-binary @all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/v1/data/upload/all_aml/all_aml_test.cls
     * </pre>
     * 
     * @param request
     * @param path, the relative path to the file, can be a simple filename (e.g. 'all_aml_test.gct') or a relative path (e.g. 'tutorial/all/all_aml_test.gct').
     * @param in, the content of the file must be the only thing included in the body of the request
     * 
     * @return the URI for the new or updated file.
     */
    @PUT
    @Path("/upload/{path:.+}")  //regular expression to match nested paths, e.g. PUT /upload/tmp/a/b/file.txt
    public Response putFile(
            final @Context HttpServletRequest request,
            final @HeaderParam("Content-Length") String contentLength,
            final @PathParam("path") String path,
            final @DefaultValue("false") @QueryParam("replace") boolean replace,
            final InputStream in) 
    {
        try {
            final GpContext userContext=Util.getUserContext(request);  
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext);
            final GpFilePath gpFilePath=writeUserUploadFile(userContext, in, path, maxNumBytes);
            final String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Helper method which checks the 'Content-Length' header as well as the maxNumBytes configuration param
     * for the current user.
     * 
     * This allows us to fail early if the 'Content-Length' exceeds maxNumBytes.
     * But we can't trust the client so we also need to enforce maxNumBytes when streaming the response body into the file system.
     * 
     * @param contentLength, as parsed from the HTTP request header
     * @param userContext
     * @return
     * @throws WebApplicationException, HTTP response headers are automatically set when errors occur
     */
    private long initMaxNumBytes(final String contentLength, final GpContext userContext) 
    throws WebApplicationException
    {
        if (contentLength == null) {
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.LENGTH_REQUIRED).build());
        }
        long numBytes = -1L;
        long maxNumBytes = MAX_FILE_SIZE_DEFAULT;
        numBytes=Long.parseLong(contentLength);
        maxNumBytes=ServerConfigurationFactory.instance().getGPLongProperty(userContext, PROP_MAX_FILE_SIZE, MAX_FILE_SIZE_DEFAULT);
        if (numBytes > maxNumBytes) {
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.REQUEST_ENTITY_TOO_LARGE).build());
        }
        return maxNumBytes;
    }

    ////////////////////////////////////////////////////////////////
    // Helper methods for adding user upload files to GenePattern
    // TODO: should refactor these methods into an interface
    ////////////////////////////////////////////////////////////////
    GpFilePath writeUserUploadFile(final GpContext userContext, final InputStream in, final String path, final long maxNumBytes) 
    throws Exception
    {
        JobInputFileUtil fileUtil = new JobInputFileUtil(userContext);
        
        File relativePath = new File(path);
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(relativePath);
        
        //save it
        writeBytesToFile(userContext, in, gpFilePath, maxNumBytes);
        return gpFilePath;
    }

    /**
     * This method saves the data file uploaded from the REST client into a 
     * temporary directory within the user upload folder. It also saves a record
     * of this into the GP database.
     * 
     * @param userContext
     * @param in
     * @return
     * @throws Exception
     */
    GpFilePath writeJobInputFile(final GpContext userContext, final InputStream in, final String filename, final long maxNumBytes) 
    throws WebApplicationException
    {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userContext.userId not set");
        }
        GpFilePath gpFilePath=null;
        try {
            gpFilePath=createJobInputDir(userContext, filename);
        }
        catch (Exception e) {
            //TODO: figure out how to include more meaningful error message in the response header
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.INTERNAL_SERVER_ERROR).build());
        }

        // save it
        writeBytesToFile(userContext, in, gpFilePath, maxNumBytes);
        return gpFilePath;
    }

    private String createPipelineMessage(String user, List<ParameterInfo> params) throws UnsupportedEncodingException {
        String toReturn = "";
        GpContext userContext = GpContext.getContextForUser(user);
        long maxFileSize = ServerConfigurationFactory.instance().getGPLongProperty(userContext, "pipeline.max.file.size", 250L * 1000L * 1024L);
        for (ParameterInfo i : params) {
            toReturn += "Changed parameter " + i.getName() + " to 'Prompt When Run' because it exceeded maximum file size of " + JobHelper.getFormattedSize(maxFileSize) + " for pipelines.  ";
        }
        if (toReturn.length() != 0) {
            toReturn = "&message=" + URLEncoder.encode(toReturn, "UTF-8");
        }
        return toReturn;
    }

    /**
     * Create a provenance pipeline from the given job result file
     * @param request
     * @param path
     * @return
     */
    @PUT
    @Path("/createPipeline/{path:.+}")
    public Response createPipeline(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path, @QueryParam("name") String pipelineName) {
        try {
            String user = (String) request.getSession().getAttribute(GPConstants.USERID);
            GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj("<GenePatternURL>" + path);
            Integer jobNumber = Integer.parseInt(((JobResultFile) filePath).getJobId());

            // Set the pipeline name
            if (pipelineName == null) pipelineName = "job_" + jobNumber;

            ProvenanceFinder.ProvenancePipelineResult pipelineResult = new LocalAnalysisClient(user).createProvenancePipeline(jobNumber.toString(), pipelineName);
            String lsid = pipelineResult.getLsid();
            String message = createPipelineMessage(user, pipelineResult.getReplacedParams());
            if (lsid == null) {
                return Response.status(500).entity("Unable to create pipeline: " + filePath.getName()).build();
            }

            response.setHeader("pipeline-forward", request.getContextPath() + "/pipeline/index.jsf?lsid=" + lsid);
            return Response.ok().entity("Created Pipeline: " + pipelineName + " " + message).build();
        }
        catch (Exception ex) {
            return Response.status(500).entity("Unable to create pipeline: " + ex.getLocalizedMessage()).build();
        }

        //UIBeanHelper.getResponse().sendRedirect(UIBeanHelper.getRequest().getContextPath() + "/pipeline/index.jsf?lsid=" + UIBeanHelper.encode(lsid) + message);
    }
    
    private GpFilePath createJobInputDir(final GpContext userContext, final String filename) 
    throws Exception
    {
        GpFilePath tmpDir=null;
        try {
            tmpDir=JobInputFileUtil.createTmpDir(userContext);
        }
        catch (Exception e) {
            String message="Error creating unique parent directory for the job input file: "+filename;
            log.error(message, e);
            throw new Exception(message, e);
        }
        final String path=tmpDir.getRelativePath() + "/" + filename;
        File relativeFile=new File(path);
        GpFilePath job_input_file=null;
        try {
            job_input_file=GpFileObjFactory.getUserUploadFile(userContext, relativeFile);
            return job_input_file;
        }
        catch (Exception e) {
            String message="Error initializing GpFilePath for the job input file: "+filename;
            log.error(message,e);
            throw new Exception(message, e);
        }
    }
    
    final static class MaxFileSizeException extends Exception {
        public MaxFileSizeException(String m) {
            super(m);
        }
    }
    final static class WriteToFileException extends Exception {
        public WriteToFileException(Exception e) {
            super(e);
        }
    }
    
    private void writeBytesToFile(final GpContext userContext, final InputStream in, final GpFilePath gpFilePath, final long maxNumBytes) {
        // save it
        boolean success=false;
        try {
            writeToFile(in, gpFilePath.getServerFile().getCanonicalPath(), maxNumBytes);
            success=true;
        }
        catch (MaxFileSizeException e) {
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.REQUEST_ENTITY_TOO_LARGE).build());
        }
        catch (WriteToFileException e) {
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.INTERNAL_SERVER_ERROR).build());
        }
        catch (Throwable t) {
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.INTERNAL_SERVER_ERROR).build());
        }
        finally {
            if (!success) {
                //delete the local file
                boolean deleted=gpFilePath.getServerFile().delete();
                if (!deleted) {
                    log.error("Didn't delete job_input_file, user="+userContext.getUserId()+", filename="+gpFilePath.getRelativePath());
                }
            }
        }
        try {
            JobInputFileUtil.addUploadFileToDb(userContext,gpFilePath);
        }
        catch (Throwable t) {
            log.error("Error saving record of job_input_file to DB, filename="+gpFilePath.getRelativePath(), t);
            throw new WebApplicationException(
                    Response.status(ClientResponse.Status.INTERNAL_SERVER_ERROR).build());
        }
    }
    
    private void writeToFile( final InputStream uploadedInputStream, final String uploadedFileLocation, final long maxNumBytes) 
    throws MaxFileSizeException, WriteToFileException
    {
        final File toFile=new File(uploadedFileLocation);
        OutputStream out=null;
        try {
            long numBytesRead = 0L;
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(toFile);
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
                numBytesRead += read;
                if (numBytesRead > maxNumBytes) {
                    log.debug("maxNumBytes reached: "+maxNumBytes);
                    throw new MaxFileSizeException("maxNumBytes reached: "+maxNumBytes);
                } 
            }
            out.flush();
            out.close();
        } 
        catch (IOException e) {
            log.error("Error writing to file: "+toFile.getAbsolutePath());
            throw new WriteToFileException(e);
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    log.error("Error closing output stream in finally clause", e);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////
    // Helper methods for working with HTTP requests
    // TODO: should refactor into a common utility class
    ////////////////////////////////////////////////////////////////
    private void debugHeaders(final HttpServletRequest request) {
        //for debugging
        Enumeration<?> hNames = request.getHeaderNames();
        while (hNames.hasMoreElements()) {
            final String hName = (String) hNames.nextElement();
            final String hVal = request.getHeader(hName);
            System.out.println(hName+": "+hVal);
        }
    }
    
    private void debugContent(final InputStream in) {
        final byte[] bytes; 
        if (in == null) {
            bytes = new byte[0];
        }
        else {
            try {
                bytes = IOUtils.toByteArray(in);
            }
            catch (IOException e) {
                log.error(e);
                throw new WebApplicationException();
            }
        }
        String content="";
        int numBytes=0;
        int numChars=0;
        if (bytes != null) {
            numBytes=bytes.length;
            try {
                content = new String(bytes,"UTF-8");
                numChars=content.length();
                System.out.println("numChars: "+numChars);
            }
            catch (UnsupportedEncodingException e) {
                log.error(e);
                throw new WebApplicationException();
            }
        }
    }

}
