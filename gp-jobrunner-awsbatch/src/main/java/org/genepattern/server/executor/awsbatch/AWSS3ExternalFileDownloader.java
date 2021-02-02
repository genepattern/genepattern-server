package org.genepattern.server.executor.awsbatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.dm.ExternalFileDownloader;
import org.json.JSONException;
import org.json.JSONObject;

public class AWSS3ExternalFileDownloader extends ExternalFileDownloader {
    private static Logger log = Logger.getLogger(AWSS3ExternalFileDownloader.class);
    
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
    
    
    public void downloadFile(GpContext userContext, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
           
        
        // backstop with the old params for a release
        String bucket = getBucketName(gpConfig, userContext);
        if (bucket == null) bucket = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket", null);
        String bucketRoot = getBucketRoot(gpConfig, userContext);
        if (bucketRoot == null) bucketRoot = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket.root", null);

        String profile = gpConfig.getGPProperty(userContext, "upload.aws.s3.profile", "");
        
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        String execArgs[];
        if (profile.length() > 0){
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign", bucket+ "/"+bucketRoot+file.getAbsolutePath(), "--profile", profile};
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign", bucket+ "/"+bucketRoot+file.getAbsolutePath()};
        }
        
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor();
        
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
    
           // Read the output from the command
           log.debug("Here is the standard output of the command:\n");
           String s = null;
           String redirectUrl = null;
           while ((s = stdInput.readLine()) != null) {
               log.debug(s);
               redirectUrl = s;
           }
        
           // Read any errors from the attempted command
           System.out.println("Here is the standard error of the command (if any):\n");
           while ((s = stdError.readLine()) != null) {
               log.debug(s);
           }    
           resp.sendRedirect(redirectUrl);
           
        } catch (Exception e){
            log.debug(e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    
    
    }
    
    
    
    
    
    
   

}
