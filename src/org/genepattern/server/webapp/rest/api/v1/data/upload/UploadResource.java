package org.genepattern.server.webapp.rest.api.v1.data.upload;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

/**
 * RESTful implementation of the /upload resource.
 *
 * @author Thorin Tabor
 */
@Path("/v1/upload")
public class UploadResource {
    final static private Logger log = Logger.getLogger(UploadResource.class);

    /**
     * Get the GenePattern file path to which to upload the file
     *
     * @param userContext
     * @param uploadDirPath
     * @return
     * @throws org.apache.commons.fileupload.FileUploadException
     */
    private File getUploadDirectory(GpContext userContext, String uploadDirPath) throws FileUploadException {
        if (uploadDirPath == null) {
            throw new FileUploadException("server error, missing session attribute 'uploadPath'");
        }

        GpFilePath dir;
        try {
            dir = GpFileObjFactory.getRequestedGpFileObj(uploadDirPath);


            // Handle special case for root uploads directory
            if (dir.getRelativeFile().getPath().equals("")) {
                dir = GpFileObjFactory.getUserUploadFile(userContext, new File("./"));
            }
        }
        catch (Exception e) {
            throw new FileUploadException("Could not get the appropriate directory path for file upload");
        }

        // lazily create directory if need be
        if (!dir.getServerFile().exists()) {
            boolean success = dir.getServerFile().mkdir();
            if (!success) {
                log.error("Failed to mkdir for dir=" + dir.getServerFile().getAbsolutePath());
                throw new FileUploadException("Could not get the appropriate directory for file upload");
            }
        }

        return dir.getRelativeFile();
    }

    /**
     * Protect against Jersey's tendency to decode %20 to spaces
     * @param path
     * @return
     */
    private String protectAgainstSpaces(String path) {
        return path.replaceAll(" ", "%20");
    }

    /**
     * Get the GenePattern file path to which to upload the file.
     *
     * @param userContext
     * @param uploadPath
     * @return
     * @throws FileUploadException
     */
    private GpFilePath getUploadFile(GpContext userContext, String uploadPath) throws FileUploadException {
        try {
            //special-case, block 'tmp'
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(uploadPath);
            if (DataManager.isTmpDir(uploadFilePath)) {
                throw new FileUploadException("Can't save file with reserved filename: " + uploadPath);
            }

            return uploadFilePath;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FileUploadException("Error initializing upload file reference for '" + uploadPath + "': "+e.getLocalizedMessage());
        }
    }

    private GpFilePath initUploadFromDB(GpContext userContext, GpFilePath uploadFilePath) throws Exception {
        return UserUploadManager.getUploadFileObj(userContext, uploadFilePath, false);
    }

    public File getTempDir() {
        String str = System.getProperty("java.io.tmpdir");
        return new File(str);
    }

    public File[] getFileParts(File uploadDir) {
        // Get the list of parts and sort
        File[] fileList = uploadDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                try { Integer.parseInt(name); }
                catch(NumberFormatException nfe) { return false; }
                return true;
            }
        });
        Arrays.sort(fileList, new Comparator<File>() {
            public int compare(File f1, File f2) {
                try {
                    int i1 = Integer.parseInt(f1.getName());
                    int i2 = Integer.parseInt(f2.getName());
                    return i1 - i2;
                } catch(NumberFormatException e) {
                    throw new AssertionError(e);
                }
            }
        });

        return fileList;
    }

    public File getUploadDir(String token) throws FileUploadException {
        File serverTempDir = getTempDir();
        File uploadDir = new File(serverTempDir, token);

        // Check to see if it is all good
        if (!uploadDir.exists()) {
            throw new FileUploadException("Upload directory does not exist: " + token);
        }
        if (!uploadDir.canWrite() || !uploadDir.canRead()) {
            throw new FileUploadException("Missing permissions on upload directory: " + token);
        }
        if (!uploadDir.isDirectory()) {
            throw new FileUploadException("Provided upload token does not match a directory: " + token);
        }

        return uploadDir;
    }

    public JSONObject getStatusObject(GpContext userContext, String token, String path, GpFilePath file, File uploadDir) throws Exception {
        // Make list of missing and received
        JSONArray missing = new JSONArray();
        JSONArray received = new JSONArray();
        for (Integer i = 0; i < file.getNumParts(); i++) {
            File part = new File(uploadDir, i.toString());
            if (part.exists()) {
                received.put(i);
            }
            else {
                missing.put(i);
            }
        }

        // Create the status object to return
        JSONObject toReturn = new JSONObject();
        toReturn.put("missing", missing);
        toReturn.put("received", received);
        toReturn.put("token", token);
        toReturn.put("path", path);

        return toReturn;
    }

    /**
     * Append the contents of the fileItem to the given file.
     *
     * @param is - an InputStream with the file's binary data
     * @param to - the partial file to which to append the bytes
     * @throws java.io.IOException
     */
    private void appendPartition(InputStream is, File to) throws IOException {
        OutputStream os = null;

        try {
            os = new BufferedOutputStream(new FileOutputStream(to, true));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        catch (Throwable t) {
            log.error("Something thrown while writing file chunk: " + t.getLocalizedMessage(), t);
        }
        finally {
            is.close();
            os.close();
        }
    }

    /**
     * Creates a multipart user upload resource
     *
     * Returns a unique token for this multipart upload
     * { token: "the_token", path: "the_path" }
     *
     * @param request
     * @return
     */
    @POST
    @Path("multipart/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startMultipartUpload(@Context HttpServletRequest request, @QueryParam("path") String path, @QueryParam("parts") int parts) {
        try {
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be uploading to
            path = protectAgainstSpaces(path);
            GpFilePath file = getUploadFile(userContext, path);

            // Check if the file exists and throw an error if it does
            if (file.getServerFile().exists() && file.getServerFile().length() > 0) {
                throw new FileUploadException("File already exists");
            }

            // Create the temp directory for the upload
            File fileTempDir = ServerConfigurationFactory.instance().getTemporaryUploadDir(userContext);

            // Return the JSON response with the upload token
            JSONObject returnObject = new JSONObject();
            returnObject.put("token", fileTempDir.getName());
            returnObject.put("path", path);
            return Response.ok().entity(returnObject.toString()).build();
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Put a chunk of the file to the upload resource
     * Requires: upload token in param string
     * Returns: list of missing file chunks
     * @param request - The HttpServletRequest
     * @param token
     * @param path
     * @return
     */
    @PUT
    @Path("multipart/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateMultipartUpload(@Context HttpServletRequest request, @QueryParam("token") String token, @QueryParam("path") String path, @QueryParam("index") Integer index, @QueryParam("parts") int parts) {
        try {
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be uploading to
            path = protectAgainstSpaces(path);
            GpFilePath file = getUploadFile(userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(token);

            // Create the file to write
            File toWrite = new File(uploadDir, index.toString());

            // Check to see if it already exists and throw an error if it does
            if (toWrite.exists()) {
                throw new FileUploadException("File chunk already exists: " + token + " path: " + path + " index: " + index);
            }

            // Write the file
            InputStream is = request.getInputStream();
            appendPartition(is, toWrite);

            // Return the status object
            JSONObject toReturn =  getStatusObject(userContext, token, path, file, uploadDir);

            return Response.ok().entity(toReturn.toString()).build();
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Get the status of the multipart upload
     * Return a list of missing chunks, received chunks, token and path
     *
     * @param request
     * @param token
     * @param path
     * @return
     */
    @GET
    @Path("multipart/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMultipartStatus(@Context HttpServletRequest request, @QueryParam("token") String token, @QueryParam("path") String path, @QueryParam("parts") int parts) {
        try {
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be writing to
            path = protectAgainstSpaces(path);
            GpFilePath file = getUploadFile(userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(token);

            // Get the status object
            JSONObject toReturn = getStatusObject(userContext, token, path, file, uploadDir);

            return Response.ok().entity(toReturn.toString()).build();
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    @POST
    @Path("multipart/assemble/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response assembleMultipartUpload(@Context HttpServletRequest request, @QueryParam("path") String path, @QueryParam("token") String token, @QueryParam("parts") int parts) {
        try {
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be writing to
            path = protectAgainstSpaces(path);
            GpFilePath file = getUploadFile(userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(token);

            // Get the status object
            JSONObject status = getStatusObject(userContext, token, path, file, uploadDir);

            // See if any parts are still missing and throw an error if they are
            if (status.getJSONArray("missing").length() > 0) {
                throw new FileUploadException("Cannot assemble, parts still missing: " + token + " parts: " + status.getJSONArray("missing").toString());
            }

            // Check to see if it already exists and throw an error if it does
            if (file.getServerFile().exists()) {
                throw new FileUploadException("Upload file already exists: " + token + " path: " + path);
            }

            // Get the list of parts and sort
            File[] fileList = getFileParts(uploadDir);

            // Write the file
            for (File i : fileList) {
                FileInputStream fileIS = new FileInputStream(i);
                appendPartition(fileIS, file.getServerFile());
            }

            // Update the database
            UserUploadManager.createUploadFile(userContext, file, fileList.length);
            UserUploadManager.updateUploadFile(userContext, file, fileList.length, fileList.length);

            // Delete the temp directory, since we no longer need it
            FileUtils.deleteDirectory(uploadDir);

            return Response.ok().entity(status.toString()).build();
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }
}
