/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.UserUploadFile;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.util.LSID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * RESTful implementation of the /upload resource.
 *
 * @author Thorin Tabor, modified by Ted Liefeld
 */
@Path("/v1/upload")
public class UploadResource {
    final static private Logger log = Logger.getLogger(UploadResource.class);

    /**
     * Get the GenePattern file path to which to upload the file.
     *
     * @param userContext
     * @param uploadPath
     * @return
     * @throws FileUploadException
     */
    private GpFilePath getUploadFile(final GpConfig gpConfig, final GpContext userContext, final String uploadPath) throws FileUploadException {
        try {
            //special-case, block 'tmp'
            GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, uploadPath, (LSID)null);
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

    protected File getTempDir(final GpConfig gpConfig) {
        String str = gpConfig.getTempDir(GpContext.getServerContext()).getAbsolutePath();
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

    protected File getUploadDir(final GpConfig gpConfig, final String token) throws FileUploadException {
        File serverTempDir = getTempDir(gpConfig);
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
            t.printStackTrace();
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
    public Response startMultipartUpload(
            @Context HttpServletRequest request, 
            final @QueryParam("path") String path, 
            @QueryParam("parts") int parts,
            @QueryParam("fileSize") long fileSize) {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);
            if (log.isDebugEnabled()) {
                log.debug("path="+path);
                log.debug("parts="+parts);
                log.debug("fileSize=" + fileSize);
            }

            checkDiskQuota(gpConfig, userContext, fileSize);

            GpFilePath file = getUploadFile(gpConfig, userContext, path);

            // Check if the file exists and throw an error if it does
            if (file.getServerFile().exists() && file.getServerFile().length() > 0) {
                throw new FileUploadException("File already exists");
            }

            // Create the temp directory for the upload
            File fileTempDir = gpConfig.getTemporaryUploadDir(userContext);

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
    public Response updateMultipartUpload(
            @Context HttpServletRequest request, 
            @QueryParam("token") String token, 
            @QueryParam("path") String path, 
            @QueryParam("index") Integer index, 
            @QueryParam("parts") int parts) 
    {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            
            // Get the file we will be uploading to
            if (log.isDebugEnabled()) {
                log.debug("token="+token);
                log.debug("path="+path);
                log.debug("index="+index);
                log.debug("parts="+parts);
            }
            GpFilePath file = getUploadFile(gpConfig, userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(gpConfig, token);

            // Create the file to write
            File toWrite = new File(uploadDir, index.toString());

            // Check to see if it already exists and throw an error if it does
            if (toWrite.exists()) {
                //throw new FileUploadException("File chunk already exists: " + token + " path: " + path + " index: " + index);
                
                // JTL try just ignoring duplicates
                // JSONObject toReturn =  getStatusObject(userContext, token, path, file, uploadDir);
                // System.out.println("returning after quota");
                // return Response.ok().entity(toReturn.toString()).build();
                System.out.println("IGNORING -- File chunk already exists: " + token + " path: " + path + " index: " + index);
            }

            // Write the file
            InputStream is = request.getInputStream();
            appendPartition(is, toWrite);

            System.out.println("======= finished  " + toWrite.getTotalSpace());
            
           
            checkDiskQuota(gpConfig, userContext, toWrite.length());

            // Return the status object
            JSONObject toReturn =  getStatusObject(userContext, token, path, file, uploadDir);
            System.out.println("returning after quota");
            return Response.ok().entity(toReturn.toString()).build();
           
        }
        catch (Throwable t) {
            t.printStackTrace();
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
    public Response getMultipartStatus(
            @Context HttpServletRequest request, 
            @QueryParam("token") String token, 
            @QueryParam("path") String path, 
            @QueryParam("parts") int parts) {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be writing to
            if (log.isDebugEnabled()) {
                log.debug("token="+token);
                log.debug("path="+path);
                log.debug("parts="+parts);
            }
            GpFilePath file = getUploadFile(gpConfig, userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(gpConfig, token);

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
    public Response assembleMultipartUpload(
            @Context HttpServletRequest request, 
            @QueryParam("path") String path, 
            @QueryParam("token") String token, 
            @QueryParam("parts") int parts
    ) {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be writing to
            if (log.isDebugEnabled()) {
                log.debug("token="+token);
                log.debug("path="+path);
                log.debug("parts="+parts);
            }
            
            System.out.println("===================== ASSEMBLE MULTIPART ");
            System.out.println("token="+token);
            System.out.println("path="+path);
            System.out.println("parts="+parts);
      
            
            GpFilePath file = getUploadFile(gpConfig, userContext, path);
            file.setNumParts(parts);

            // Get the temp directory for the upload
            File uploadDir = getUploadDir(gpConfig, token);

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


            //check again to see that the disk quota is not exceeded
            //we need to total the size of all the file chunks first
            long totalSize = 0;
            for (File myfile : fileList) {
                if(myfile != null)
                {
                    System.out.println("assemble 1: "+ myfile.getAbsolutePath()+ " -- " + myfile.length());
                    totalSize += myfile.length();
                }
            }
            checkDiskQuota(gpConfig, userContext, totalSize);

            // Write the file
            for (File i : fileList) {
                System.out.println("assemble 2 : "+ i.getAbsolutePath()+ " -- " + i.length());
                FileInputStream fileIS = new FileInputStream(i);
                appendPartition(fileIS, file.getServerFile());
            }


            // Update the database
            final HibernateSessionManager mgr=HibernateUtil.instance();
            UserUploadManager.createUploadFile(mgr, userContext, file, fileList.length);
            UserUploadManager.updateUploadFile(mgr, userContext, file, fileList.length, fileList.length);

            // Delete the temp directory, since we no longer need it
            FileUtils.deleteDirectory(uploadDir);

            return Response.ok().entity(status.toString()).build();
        }
        catch (Throwable t) {
            t.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Post a file to the upload resource
     * @param request - The HttpServletRequest
     * @param path
     * @return
     */
    @POST
    @Path("/whole")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeUpload(
            @QueryParam("path") String path,
            @Context HttpServletRequest request)
    {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be uploading to
            if (log.isDebugEnabled()) {
                log.debug("path="+path);
            }

            GpFilePath file = getUploadFile(gpConfig, userContext, path);
            if(file.getServerFile() != null && file.getServerFile().exists())
            {
                throw new FileUploadException("File already exists: " + file.getServerFile().getName());
            }

            // Get the temp directory for the upload
            File fileTempDir = gpConfig.getTemporaryUploadDir(userContext);

            // Create the file to write
            File toWrite = File.createTempFile("cls", null, fileTempDir);

            // Write the file to temporary location

            InputStream inputStream = request.getInputStream();
            appendPartition(inputStream, toWrite);

            checkDiskQuota(gpConfig, userContext, toWrite.length());

            //now move the file to it's permanent location
            boolean success = toWrite.renameTo(file.getServerFile());
            if(success)
            {
                //update the user uploads database
                JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, userContext);
                fileUtil.updateUploadsDb(HibernateUtil.instance(), file);
            }
            else
            {
                throw new Exception("Error saving file: " + file.getName());
            }

            JSONObject toReturn = new JSONObject();
            toReturn.append("success", true);
            // Return the status object
            return Response.ok().entity(toReturn.toString()).build();
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    private void checkDiskQuota(GpConfig gpConfig, GpContext userContext, long fileSizeBytes) throws Exception
    {
        //first check if the disk quota is or will be exceeded
        DiskInfo diskInfo = DiskInfo.createDiskInfo(gpConfig, userContext);

        if(diskInfo.isAboveQuota())
        {
            //disk usage exceeded so abort the file upload
            throw new FileUploadException("Disk usage quota exceeded. Disk Usage:" + diskInfo.getDiskUsageFilesTab().getDisplayValue().toUpperCase()
                    + ". Quota: " + diskInfo.getDiskQuota().getDisplayValue().toUpperCase() + ". Please delete some files from the Files Tab.");
        }

        if(diskInfo.isAboveQuota(fileSizeBytes))
        {
            //disk usage exceeded so abort the fail upload
            throw new FileUploadException("Uploading file will cause disk usage to be exceeded. Disk Usage:" + diskInfo.getDiskUsageFilesTab().getDisplayValue().toUpperCase()
                    + ". Quota: " + diskInfo.getDiskQuota().getDisplayValue().toUpperCase() + ". Please delete some files from the Files Tab.");
        }

    }
    
    
    /* ================ additions below to support resumablejs =====================*/
    /**
     * Creates a multipart user upload resource fore resumablejs
     *
     * 
     *
     * @param request
     * @return
     */
    @POST
    @Path("resumable/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumable(
            @Context HttpServletRequest request) {
        try {
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);
            
            String path = request.getParameter("target");
            ResumableInfo info = getResumableInfo(request, gpConfig);
            
            GpFilePath file = getUploadFile(gpConfig, userContext, path);      
            
            checkDiskQuota(gpConfig, userContext, info.resumableTotalSize);
        
            int resumableChunkNumber        = getResumableChunkNumber(request);
           
            RandomAccessFile raf = new RandomAccessFile(info.resumableFilePath, "rw");
            
            //Seek to position
            raf.seek((resumableChunkNumber - 1) * (long)info.resumableChunkSize);

            //Save to file
            InputStream is = request.getInputStream();
            long readed = 0;
            long content_length = request.getContentLength();
            byte[] bytes = new byte[1024 * 100];
            while(readed < content_length) {
                int r = is.read(bytes);
                if (r < 0)  {
                    break;
                }
                raf.write(bytes, 0, r);
                readed += r;
            }
            raf.close();


            //Mark as uploaded.
            info.uploadedChunks.add(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber));
           
            // pass in the files final location
            if (info.checkIfUploadFinished()) { //Check if all chunks uploaded, and change filename
                ResumableInfoStorage.getInstance().remove(info);
                
                // GpFilePath finalFile = getUploadFile(gpConfig, userContext, info.destinationTargetPath + info.resumableFilename);      
                
                String uploadRelPath = getUploadRelativePath(info.destinationFilePath, userContext.getUserId());

               
                
                File serverFile = new File(info.destinationFilePath+File.separator + info.resumableFilename);
                UserUploadFile finalFile = GpFileObjFactory.getUploadedFilePath(userContext, info, uploadRelPath, serverFile);
                
                
                finalFile.setName(info.resumableFilename);
                
                // Update the database - lengths are 1 since resumable already assembled it
                final HibernateSessionManager mgr=HibernateUtil.instance();
                UserUploadManager.createUploadFile(mgr, userContext, finalFile, 1, true);
                UserUploadManager.updateUploadFile(mgr, userContext, finalFile, 1, 1);
                
                return Response.ok().entity("All finished.").build();
            } else {
                //response.getWriter().print("Upload");
                return  Response.ok().entity("Upload.").build();
                
            }
            
            
        } catch (Throwable t){
            t.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }


    
    private String getUploadRelativePath(String destPath, String username){
        String root = "/users/" + username + "/uploads";
        int idx = destPath.indexOf(root);
        String path =  destPath.substring(idx + root.length());
        if (path.length() ==0)  return  ".";
        else return path.substring(1);
    }

    
    
    @GET
    @Path("resumable/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumableGet( @Context HttpServletRequest request) throws Exception{
        int resumableChunkNumber        = getResumableChunkNumber(request);
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        ResumableInfo info = getResumableInfo(request, gpConfig);

        if (info.uploadedChunks.contains(new ResumableInfo.ResumableChunkNumber(resumableChunkNumber))) {
            // response.getWriter().print("Uploaded."); //This Chunk has been Uploaded.
            return  Response.ok().entity("Uploaded.").build();
        } else {
            //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        
        
    }
    
    private ResumableInfo getResumableInfo(HttpServletRequest request, final GpConfig gpConfig) throws Exception {
        GpContext userContext = Util.getUserContext(request);
        
        File fileTempDir = gpConfig.getTemporaryUploadDir(userContext);
        String base_dir =  fileTempDir.getAbsolutePath();
        
        int resumableChunkSize          = ResumableHttpUtils.toInt(request.getParameter("resumableChunkSize"), -1);
        long resumableTotalSize         = ResumableHttpUtils.toLong(request.getParameter("resumableTotalSize"), -1);
        String resumableIdentifier      = request.getParameter("resumableIdentifier");
        String resumableFilename        = request.getParameter("resumableFilename");
        String resumableRelativePath    = request.getParameter("resumableRelativePath");
        
        String path              = request.getParameter("target");
        GpFilePath file = getUploadFile(gpConfig, userContext, path);      
        
        
        //Here we add a ".temp" to every upload file to indicate NON-FINISHED
        new File(base_dir).mkdir();
        String resumableFilePath        = new File(base_dir, resumableFilename).getAbsolutePath() + ".temp";

        ResumableInfoStorage storage = ResumableInfoStorage.getInstance();

        ResumableInfo info = storage.get(resumableChunkSize, resumableTotalSize, 
                            resumableIdentifier, resumableFilename, resumableRelativePath, 
                            resumableFilePath, file.getServerFile().getAbsolutePath(), path);
        
       
        
        if (!info.vaild())         {
            storage.remove(info);
            throw new Exception("Invalid request params.");
        }
        return info;
    }    

    private int getResumableChunkNumber(HttpServletRequest request) {
        return ResumableHttpUtils.toInt(request.getParameter("resumableChunkNumber"), -1);
    }
    
    // *******************  below additions for direct to external (e.g. S3) uploads ***********************
    //  currently this is a bit S3 centric but it can be updated and generalized if/when we start
    //  trying to deploy to the Google Cloud Platform (GCP)
    @GET
    @Path("getExternalUploadUrl/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getS3UploadUrl(
            @Context HttpServletRequest request, 
            @QueryParam("path") String rawPath, 
            @QueryParam("fileType") String mimeType,
            @QueryParam("numParts") int numParts
    )
    {
        File tmp = null;
        Process proc = null;
        try {
            String path = URIUtil.encodePath(rawPath);
            //String path = rawPath;

            // we want a temp file name but the file itself will block
            tmp = File.createTempFile("lambda", ".json");
            String filename = tmp.getName();
            tmp.delete();

            // need to get the bucket from the config file entries
            //     upload.aws.s3.bucket: gp-temp-test-bucket/tedslaptop
            //     upload.aws.s3.bucket.root: tedslaptop
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);            
            String bucket = getBucketName(gpConfig, userContext);
            String bucketRoot = getBucketRoot(gpConfig, userContext);

            String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");
            String signingScript = gpConfig.getGPProperty(userContext, "upload.aws.s3.presigning.script");

            GpFilePath gpFile = getUploadFile(gpConfig, userContext, path);  
            String fullPath = bucketRoot + gpFile.getServerFile().getAbsolutePath();

            JSONObject json = new JSONObject();
            json.put("bucket", bucket);
            json.put("path", fullPath);
            json.put("contentType", mimeType);
            json.put("numParts", numParts);

            File tmpInput = File.createTempFile("beginUpload", ".json");

            BufferedWriter writer = new BufferedWriter(new FileWriter (tmpInput));
            writer.append(json.toString());
            writer.close();


            // NO BLANK SPACES IN THE PAYLOAD since it messes up the arg parsing
            StringBuffer execBuff = new StringBuffer();
            execBuff.append(awsScriptDir);
            execBuff.append(signingScript);
            execBuff.append(" ");
            execBuff.append(" -i ");
            execBuff.append(tmpInput.getAbsolutePath());  // $4
            execBuff.append(" -f ");
            execBuff.append(filename);   //$5

            proc = Runtime.getRuntime().exec(execBuff.toString());

            proc.waitFor();


            BufferedReader stdInput = new BufferedReader(new 
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                    InputStreamReader(proc.getErrorStream()));

            // Read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            String resp = readOutputFileToString(filename);
            System.out.println(resp);
            return Response.ok().entity(resp).build();

        } catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (proc != null) debugProcessStdOutAndErr(proc);
            try {
                if (tmp != null)
                    tmp.delete();
            } catch (Exception e){}
        }


    }

    private String getBucketName(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        // pull the bucket name out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(5,endIdx);
    }
    private String getBucketRoot(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        // pull the bucket root path out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(endIdx+1);
    }
    
    //  currently this is a bit S3 centric but it can be updated and generalized if/when we start
    //  trying to deploy to the Google Cloud Platform (GCP)
    @GET
    @Path("startS3MultipartUpload/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startS3MultipartUpload(
            @Context HttpServletRequest request, 
            @QueryParam("path") String rawPath
    )
    {
        File tmp = null;
        File tmpInput = null;
        Process proc = null;
        try {
            String path = URIUtil.encodePath(rawPath);
            
            // we want a temp file name but the file itself will block
            tmp = File.createTempFile("lambda", ".json");
            String outfilename = tmp.getName();
            tmp.delete();

            // need to get the bucket from the config file entries
            //     upload.aws.s3.bucket: gp-temp-test-bucket/tedslaptop
            //     upload.aws.s3.bucket.root: tedslaptop
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);            
            String bucket = getBucketName(gpConfig, userContext);
            String bucketRoot = getBucketRoot(gpConfig, userContext);

            String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");
            String signingScript = "startS3MultipartUpload.sh" ;  // gpConfig.getGPProperty(userContext, "upload.aws.s3.presigning.script");

            GpFilePath gpFile = getUploadFile(gpConfig, userContext, path);  
            String fullPath = bucketRoot + gpFile.getServerFile().getAbsolutePath();

            // need to pass this into the python behind the shell script.  Shell script just sets the env for the python
            JSONObject json = new JSONObject();
            json.put("bucket", bucket);
            json.put("path", fullPath);

            tmpInput = File.createTempFile("beginUpload", ".json");

            BufferedWriter writer = new BufferedWriter(new FileWriter (tmpInput));
            writer.append(json.toString());
            writer.close();

            // NO BLANK SPACES IN THE PAYLOAD since it messes up the arg parsing
            StringBuffer execBuff = new StringBuffer();
            execBuff.append(awsScriptDir);
            execBuff.append(signingScript);
            execBuff.append(" ");
            execBuff.append(" -i ");
            execBuff.append(tmpInput.getAbsolutePath());  // $4
            execBuff.append(" -o ");
            execBuff.append(outfilename);   //$5

            proc = Runtime.getRuntime().exec(execBuff.toString());

            // give it some time but not too much
            try {
                proc.waitFor();
            
                // timeout, kill the process
                debugProcessStdOutAndErr(proc);
                proc.destroy();
                proc = null;
            } catch (InterruptedException ie) {
                debugProcessStdOutAndErr(proc);
                proc.destroy();
            }

            String resp = readOutputFileToString(outfilename);
            log.debug(resp);
            return Response.ok().entity(resp).build();

        } catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (proc != null) debugProcessStdOutAndErr(proc);
            try {
                if (!log.isDebugEnabled())  if (tmp != null) tmp.delete();   
            } catch (Exception e){}
            try {
                if (!log.isDebugEnabled()) if (tmpInput != null) tmpInput.delete();
            } catch (Exception e){}
        }
    }

    //  currently this is a bit S3 centric but it can be updated and generalized if/when we start
    //  trying to deploy to the Google Cloud Platform (GCP)
    @GET
    @Path("getS3MultipartUploadPresignedUrlOnePart/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getS3MultipartUploadPresignedUrlOnePart(
            @Context HttpServletRequest request, 
            @QueryParam("path") String rawPath,
            @QueryParam("partNum") Integer partNum,
            @QueryParam("uploadId") String uploadId
    )
    {
        File tmp = null;
        File tmpInput = null;
        Process proc = null;
        try {
            String path = URIUtil.encodePath(rawPath);
            
            // we want a temp file name but the file itself will block
            tmp = File.createTempFile("lambda", ".json");
            String outfilename = tmp.getName();
            tmp.delete();

            // need to get the bucket from the config file entries
            //     upload.aws.s3.bucket: gp-temp-test-bucket/tedslaptop
            //     upload.aws.s3.bucket.root: tedslaptop
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);            
            String bucket = getBucketName(gpConfig, userContext);
            String bucketRoot = getBucketRoot(gpConfig, userContext);

            String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");
            String signingScript = "getS3MultipartPresingedUploadURL.sh" ;  // gpConfig.getGPProperty(userContext, "upload.aws.s3.presigning.script");

            GpFilePath gpFile = getUploadFile(gpConfig, userContext, path);  
            String fullPath = bucketRoot + gpFile.getServerFile().getAbsolutePath();

            // need to pass this into the python behind the shell script.  Shell script just sets the env for the python
            JSONObject json = new JSONObject();
            json.put("bucket", bucket);
            json.put("path", fullPath);
            json.put("expiration", 7200); // get from props later XXX JTL
            json.put("uploadId", uploadId);
            json.put("partNum", partNum);
            
            tmpInput = File.createTempFile("getUploadUrl", ".json");

            BufferedWriter writer = new BufferedWriter(new FileWriter (tmpInput));
            writer.append(json.toString());
            writer.close();

            // NO BLANK SPACES IN THE PAYLOAD since it messes up the arg parsing
            StringBuffer execBuff = new StringBuffer();
            execBuff.append(awsScriptDir);
            execBuff.append(signingScript);
            execBuff.append(" ");
            execBuff.append(" -i ");
            execBuff.append(tmpInput.getAbsolutePath());  // $4
            execBuff.append(" -o ");
            execBuff.append(outfilename);   //$5

            proc = Runtime.getRuntime().exec(execBuff.toString());

            // give it some time but not too much
            try {
                proc.waitFor();
            
                // timeout, kill the process
                debugProcessStdOutAndErr(proc);
                proc.destroy();
                proc = null;
            } catch (InterruptedException ie) {
                debugProcessStdOutAndErr(proc);
                proc.destroy();
            }
            String resp = readOutputFileToString(outfilename);
            
            json.put("presignedUrl", resp);
            
            
            log.debug(resp);
            return Response.ok().entity(json.toString()).build();

        } catch (Exception e){
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (proc != null) debugProcessStdOutAndErr(proc);
            try {
                if (!log.isDebugEnabled())  if (tmp != null) tmp.delete();   
            } catch (Exception e){}
            try {
                if (!log.isDebugEnabled()) if (tmpInput != null) tmpInput.delete();
            } catch (Exception e){}
        }


    }

    
    
    

    
    
 
    
    
    /**
     * Post a file to the upload resource
     * @param request - The HttpServletRequest
     * @param path
     * @return
     */
    @POST
    @Path("/registerExternalUpload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response registerExternalUpload(
            String jsonPayload,
            @QueryParam("path") String rawPath,
            @QueryParam("length") String fileLength,
            @QueryParam("uploadId") String uploadId,
            @Context HttpServletRequest request)
    {
        Process proc = null;
        try {
            String path = URIUtil.encodePath(rawPath);
            // String path = rawPath;

            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            // Get the user context
            GpContext userContext = Util.getUserContext(request);

            // Get the file we will be uploading to
            if (log.isDebugEnabled()) {
                log.debug("path="+path);
            }

            GpFilePath file = getUploadFile(gpConfig, userContext, path);
            file.setFileLength(new Long(fileLength));
            file.setLastModified(new Date());

            String bucket = getBucketName(gpConfig, userContext);
            String bucketRoot = getBucketRoot(gpConfig, userContext);
            String fileName = bucketRoot + file.getServerFile().getAbsolutePath();
            System.out.println("Filename is " + fileName);

            JSONObject multipartCompletion = new JSONObject();
            multipartCompletion.put("UploadId", uploadId);

            // multipartCompletion.put("Key", URIUtil.encodePath(fileName));       
            multipartCompletion.put("Key", fileName);       

            multipartCompletion.put("Bucket", bucket);
            JSONArray parts = new JSONArray(jsonPayload);
            JSONObject uploadObj = new JSONObject();
            uploadObj.put("Parts", parts);
            multipartCompletion.put("MultipartUpload", uploadObj);

            File tmp = File.createTempFile("completeUpload", ".json");

            BufferedWriter writer = new BufferedWriter(new FileWriter (tmp));
            writer.append(multipartCompletion.toString());
            writer.close();
            String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");

            StringBuffer execBuff = new StringBuffer();
            execBuff.append(awsScriptDir);
            execBuff.append("completeUpload.sh  ");
            execBuff.append(" file://");
            execBuff.append(tmp.getAbsolutePath());

            log.debug("Multipart S3 Upload completed -- " + execBuff.toString());

            proc = Runtime.getRuntime().exec(execBuff.toString());
            // give it some time but not too much
            try {
                proc.waitFor();
            
                // timeout, kill the process
                debugProcessStdOutAndErr(proc);
                proc.destroy();
                proc = null;
            } catch (InterruptedException ie) {
                debugProcessStdOutAndErr(proc);
                proc.destroy();
            }
            if (file.getServerFile() != null && file.getServerFile().exists())
            {
                // should I throw an exception or overwrite it? (delete since the new file is external)
                // throw new FileUploadException("File already exists: " + file.getServerFile().getName());
            }


            //update the user uploads database
            JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, userContext);
            fileUtil.updateUploadsDb(HibernateUtil.instance(), file);

            JSONObject toReturn = new JSONObject();
            toReturn.append("success", true);
            // Return the status object
            return Response.ok().entity(toReturn.toString()).build();
        }
        catch (Throwable t) {
            log.error(t.getMessage(), t);
            t.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        } finally {
            // for debugging
            if (proc != null) {
                debugProcessStdOutAndErr(proc);
                proc.destroy();
            }

        }
    }

    private String readOutputFileToString(String outfilename) throws FileNotFoundException, IOException {
        // read the result from the temp file
        BufferedReader reader = new BufferedReader(new FileReader (outfilename));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");
        String resp;
        try {
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            resp =  stringBuilder.toString();
        } finally {
            reader.close();
            
            
        
        }
        return resp;
    }
    
    private void debugProcessStdOutAndErr(Process proc) {
        try {
            if (!log.isDebugEnabled()) return;
            
            BufferedReader stdInput = new BufferedReader(new 
                    InputStreamReader(proc.getInputStream()));
   
            BufferedReader stdError = new BufferedReader(new 
                    InputStreamReader(proc.getErrorStream()));
   
            // Read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
   
            // Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            
        } catch (Exception e){
            log.error(e);
        }
    }

    
    
    
}
