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
    
    public void downloadFile(GpContext userContext, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
           
        
        String bucket = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket", "gp-temp-test-bucket");
        String bucketRoot = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket.root", "tedslaptop");
        String profile = gpConfig.getGPProperty(userContext, "upload.aws.s3.profile", "");
        String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");
        String signingScript = gpConfig.getGPProperty(userContext, "download.aws.s3.presigning.script");
        
        String awsfilepath = gpConfig.getGPProperty(userContext,"aws-batch-script-dir");
        String awsfilename = gpConfig.getGPProperty(userContext, AWSBatchJobRunner.PROP_AWS_CLI, "aws-cli.sh");
         
        List<String> args=Arrays.asList(awsfilepath+awsfilename,  "s3", "presign",
                "s3://" + bucketRoot+file.getAbsolutePath());
        
        StringBuffer execBuff = new StringBuffer();
        // "/Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload '{\"input\": { \"name\":\""+path+"\", \"fileType\": \""+mimeType+"\"}}' response.json --profile genepattern";
        execBuff.append(awsfilepath+awsfilename);
        execBuff.append(" s3 presign \\\"s3://");
        execBuff.append(bucket);
        execBuff.append("/");
        execBuff.append(bucketRoot);
        execBuff.append(file.getAbsolutePath());
        execBuff.append("\\\"");
        
       
        System.out.println(execBuff.toString());
        String execArgs[];
        if (profile.length() > 0){
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign", bucket+ "/"+bucketRoot+file.getAbsolutePath(), "--profile", profile};
            
             System.out.println("A");
        } else {
            execArgs = new String[] {awsfilepath+awsfilename, "s3", "presign", bucket+ "/"+bucketRoot+file.getAbsolutePath()};
            
            System.out.println("B");
        }
        
        
        
        Process proc = Runtime.getRuntime().exec(execArgs);
        try {
            proc.waitFor();
        
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
    
           // Read the output from the command
           System.out.println("Here is the standard output of the command:\n");
           String s = null;
           String redirectUrl = null;
           while ((s = stdInput.readLine()) != null) {
               System.out.println(s);
               redirectUrl = s;
           }
        
           // Read any errors from the attempted command
           System.out.println("Here is the standard error of the command (if any):\n");
           while ((s = stdError.readLine()) != null) {
               System.out.println(s);
           }    
           resp.sendRedirect(redirectUrl);
           
        } catch (Exception e){
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    
    
    }
    
    
    
    
    
    
    // @Override
    public void downloadFile2(GpContext userContext, HttpServletRequest req, HttpServletResponse resp, File file) throws IOException {
        // For S3 file downloads, we want to generate a presigned URL to redirect to
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        
        String bucket = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket", "gp-temp-test-bucket");
        String bucketRoot = gpConfig.getGPProperty(userContext, "upload.aws.s3.bucket.root", "tedslaptop");
        String profile = gpConfig.getGPProperty(userContext, "upload.aws.s3.profile", "");
        String awsScriptDir = gpConfig.getGPProperty(userContext, "aws-batch-script-dir");
        String signingScript = gpConfig.getGPProperty(userContext, "download.aws.s3.presigning.script");

        
        StringBuffer execBuff = new StringBuffer();
        // "/Users/liefeld/AnacondaProjects/CondaInstall/anaconda3/bin/aws lambda invoke --function-name createPresignedPost --payload '{\"input\": { \"name\":\""+path+"\", \"fileType\": \""+mimeType+"\"}}' response.json --profile genepattern";
        execBuff.append(awsScriptDir);
        execBuff.append(signingScript);
        execBuff.append(" ");
        // need to include the path used for the real user dir
        
        execBuff.append(bucketRoot);
        execBuff.append(file.getAbsolutePath());
        execBuff.append(" ");
        execBuff.append(bucket);
        if (profile.length() > 0){
            execBuff.append(" ");
            execBuff.append(profile);
        }
        
        System.out.println(execBuff.toString());
        
        Process proc = Runtime.getRuntime().exec(execBuff.toString());
        try {
            proc.waitFor();
        
            BufferedReader stdInput = new BufferedReader(new     InputStreamReader(proc.getInputStream()));
            BufferedReader stdError = new BufferedReader(new  InputStreamReader(proc.getErrorStream()));
    
           // Read the output from the command
           System.out.println("Here is the standard output of the command:\n");
           String s = null;
           String redirectUrl = null;
           while ((s = stdInput.readLine()) != null) {
               System.out.println(s);
               redirectUrl = s;
           }
        
           // Read any errors from the attempted command
           System.out.println("Here is the standard error of the command (if any):\n");
           while ((s = stdError.readLine()) != null) {
               System.out.println(s);
           }    
           resp.sendRedirect(redirectUrl);
        } catch (Exception e){
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}
