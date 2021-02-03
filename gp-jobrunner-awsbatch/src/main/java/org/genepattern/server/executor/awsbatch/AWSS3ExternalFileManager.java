package org.genepattern.server.executor.awsbatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

import org.genepattern.server.dm.ExternalFileManager;
import org.json.JSONException;
import org.json.JSONObject;

public class AWSS3ExternalFileManager extends ExternalFileManager {
    private static Logger log = Logger.getLogger(AWSS3ExternalFileManager.class);
    
    private String getBucketName(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        if (aws_s3_root == null){
            return gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket", null);
        }
        // pull the bucket name out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(5,endIdx);
    }
    private String getBucketRoot(final GpConfig gpConfig, GpContext userContext) {
        String aws_s3_root = gpConfig.getGPProperty(userContext, "aws-s3-root");
        if (aws_s3_root == null){
            return gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket.root", null);
        }
        // pull the bucket root path out of something like "s3://moduleiotest/gp-dev-ami"
        int endIdx = aws_s3_root.indexOf("/", 5);
        return aws_s3_root.substring(endIdx+1);
    }
    
    
    public void downloadFile(GpContext userContext, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = getBucketName(gpConfig, userContext);
        String bucketRoot = getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[] = new String[] {awsfilepath+awsfilename, "s3", "presign", bucket+ "/"+bucketRoot+file.getAbsolutePath()};
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
               log.debug(s);
               redirectUrl = s;
           }  
           // Read any errors from the attempted command
           logStderr(proc, " download file");    
           
           resp.sendRedirect(redirectUrl);
           
        } catch (Exception e){
            log.debug(e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
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
    
    public boolean MoveFile(GpContext userContext, File fromFile, File toFile) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        String bucket = getBucketName(gpConfig, userContext);
        String bucketRoot = getBucketRoot(gpConfig, userContext);
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[]  = new String[] {awsfilepath+awsfilename, "s3", "mv", "s3://"+bucket+ "/"+bucketRoot+fromFile.getAbsolutePath(), "s3://"+bucket+ "/"+bucketRoot+toFile.getAbsolutePath()};
        
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
    
    
    
    
   

}
