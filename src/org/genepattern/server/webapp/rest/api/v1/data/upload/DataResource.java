/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.FileUtil;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.UploadDirectoryZipWriter;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpDirectoryNode;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.dm.Node;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.output.GpFileType;
import org.genepattern.server.job.output.JobOutputFile;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genepattern.server.webapp.rest.api.v1.Util;

import org.genepattern.server.webapp.rest.api.v1.job.GpLink;
import org.genepattern.server.webapp.rest.api.v1.job.JobObjectCache;
import org.genepattern.server.webapp.uploads.UploadFileServlet;
import org.genepattern.server.webapp.uploads.UploadFilesBean;
import org.genepattern.server.webapp.uploads.UploadTreeJSON;
import org.genepattern.server.webservice.server.ProvenanceFinder;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONException;
import org.json.JSONObject;
import org.genepattern.server.webapp.uploads.UploadTreeJSON;
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
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext); 
            final GpFilePath gpFilePath=writeJobInputFile(gpConfig, userContext, in, filename, maxNumBytes);
            final String location = UrlUtil.getHref(request, gpFilePath);
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
     * XXX GP-9808 JTL 011725 
     * 
     * Add a new file to be used as output to a job. Return the URI for the uploaded file in the 'Location'
     * header of the response.
     *
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
       curl -u test:test -X POST --data-binary @all_aml_test.cls "http://127.0.0.1:8080/gp/rest/v1/data/upload/job_output?name=all_aml_test.cls&job_id=12345"
     * </pre>
     * 
     * @param request
     * @param filename
     * @param in
     * 
     * @return an HTTP response
     */
    @POST
    @Path("/upload/job_output") 
    public Response handlePostJobOutput(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            final @HeaderParam("Content-Length") String contentLength,
            final @QueryParam("name") String filename,
            final @QueryParam("jobid") String job_id_str,
            final InputStream in)
    {
        try { 
                       
            Integer job_id = null;
            try {
                job_id = Integer.parseInt(job_id_str);
            } catch (NumberFormatException e) {
                return Response.status(Status.BAD_REQUEST).entity("Invalid job_id format").build();
            }
            
            
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext); 
            GpContext jobContext = Util.getJobContext(request, ""+ job_id);
            JobInfo jobInfo = jobContext.getJobInfo();
            
            
            PermissionsHelper ph = new PermissionsHelper(
                    userContext.isAdmin(), //final boolean _isAdmin,
                    userContext.getUserId(), // final String _userId,
                    jobInfo.getJobNumber(), // final int _jobNo,
                    jobInfo.getUserId(), //final String _rootJobOwner,
                    jobInfo.getJobNumber()//, //final int _rootJobNo,
            );

            if (!ph.canWriteJob()) {
                return Response.status(Status.UNAUTHORIZED).entity("User does not have permission to write files to this job").build();
            
            }
            
            final GpFilePath gpFilePath=writeJobOutputFile(gpConfig, userContext, in, filename, jobInfo, jobContext, maxNumBytes);
            
            
            
            final String location = UrlUtil.getHref(request, gpFilePath);
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
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext);
            final GpFilePath gpFilePath=writeJobInputFile(gpConfig, userContext, in, fileDetail.getFileName(), maxNumBytes);
            final String location = UrlUtil.getHref(request, gpFilePath);
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Copies a file from one location to another
     * @param request
     * @param from
     * @param to
     * @return
     */
    @POST
    @Path("/copy")
    public Response copyFile(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) {
        // Fix for when the preceding slash is missing from the path
        if (!from.startsWith("/")) from = "/" + from;
        if (!to.startsWith("/")) to = "/" + to;

        try {
            // Handle space characters
            from = URLDecoder.decode(from, "UTF-8");
            to = URLDecoder.decode(to, "UTF-8");

            // Get the file location to copy from
            final GpConfig gpConfig = ServerConfigurationFactory.instance();
            GpContext userContext = Util.getUserContext(request);
            GpFilePath fromPath = null;
            if (from.startsWith("/users")) {
                File fromFile = extractUsersPath(userContext, from);
                fromPath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, fromFile);
            }
            else if (from.startsWith("/jobResults")) {
                String fromFileString = extractJobResultsPath(from);
                fromPath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, "/jobResults", fromFileString);
            }
            else {
                return Response.status(500).entity("Copy not implemented for this source file type: " + from).build();
            }

            if (to.startsWith("/users")) { // If copying to a user upload
                File toFile = extractUsersPath(userContext, to);
                GpFilePath toPath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, toFile);

                boolean copied = DataManager.copyToUserUpload(HibernateUtil.instance(), userContext, fromPath, toPath);

                if (copied) {
                    return Response.ok().entity("Copied " + fromPath.getRelativePath() + " to " + toPath.getRelativePath()).build();
                }
                else {
                    return Response.status(500).entity("Could not copy " + fromPath.getRelativePath() + " to " + toPath.getRelativePath()).build();
                }
            }
            else { // Copying to other file locations not supported
                return Response.status(500).entity("Copy not implemented for this destination file type: " + to).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * 
     * GP-9217 return representation of a user's files tab for use/display
     * 
     *  * Example usage:
     * <pre>
     *  curl -u test:test http://127.0.0.1:8080/gp/rest/v1/data/userfilesjson
     *  </pre>
     * @param request
     * @return
     */
    @GET
    @Path("/user/files")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@Context HttpServletRequest request, @Context HttpServletResponse response) throws JSONException {
        GpContext userContext = Util.getUserContext(request);
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        JSONObject object = new JSONObject();
        final HibernateSessionManager mgr=HibernateUtil.instance();
        try {
            GpDirectoryNode treeNode = UserUploadManager.getFileTree(mgr, gpConfig, userContext);
            JSONObject treeJson = makeFileJSON(request, treeNode);
            
            object = treeJson;
        } catch (Exception e){
            e.printStackTrace();
        }
        return Response.ok().entity(object.toString()).build();
    }

    protected static JSONObject makeFileJSON(final HttpServletRequest request, final Node<GpFilePath> node) throws Exception {
        
        JSONObject attr = new JSONObject();
        final String href=UrlUtil.getHref(request, node.getValue());
        attr.put("href", href);
        attr.put("name", node.getValue().getName());

        // Add the Kind data
        String kind = node.getValue().getKind();
        attr.put("kind", kind);

       
        Collection <Node<GpFilePath>> childNodes = node.getChildren();
        if (node.getValue().isDirectory()) {
            attr.put("isDirectory", true);
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (Node<GpFilePath> child : childNodes) {
               
                JSONObject childJSON = makeFileJSON(request, child);
                children.add(childJSON);
              
            }
            attr.put("children", children);
        } else {
            // add file size and date
            attr.put("isDirectory", false);
            attr.put("size", node.getValue().getFileLength());
            attr.put("lastModified", node.getValue().getLastModified());
            attr.put("extension",node.getValue().getExtension()  );         
            
        }
        return attr;
    }

    
    /**
     * Download endpoint for DataResource
     * Currently only downloading directories is implemented.
     * Downloading files is still performed by our old DataServlet.
     * @param request
     * @return
     */
    @GET
    @Path("/download")
    public Response download(@Context HttpServletRequest request, @Context HttpServletResponse response, @QueryParam("path") String path) {
        // Fix for when the preceding slash is missing from the path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            // Handle space characters
            path = URLDecoder.decode(path, "UTF-8");

            final GpConfig gpConfig = ServerConfigurationFactory.instance();
            final GpContext userContext = Util.getUserContext(request);
            if (path.startsWith("/users")) { // If this is a user upload
                File uploadFilePath = extractUsersPath(userContext, path);
                GpFilePath fileToDownload = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, uploadFilePath);

                if (fileToDownload.isDirectory()) {
                    UploadDirectoryZipWriter udzw = new UploadDirectoryZipWriter(fileToDownload);

                    response.setHeader("Content-Disposition", "attachment; filename=" + fileToDownload.getName() + ".zip" + ";");
                    response.setHeader("Content-Type", "application/octet-stream");
                    response.setHeader("Cache-Control", "no-store");
                    response.setHeader("Pragma", "no-cache");
                    response.setDateHeader("Expires", 0);

                    OutputStream os = response.getOutputStream();
                    ZipOutputStream zipStream = new ZipOutputStream(os);
                    udzw.writeFilesToZip(zipStream);
                    os.close();

                    return Response.ok().build();
                }
                else {
                    // Non-directories not implemented
                    return Response.status(500).entity("Download not implemented for non-directories. Use DataServlet for: " + path).build();
                }
            }
            else {
                // Other files not implemented
                return Response.status(500).entity("Download not implemented for this file type: " + path).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }


    /**
     * Renames a user upload file
     * @param request
     * @param path, for example '/jobResults/14855/all aml test.cvt.gct',
     *                          '/users/user_id/all_aml_test.cls'
     * @return
     */
    @PUT
    @Path("/rename")
    public Response renameFile(@Context HttpServletRequest request, @QueryParam("path") String path, @QueryParam("name") String name) {
        // Fix for when the preceding slash is missing from the path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            // Handle space characters
            path = URLDecoder.decode(path, "UTF-8");

            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);
            if (path.startsWith("/users")) { // If this is a user upload
                final File uploadFilePath=extractUsersPath(userContext, path);
                final GpFilePath fileToRename = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, uploadFilePath);

                // Check whether file with new name already exists
                File oldFile = fileToRename.getServerFile();
                File newFileAbsolute = new File(oldFile.getParentFile(), name);
                if (newFileAbsolute.exists()) {
                    return Response.status(500).entity("Could not rename " + fileToRename.getName() + ". File already exists.").build();
                }

                boolean renamed = DataManager.renameUserUpload(HibernateUtil.instance(), gpConfig, userContext.getUserId(), fileToRename, name);

                if (renamed) {
                    return Response.ok().entity("Renamed " + fileToRename.getName() + " to " + name).build();
                }
                else {
                    return Response.status(500).entity("Could not rename " + fileToRename.getName()).build();
                }
            }
            else { // Other files not implemented
                return Response.status(500).entity("Rename not implemented for this file type: " + path).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Moves a file or directory to a new upload file location
     * @param request
     * @param from
     * @param to
     * @return
     */
    @PUT
    @Path("/move")
    public Response moveFile(@Context HttpServletRequest request, @QueryParam("from") String from, @QueryParam("to") String to) {
        // Fix for when the preceding slash is missing from the path
        if (!from.startsWith("/")) from = "/" + from;
        if (!to.startsWith("/")) to = "/" + to;

        try {
            // Handle space characters
            from = URLDecoder.decode(from, "UTF-8");
            to = URLDecoder.decode(to, "UTF-8");

            // Prevent moving a directory to itself or a child directory
            if (to.startsWith(from)) {
                return Response.status(500).entity("Cannot move a directory to itself or a child").build();
            }

            // Get the file location to copy from
            final GpConfig gpConfig = ServerConfigurationFactory.instance();
            GpContext userContext = Util.getUserContext(request);
            GpFilePath fromPath = null;
            if (from.startsWith("/users")) {
                File fromFile = extractUsersPath(userContext, from);
                fromPath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, fromFile);
            }
            else if (from.startsWith("/jobResults")) {
                String fromFileString = extractJobResultsPath(from);
                fromPath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, "/jobResults", fromFileString);
            }
            else {
                return Response.status(500).entity("Move not implemented for this source file type: " + from).build();
            }

            if (to.startsWith("/users")) { // If copying to a user upload
                File toFile = extractUsersPath(userContext, to);
                GpFilePath toPath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, toFile);

                boolean moved = DataManager.moveToUserUpload(HibernateUtil.instance(), userContext.getUserId(), fromPath, toPath, userContext);

                if (moved) {
                    return Response.ok().entity("Moved " + fromPath.getRelativePath() + " to " + toPath.getRelativePath()).build();
                }
                else {
                    return Response.status(500).entity("Could not move " + fromPath.getRelativePath() + " to " + toPath.getRelativePath()).build();
                }
            }
            else { // Copying to other file locations not supported
                return Response.status(500).entity("Move not implemented for this destination file type: " + to).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Delete the specified jobResults or user upload file
     * @param request
     * @param path, for example '/jobResults/14855/all aml test.cvt.gct', 
     *                          '/users/user_id/all_aml_test.cls'
     * @return
     */
    @DELETE
    @Path("/delete/{path:.+}")
    public Response deleteFile(@Context HttpServletRequest request, @PathParam("path") String path) {
        // Fix for when the preceding slash is missing from the path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            final GpConfig gpConfig = ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);
            if (path.startsWith("/users")) { // If this is a user upload
                final File uploadFilePath=extractUsersPath(userContext, path);
                final GpFilePath uploadFileToDelete = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, uploadFilePath);

                boolean deleted = DataManager.deleteUserUploadFile(HibernateUtil.instance(), userContext.getUserId(), uploadFileToDelete);

                if (deleted) {
                    return Response.ok().entity("Deleted " + uploadFileToDelete.getName()).build();
                }
                else {
                    return Response.status(500).entity("Could not delete " + uploadFileToDelete.getName()).build();
                }
            }
            else if (path.startsWith("/jobResults/")) { // If this is a job result file
                try {
                    String relativePath=deleteJobResultFile(HibernateUtil.instance(), gpConfig, userContext, path);
                    return Response.ok().entity("Deleted " + relativePath).build();
                }
                catch (Exception e) {
                    //Exception means some kind of user error, such as an invalid file path
                    return Response.status(404).entity(e.getMessage()).build();
                }
                catch (Throwable t) {
                    log.error("Unexpected server error deleting path=" + path, t);
                    return Response.status(500).entity("Unexpected server error deleting path=" + path).build();
                }
            }
            else { // Other files not implemented
                return Response.status(500).entity("Delete not implemented for this file type: " + path).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }
    
    private String deleteJobResultFile(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext, final String path) throws Exception {
        //example path="/jobResults/14855/all # aml test.cvt.gct"
        final int idx0="/jobResults/".length();
        if (idx0<0) {
            log.error("Unexpected error, path should start with '/jobResults/', path="+path);
            throw new Exception("Unable to delete path="+path);
        }
        final int idx1=path.indexOf("/", idx0);
        if (idx1<idx0) {
            //return Response.status(404).entity("Unable to delete path="+path).build();
            throw new Exception("Unable to delete path="+path);
        }
        final String jobIdStr=path.substring(idx0, idx1);
        final Integer jobId=Integer.parseInt(jobIdStr);
        final File relativePath=new File(path.substring(idx1+1));

        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            LocalAnalysisClient analysisClient = new LocalAnalysisClient(mgr, gpConfig, userContext.getUserId());
            analysisClient.deleteJobResultFile(jobId, jobId + "/" + relativePath.getPath());
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (WebServiceException e) {
            log.debug("Error deleting path="+path, e);
            throw new Exception("Unable to delete path="+path);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        
        // remove job from cache
        JobObjectCache.removeJobFromCache(jobId);
        //TODO: handle pipelines
        //TODO: consider using an eventbus, fireGpJobChangedEvent
        //   JobStatusEvent event = new JobStatusEvent(jobIdStr, null, null); 
        //   JobEventBus.instance().post(event);

        //include the jobId in the end-user message
        return jobId + "/" + relativePath.getPath();
    }

    /**
     * Create the specified subdirectory
     * Subdirectory creation in this way is not recursive
     * 
     * TODO: the preferred API is for the 'path' to be relative to the current user's 
     *     'home directory'.
     * @param request
     * @param path
     * @return
     */
    @PUT
    @Path("/createDirectory/{path:.+}")
    public Response createDirectoryV1(@Context HttpServletRequest request, @PathParam("path") String path) {
        // Fix for when the preceding slash is missing from the path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            GpContext userContext=Util.getUserContext(request);
            File relativePath = extractUsersPath(userContext, path);
            if (relativePath == null) {
                //error
                return Response.status(500).entity("Could not createDirectory: " + path).build();
            }

            // Create the directory
            boolean success = DataManager.createSubdirectory(HibernateUtil.instance(), ServerConfigurationFactory.instance(), userContext, relativePath);

            if (success) {
                return Response.ok().entity("Created " + relativePath.getName()).build();
            }
            else {
                return Response.status(500).entity("Could not create " + relativePath.getName()).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Extract the job result file's pathInfo from the full job results path
     * @param path
     * @return
     */
    private String extractJobResultsPath(String path) {
        final String pathInfo = "/jobResults";
        return path.substring(pathInfo.length());
    }

    /**
     * Extract path=/users/{userId}/{relativePath} from the pathParam arg.
     * Copies hard-code rules from the GpFileObjFactory for working with user directories.
     * @param userContext, must be non-null with a valid userId
     * @param pathParam, should be non-null
     * @return
     */
    private File extractUsersPath(final GpContext userContext, final String pathParam) {
        if (pathParam==null) {
            return null;
        }
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null) {
            throw new IllegalArgumentException("userContext.userId==null");
        }
        
        final String pathInfo="/users/"+userContext.getUserId()+"/";        
        if (!pathParam.startsWith(pathInfo)) {
            return null;
        }
        final String relativePath=pathParam.substring(pathInfo.length());
        return new File(relativePath);
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
            final GpConfig gpConfig = ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);  
            final long maxNumBytes=initMaxNumBytes(contentLength, userContext);
            final GpFilePath gpFilePath=writeUserUploadFile(gpConfig, userContext, in, path, maxNumBytes);
            final String location = UrlUtil.getHref(request, gpFilePath);
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
                    Response.status(Response.Status.LENGTH_REQUIRED).build());
        }
        long numBytes = -1L;
        long maxNumBytes = MAX_FILE_SIZE_DEFAULT;
        numBytes=Long.parseLong(contentLength);
        maxNumBytes=ServerConfigurationFactory.instance().getGPLongProperty(userContext, PROP_MAX_FILE_SIZE, MAX_FILE_SIZE_DEFAULT);
        if (numBytes > maxNumBytes) {
            throw new WebApplicationException(
                    Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build());
        }
        return maxNumBytes;
    }

    ////////////////////////////////////////////////////////////////
    // Helper methods for adding user upload files to GenePattern
    ////////////////////////////////////////////////////////////////
    GpFilePath writeUserUploadFile(final GpConfig gpConfig, final GpContext userContext, final InputStream in, final String path, final long maxNumBytes) 
    throws GpFilePathException 
    {
        JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, userContext);
        
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
    GpFilePath writeJobInputFile(final GpConfig gpConfig, final GpContext userContext, final InputStream in, final String filename, final long maxNumBytes) 
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
            gpFilePath=createJobInputDir(gpConfig, userContext, filename);
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }

        // save it
        writeBytesToFile(userContext, in, gpFilePath, maxNumBytes);
        return gpFilePath;
    }
    
    
    /**
     * This method saves the data file uploaded from the REST client into a 
     * job output directory within the job_results folder.  The user must own the job
     * or an exception is thrown.
     * 
     * @param userContext
     * @param in
     * @return
     * @throws Exception
     */
    GpFilePath writeJobOutputFile(final GpConfig gpConfig, final GpContext userContext, final InputStream in, final String filename, final JobInfo jobInfo, GpContext jobContext, final long maxNumBytes) 
    throws WebApplicationException
    {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userContext.userId not set");
        }
        
        File expectedJobDir = new File(GenePatternAnalysisTask.getJobDir(gpConfig, jobContext, ""+jobInfo.getJobNumber()));
        if (!expectedJobDir.exists()) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND).entity("Job directory not found").build());
        }
        File absJobResultFile = new File(expectedJobDir, filename);
        File relJobResultFile = new File(filename);
        GpFilePath gpResultFilePath=null;
        try {
            //File userRootDir = ServerConfigurationFactory.instance().getRootJobDir(jobContext);
            //File rel = FileUtil.relativizePath(userRootDir, jobResultFile);
            
            gpResultFilePath=new JobResultFile(jobInfo, relJobResultFile);
            //createJobInputDir(gpConfig, userContext, filename);
        }
        catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }

        
        
        try {
            // save it
            writeToFile(in, gpResultFilePath.getServerFile().getCanonicalPath(), maxNumBytes);
           
        }
        catch (Throwable e) {
            log.error("Error writing output file to disk, filename="+gpResultFilePath.getRelativePath(), e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        try {
            // and save it to the database as well
            final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance();
            final boolean isInTransaction=mgr.isInTransaction();
            if (!isInTransaction) mgr.beginTransaction();
            JobOutputDao dao=new JobOutputDao(mgr);
            JobOutputFile jof =  JobOutputFile.from( ""+jobInfo.getJobNumber(), expectedJobDir, relJobResultFile, GpFileType.FILE);
            dao.recordOutputFile(jof);
            if (!isInTransaction) {
                mgr.commitTransaction();
            } 
            JobObjectCache.removeJobFromCache(jobInfo.getJobNumber());
        }
        catch (Throwable e) {
            log.error("Error saving record of output file to DB, filename="+gpResultFilePath.getRelativePath(), e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        
        // final step, we need to add an analysis parameter of mode out to the jobs analysis parameters
        // in the database
        try {
            final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
            final boolean isInTransaction = mgr.isInTransaction();
            if (!isInTransaction)
                mgr.beginTransaction();
            AnalysisDAO dao = new AnalysisDAO(mgr);
            JobInfo jobInfo2 = dao.getJobInfo(jobInfo.getJobNumber());
            if (jobInfo2 == null) {
                throw new WebApplicationException(
                        Response.status(Response.Status.NOT_FOUND).entity("Job not found").build());
            }
           AnalysisJob theJob= dao.getAnalysisJob(jobInfo.getJobNumber());
           addFileToOutputParameters(jobInfo2, filename, filename);
           dao.updateJob(jobInfo.getJobNumber(), jobInfo2.getParameterInfo(), theJob.getJobStatus().getStatusId());
            
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            
        } catch (Throwable e) {
            log.error("Error adding record of output file to AnalysisJob in DB, filename="+gpResultFilePath.getRelativePath(), e);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        // and if there is an externalFileManager in play, lets push that up
        // to the external file system
        try {
            ExternalFileManager externalFileManager = DataManager.getExternalFileManager(jobContext);
            
            if (externalFileManager != null)
                externalFileManager.syncLocalFileToRemote(jobContext, gpResultFilePath.getServerFile(), true);   
            
        }
        catch (Throwable e) {
            log.error("Error sync-ing file from local to S3, filename=" + gpResultFilePath.getRelativePath(), e);
            // don't throw an error, this should still be OK as long as there is disk space
        }
        
        return gpResultFilePath;
    }
    
   
    private static void addFileToOutputParameters(JobInfo jobInfo, String fileName, String label) {
        if (jobInfo == null) {
            log.error("null jobInfo arg!");
            return;
        }
        fileName = jobInfo.getJobNumber() + "/" + fileName;
        ParameterInfo paramOut = new ParameterInfo(label, fileName, "");
        paramOut.setAsOutputFile();
        
      
        ParameterInfo[] params =  jobInfo.getParameterInfoArray();
        for (int i=0; i< params.length; i++){
            if (params[i].isOutputFile()){
                if (params[i].getName().equals(label) && params[i].getValue().equals(fileName)){
                    // don't add it again
                    params[i] = paramOut;
                    return;
                }
            }
            
        }
        
        jobInfo.addParameterInfo(paramOut);
    }

    

    private String createPipelineMessage(final GpConfig gpConfig, final GpContext userContext, List<ParameterInfo> params) throws UnsupportedEncodingException {
        String toReturn = "";
        long maxFileSize = gpConfig.getGPLongProperty(userContext, "pipeline.max.file.size", 250L * 1000L * 1024L);
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
        // Fix for when the preceding slash is missing from the path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        try {
            final HibernateSessionManager mgr=HibernateUtil.instance();
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext=Util.getUserContext(request);

            final GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, "<GenePatternURL>" + path);
            final Integer jobNumber = Integer.parseInt(((JobResultFile) filePath).getJobId());

            // Set the pipeline name
            if (pipelineName == null) {
                pipelineName = "job_" + jobNumber;
            }
            ProvenanceFinder.ProvenancePipelineResult pipelineResult = new LocalAnalysisClient(mgr, gpConfig, userContext.getUserId()).createProvenancePipeline(jobNumber.toString(), pipelineName);
            String lsid = pipelineResult.getLsid();
            String message = createPipelineMessage(gpConfig, userContext, pipelineResult.getReplacedParams());
            if (lsid == null) {
                return Response.status(500).entity("Unable to create pipeline: " + filePath.getName()).build();
            }

            response.setHeader("pipeline-forward", request.getContextPath() + "/pipeline/index.jsf?lsid=" + lsid);
            return Response.ok().entity("Created Pipeline: " + pipelineName + " " + message).build();
        }
        catch (Exception ex) {
            return Response.status(500).entity("Unable to create pipeline: " + ex.getLocalizedMessage()).build();
        }
    }
    
    public static GpFilePath createJobInputDir(final GpConfig gpConfig, final GpContext userContext, final String filename) 
    throws Exception
    {
        GpFilePath tmpDir=null;
        try {
            tmpDir=JobInputFileUtil.createTmpDir(gpConfig, userContext);
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
            job_input_file=GpFileObjFactory.getUserUploadFile(gpConfig, userContext, relativeFile);
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
    
    public static GpFilePath moveSoapAttachmentToUserUploads(final GpConfig gpConfig, final GpContext userContext, final File fromFile, final String toFilename) throws Exception {
        GpFilePath toPath=DataResource.createJobInputDir(gpConfig, userContext, toFilename);
        boolean success=fromFile.renameTo(toPath.getServerFile());
        if (!success) {
            log.error("Failed to rename soap attachment file from "+fromFile+" to "+toPath.getServerFile());
            //delete the local file
            boolean deleted=fromFile.delete();
            if (!deleted) {
                log.error("Failed to delete original soap_attachment_file "+fromFile);
            }
            throw new Exception("Error saving soap attachment file to user upload directory, userId="
                    +userContext.getUserId()
                    +", fromFile="+fromFile);
        }
        return toPath;
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
                    Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).build());
        }
        catch (WriteToFileException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        catch (Throwable t) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
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
            final HibernateSessionManager mgr=HibernateUtil.instance();
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            JobInputFileUtil.addUploadFileToDb(mgr, gpConfig, userContext, gpFilePath);
        }
        catch (Throwable t) {
            log.error("Error saving record of job_input_file to DB, filename="+gpFilePath.getRelativePath(), t);
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
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
    ////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
    private void debugHeaders(final HttpServletRequest request) {
        //for debugging
        Enumeration<?> hNames = request.getHeaderNames();
        while (hNames.hasMoreElements()) {
            final String hName = (String) hNames.nextElement();
            final String hVal = request.getHeader(hName);
            System.out.println(hName+": "+hVal);
        }
    }
    
    @SuppressWarnings("unused")
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
