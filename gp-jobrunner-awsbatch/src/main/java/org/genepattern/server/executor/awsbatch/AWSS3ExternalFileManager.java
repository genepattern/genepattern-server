package org.genepattern.server.executor.awsbatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.log4j.Logger; 
import org.genepattern.server.DataManager;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

import org.genepattern.server.dm.ExternalFileManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;



public class AWSS3ExternalFileManager extends ExternalFileManager {
    private static Logger log = Logger.getLogger(AWSS3ExternalFileManager.class);
    private static String placeholderPrefix = ".hiddenS3placeholder";
    
    public boolean isUsableURI(String value){
        if (value.startsWith("s3://") || value.startsWith("S3://")){
            try {
                new URI(value);
                return true;
            } catch (URISyntaxException ue){
                return false;
            }
            
        }
        return false;
        
    }
    
    
    public void downloadFile(GpContext userContext, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException {
        try {
           String redirectUrl = getDownloadURL(userContext, file);
           resp.sendRedirect(redirectUrl+"&genepatternUrl="+URLEncoder.encode(req.getRequestURL().toString()));
           
        } catch (Exception e){
            e.printStackTrace();
            log.debug(e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    public String getDownloadURL(GpContext userContext,  File file) throws Exception {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsS3CustomUrl = gpConfig.getGPProperty(userContext,"aws-s3-custom-url", null); // e.g. "https://betafiles.genepattern.org/"
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
        
        // this line for when custom domain not used for the url to s3
        String thePath = bucket+ "/"+bucketRoot+file.getAbsolutePath();
        String execArgs[];
        
        if (awsS3CustomUrl  != null) {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign",thePath, "--endpoint-url", awsS3CustomUrl};
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign",thePath};
        }
        
        // these 2 for custom domain testing for GSEA
        //String 
        
       
        BufferedReader stdInput = null;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
            stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
           
           // Read the output from the command
           log.debug("Here is the standard output of the command:\n");
           String s = null;
           String redirectUrl = null;
           while ((s = stdInput.readLine()) != null) {
               System.out.println("==== REDIRECT URL result IS :" + s);
               log.debug(s);
               redirectUrl = s;
           }  
           redirectUrl = redirectUrl.replace("betafiles.genepattern.org/betafiles.genepattern.org/", "betafiles.genepattern.org/");
           // Read any errors from the attempted command
           logStderr(proc, " download file");    
           
           return redirectUrl;
           
        } catch (Exception e){
            log.debug(e);
            throw(e);
        } finally {
            proc.destroy();
            if (stdInput != null) stdInput.close();
        }
    
    
    }
    
    
    private void logStderr(Process proc, String action) throws IOException {
        String s;
        BufferedReader stdError = null;
        try {
            stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
            log.debug("Here is the standard error of "+action+" from S3 (if any):\n");
            while ((s = stdError.readLine()) != null) {
                log.debug(s);
            }
        } finally {
            if (stdError != null) stdError.close();
        }
    }
    private void logStdout(Process proc, String action) throws IOException {
        String s;
        BufferedReader stdOut = null;
        try {
            stdOut = new BufferedReader(new  InputStreamReader(proc.getInputStream()));
            log.debug("Here is the standard out of "+action+" from S3 (if any):\n");
            while ((s = stdOut.readLine()) != null) {
                log.debug(s);
            }
        } finally {
            if (stdOut != null) stdOut.close();
        }
    }
    
    public boolean moveFile(GpContext userContext, File fromFile, File toFile) throws IOException {
        return moveFile(userContext, fromFile, toFile, false);
    }
    public boolean moveDirectory(GpContext userContext, File fromFile, File toFile) throws IOException {
        return moveFile(userContext, fromFile, toFile, true);
    }
    
    public boolean moveFile(GpContext userContext, File fromFile, File toFile, Boolean recursive) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[];
        if (recursive){
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "mv", "s3://"+bucket+ "/"+bucketRoot+fromFile.getAbsolutePath(), "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()+"/", "--recursive"};
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "mv", "s3://"+bucket+ "/"+bucketRoot+fromFile.getAbsolutePath(), "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};
              
        }
        
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
        
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "move file"); 
                logStderr(proc, "move file"); 
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            proc.destroy();
        }
        return success;
    }
    
    public boolean copyFile(GpContext userContext, File fromFile, File toFile) throws IOException {
        return copyFile(userContext, fromFile, toFile, false);
    }
    
    public boolean copyDirectory(GpContext userContext, File fromFile, File toFile) throws IOException {
        return copyFile(userContext, fromFile, toFile, true);
    }
    public boolean copyFile(GpContext userContext, File fromFile, File toFile, Boolean recursive) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[];
        if (recursive) {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "cp", "s3://"+bucket+ "/"+bucketRoot+fromFile.getAbsolutePath(), "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath(), "--recursive"};
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "cp", "s3://"+bucket+ "/"+bucketRoot+fromFile.getAbsolutePath(), "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};             
        }
        
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
        
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "copy s3 file"); 
                logStderr(proc, "copy s3 file"); 
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            proc.destroy();
        }
        return success;
    
    }
    
    public  boolean deleteFile(GpContext userContext,  File file) throws IOException {
        return deleteFile(userContext, file, false);
    }
    
    public  boolean deleteDirectory(GpContext userContext,  File file) throws IOException {
        return deleteFile(userContext, file, true);  
     }

    public  boolean deleteFile(GpContext userContext,  File file, Boolean recursive) throws IOException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[];
        
        if (recursive){
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "rm", "s3://"+bucket+ "/"+bucketRoot+file.getAbsolutePath(), "--recursive"};
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "rm", "s3://"+bucket+ "/"+bucketRoot+file.getAbsolutePath()};   
        }
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
        
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "delete S3 file"); 
                logStderr(proc, "delete S3 file"); 
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            proc.destroy();
        }
        return success;
        
    }
    
    /**
     * The suspenders in a belt-and-suspenders strategy for external sub directories.  GenePattern has many cases where it looks at 
     * the local file system and we may not have caught them all, so when we create a subdirectory on the external S3 service, also create
     * a local empty directory at the same location.  We ignore errors since this is just a backstop for the main changes to look only at 
     * the remote file system.
     * 
     * @param subdir
     */
    protected void createLocalSubdirectory(File subdir){
        String[] execArgs = new String[] {"mkdir", "-p", subdir.getAbsolutePath()};
        
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(execArgs);
            proc.waitFor(10, TimeUnit.SECONDS);
        } catch (Exception e){
            log.debug(e);
        } finally {
            if (proc != null) proc.destroy();
        }
    }
    
    /**
     * Since directories (and sub directories) don't exist in S3 we will fake it out by uploading a 0-length file
     * to hold the space.  By making it start with a "." GenePattern will igore it as a hidden file
     */
    public boolean createSubDirectory(GpContext userContext,  File subdir) throws IOException{
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        File dummyFile = File.createTempFile(placeholderPrefix, "");
        
        String thePath = subdir.getAbsolutePath();
        
        String[] execArgs = new String[] {awsfilepath+awsfilename, "s3", "cp", dummyFile.getAbsolutePath(),"s3://"+bucket+ "/"+bucketRoot+thePath+"/"+dummyFile.getName()};
        
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
        
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "create S3 subdir"); 
                logStderr(proc, "create S3 subdir"); 
            } else {
                createLocalSubdirectory(subdir);
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            dummyFile.delete();
            proc.destroy();
        }
        return success; 
    }
    
    
    
    /**
     * return an array of File objects (which will not exist locally) under the given directory based on
     * the contents of the S3 directory
     */
    public  ArrayList<GpFilePath> listFiles(GpContext userContext,  File rootDir) throws IOException {
        ArrayList<GpFilePath> foundFiles = new ArrayList<GpFilePath>();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String[] execArgs = new String[] {awsfilepath+awsfilename, "s3", "ls", "s3://"+bucket+ "/"+bucketRoot+rootDir.getAbsolutePath(), "--recursive"};
        
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            logStderr(proc, "listfiles s3 directory");
            // Read the output from the command
            String s = null;

            // each line will look like  "yyyy-MM-dd HH:mm:ss"
            //         2020-12-01 13:19:01    4931101 tedslaptop/Users/liefeld/gp/users/739701.jpg
            HashMap<String, GpFilePath> parentsAdded = new HashMap<String, GpFilePath>();
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                String[] lineParts = s.split("\\s+");
                String name = lineParts[3];
                
                Long length = new Long(lineParts[2]);
                Date lastModified = parseDateFormat(lineParts[0]+" "+lineParts[1]);
                
                // strip out the bucketroot which is not wanted for the local file
                String outFilePath = name.substring(bucketRoot.length());
                File f = new File(outFilePath);
                
                
                GpFilePath gpfile = getUploadFile(userContext, rootDir, f);
                
                if (! f.getParentFile().equals(rootDir)) {
                    File parent = f.getParentFile();
                    // need to add the parent dir as well, but only do it once
                    while (!(parent.equals(rootDir)) && (parent.getAbsolutePath().startsWith(rootDir.getAbsolutePath()))) {
                        if (parentsAdded.get(parent.getAbsolutePath()) == null) {  //check if we added it already
                            GpFilePath gpfileParent = getUploadFile(userContext, rootDir, parent);
                            gpfileParent.setKind("directory");
                            gpfileParent.setLastModified(lastModified);
                            foundFiles.add(gpfileParent);
                            parentsAdded.put(parent.getAbsolutePath(), gpfileParent);
                        }
                        parent = parent.getParentFile();
                        
                    }
                } 
                
                // we have placeholder files we need to preserve "empty" directories in S3
                // leave them off the list (but we keep any parent directories)
                
                if (!f.getName().startsWith(placeholderPrefix)) {
                    gpfile.setFileLength(length);
                    gpfile.setLastModified(lastModified);
                    foundFiles.add(gpfile);
                }
            }
       } catch (Exception e){
            log.debug(e);
            proc.destroyForcibly();
            proc = null;
            return new ArrayList<GpFilePath>();
            
        } finally {
            proc.destroy();
        }
        
        
        return foundFiles;
    }
    
    private GpFilePath getUploadFile( final GpContext userContext, final File userUploadDir, final File uploadFile) throws Exception {
        try {
            //special-case, block 'tmp'
            //GpFilePath uploadFilePath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, uploadPath, (LSID)null);
            File userRootDir = ServerConfigurationFactory.instance().getUserUploadDir(userContext);
            
            
            File rel = FileUtil.relativizePath(userRootDir, uploadFile);
            
            GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(userContext, userUploadDir, rel);
            if (DataManager.isTmpDir(uploadFilePath)) {
                throw new  Exception("Can't save file with reserved filename: " + rel.getPath());
            }

            return uploadFilePath;
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new  Exception("Error initializing upload file reference for '" + uploadFile.getPath() + "': "+e.getLocalizedMessage());
        }
    }
    
    public  boolean syncRemoteFileToLocal(GpContext userContext,  File file) throws IOException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[] = new String[] {awsfilepath+awsfilename, "s3", "sync", 
                "s3://"+bucket+ "/"+bucketRoot+file.getParentFile().getAbsolutePath(), 
                file.getParentFile().getAbsolutePath(),
                "--exclude", "*",
                "--include", file.getName()};
        
       
        boolean success = false;
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor(3, TimeUnit.MINUTES);
        
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "sync S3 file"); 
                logStderr(proc, "sync S3 file"); 
            }
            
        } catch (Exception e){
            log.debug(e);
            return false;
            
        } finally {
            proc.destroy();
        }
        return success;
    }
  
    
    public  boolean syncLocalFileToRemote(GpContext userContext,  File file, boolean deleteLocalAfterSync) throws IOException {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = AwsBatchUtil.getBucketName(gpConfig, userContext);
        String bucketRoot = AwsBatchUtil.getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[] = new String[] {awsfilepath+awsfilename, "s3", "sync", 
                file.getParentFile().getAbsolutePath(),
                "s3://"+bucket+ "/"+bucketRoot+file.getParentFile().getAbsolutePath(), 
                "--exclude", "*",
                "--include", file.getName()};

        boolean success = false;
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(execArgs);
            proc.waitFor(3, TimeUnit.MINUTES);
            success = (proc.exitValue() == 0);
            if (!success){
                logStdout(proc, "sync S3 file"); 
                logStderr(proc, "sync S3 file"); 
            } else {
                try {
                    if (deleteLocalAfterSync){
                        boolean deleted = file.delete();
                        if (!deleted) file.deleteOnExit();
                    }
                } catch (Exception e){
                    file.deleteOnExit();
                }
            }

        } catch (Exception e){
            log.debug(e);


        } finally {
            if (proc != null) proc.destroy();
        }


        return true;
    }
    
    
    
    
   
    final static DateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    final private Date parseDateFormat(final String dateSpec) throws ParseException {
        
        return df.parse(dateSpec);
    }

}
