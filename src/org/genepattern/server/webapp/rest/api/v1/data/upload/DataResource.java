package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

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
 * @author pcarr
 *
 */
@Path("/v1/data")
public class DataResource {
    final static private Logger log = Logger.getLogger(DataResource.class);

    /**
     * Add a new file to be used as input to a job. Return the URI for the uploaded file in the 'Location'
     * header of the response.
     *
     * TODO: Implement more validation, for example,
     *     - when the max file size limit for individual file upload is exceeded
     *     - when the file will cause the current user's disk quota to be exceeded (as configured by the GP server)
     *     - when the file cannot be written because the OS disk quota is exceeded
     *     - when the file write operation fails because of a server timeout
     *     - other system errors, for example, we require access to the GP DB so that we can record the file record
     * 
     * Requires authentication with a valid gp user id.
     * 
     * Expected response codes:
     *     ?, this method requires authentication, if there is not a valid gp user logged in, respond with basic authentication request.
     *     201 - Created, when the file is successfully uploaded.
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
            final @QueryParam("name") String filename,
            final InputStream in) 
    {
        //for debugging
        debugHeaders(request);

        //by default create a new directory in the ./tmp dir for the current user
        //return uri for the file
        try {
            final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);        
            final GpFilePath tmpDir=JobInputFileUtil.createTmpDir(userContext);
            final String path=tmpDir.getRelativePath() + "/" + filename;
            GpFilePath gpFilePath=createUserUploadFile(userContext, in, path);
            String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Add a new file to the uploads directory of the current user, specifically when you 
     * want to use the file as a job input file (in a subsequent call to add a job).
     * 
     * This will create a new resource each time the method is called.
     * 
     * Example usage:
     * <pre>
       curl -X POST --form file=@all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input
       </pre>
     * The '-X POST' is redundant when using the '--form' option. This will work also.
     * <pre>
       curl --form file=@all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/v1/data/upload/job_input
     * </pre>
     * 
     * @param request
     * @param path
     * @param uploadedInputStream
     * @param fileDetail
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload/job_input_form") 
    public Response handlePostJobInputMultipartForm(
            final @Context HttpServletRequest request,
            final @FormDataParam("file") InputStream in,
            final @FormDataParam("file") FormDataContentDisposition fileDetail) 
    {
        //for debugging
        debugHeaders(request);

        //by default create a new directory in the ./tmp dir for the current user
        //return uri for the file
        try {
            final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);        
            final GpFilePath tmpDir=JobInputFileUtil.createTmpDir(userContext);
            final String path=tmpDir.getRelativePath() + "/" + fileDetail.getFileName();
            GpFilePath gpFilePath=createUserUploadFile(userContext, in, path);
            String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Add a file to the uploads directory of the current user. Example usage,
     * <pre>
     * curl -X PUT --data-binary @all_aml_test.cls -u test:test http://127.0.0.1:8080/gp/rest/data/upload/tmp/all_aml_test.cls
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
            final @PathParam("path") String path,
            final @DefaultValue("false") @QueryParam("replace") boolean replace,
            final InputStream in) 
    {
        
        //for debugging
        //debugHeaders(request);
        //debugContent(in);
        
        try {
            final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);        
            GpFilePath gpFilePath=createUserUploadFile(userContext, in, path);
            String location = ""+gpFilePath.getUrl().toExternalForm(); 
            return Response.status(201)
                    .header("Location", location)
                    .entity(location).build();
        }
        catch (Throwable t) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }
    
//    @POST
//    @Path("/upload_a")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response uploadFile(
//        @FormDataParam("file") InputStream uploadedInputStream,
//        @FormDataParam("file") FormDataContentDisposition fileDetail,
//        @Context HttpServletRequest request) {
//
//        try {
//        final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);
//        JobInputFileUtil fileUtil = new JobInputFileUtil(userContext);
//        
//        File relativePath = new File(fileDetail.getFileName());
//        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(relativePath);
//
//        // save it
//        writeToFile(uploadedInputStream, gpFilePath.getServerFile().getCanonicalPath());
//        fileUtil.updateUploadsDb(gpFilePath);
// 
//        String location = ""+gpFilePath.getUrl().toExternalForm();
// 
//        return Response.status(201)
//                .header("Location", location)
//                .entity(location).build();
//        }
//        catch (Throwable t) {
//            throw new WebApplicationException(
//                Response.status(Response.Status.BAD_REQUEST)
//                    .entity(t.getLocalizedMessage())
//                    .build());
//        }
// 
//    }
 
//    @POST
//    @Path("/upload_test")
//    public Response uploadFileTest(
//        @Context HttpServletRequest request) 
//    {
//        try {
//            final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);
//            JobInputFileUtil fileUtil = new JobInputFileUtil(userContext);
//        
//            //File relativePath = new File(fileDetail.getFileName());
//            //GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(relativePath);
//
//            // save it
//            //writeToFile(uploadedInputStream, gpFilePath.getServerFile().getCanonicalPath());
//            //fileUtil.updateUploadsDb(gpFilePath);
// 
//            //String location = ""+gpFilePath.getUrl().toExternalForm();
//            
//            String location="not saved!";
// 
//            return Response.status(201)
//                    .header("Location", location)
//                    .entity(location).build();
//        }
//        catch (Throwable t) {
//            throw new WebApplicationException(
//                Response.status(Response.Status.BAD_REQUEST)
//                    .entity(t.getLocalizedMessage())
//                    .build());
//        }
// 
//    }
 

    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream,
        String uploadedFileLocation) {

        try {
            OutputStream out = new FileOutputStream(new File(
                    uploadedFileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
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


    ////////////////////////////////////////////////////////////////
    // Helper methods for adding user upload files to GenePattern
    // TODO: should refactor these methods into an interface
    ////////////////////////////////////////////////////////////////
    GpFilePath createUserUploadFile(final ServerConfiguration.Context userContext, final InputStream in, final String path) 
    throws Exception
    {
        JobInputFileUtil fileUtil = new JobInputFileUtil(userContext);
        
        File relativePath = new File(path);
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(relativePath);

        // save it
        writeToFile(in, gpFilePath.getServerFile().getCanonicalPath());
        fileUtil.updateUploadsDb(gpFilePath);
        return gpFilePath;
    }
}
