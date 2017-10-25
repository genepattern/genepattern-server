package org.genepattern.server.executor.awsbatch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;

import com.google.common.base.Strings;

/**
 * Utility class for copying local files into S3.
 * Example command,
 *   aws s3 sync `pwd` s3://gpbeta`pwd` --exclude "*" --include "test.txt" --profile genepattern
 *   
 * See:
 *   http://docs.aws.amazon.com/cli/latest/userguide/cli-environment.html
 * 
 * @author pcarr
 */
public class AwsS3Cmd {
    private static final Logger log = Logger.getLogger(AwsS3Cmd.class);

    protected static int runCmd(final Map<String,String> cmdEnv, final String cmd, final List<String> args) {
        final CommandLine cl= new CommandLine(cmd);
        for(final String arg : args) {
            cl.addArgument(arg, AWSBatchJobRunner.handleQuoting);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DefaultExecutor exec=new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(outputStream));
        try {
            int exitCode=exec.execute(cl, cmdEnv);
            return exitCode;
        }
        catch (ExecuteException e) {
            log.error(e);
        }
        catch (IOException e) {
            log.error(e);
        }
        return -1;
    }

    /** optional, '--profile' '<awsProfile>' arg */
    final String profile;
    /** optional environment variables */
    final Map<String,String> cmdEnv;
    /** the target s3 bucket, e.g. 's3://my_bucket' */
    final String s3_bucket;
    /** the path to the 'aws' command or wrapper, */
    final String awsCmd;
    /** the local path to the metadata dir */
    final File metadataDir;

    private AwsS3Cmd(final Builder b) {
        this.awsCmd=b.awsCmd;
        this.profile=b.profile;
        this.cmdEnv=Collections.unmodifiableMap(b.cmdEnv);
        this.s3_bucket=b.s3_bucket;
        this.metadataDir=new File(cmdEnv.get("GP_METADATA_DIR"));
    }

    protected String toS3Uri(final File filePath) {
        // TODO: test with special characters
        // TODO: handle fully qualified AND relative paths
        return s3_bucket + filePath;
    }
    
    protected File fromS3Uri(final String s3Uri) {
        final String localPath;
        if (s3Uri.startsWith(s3_bucket)) {
            localPath=s3Uri.substring(s3_bucket.length());
        }
        else {
            log.error("invalid s3Uri="+s3Uri);
            localPath=s3Uri;
        }
        // TODO: test with special characters
        return new File(localPath);
    }
    
    public void syncToS3(final File localFile) {
        if (localFile==null) {
            log.error("ignoring null arg");
            return;
        }
        if (localFile.isFile()) {
            copyFileToS3(localFile);
        }
        else if (localFile.isDirectory()) {
            copyDirToS3(localFile);
        }
        else {
            log.error("localFile must be a file or a directory, localFile="+localFile);
            return;
        }
    }
    
    /**
     * Copy the local file to the s3 bucket. 
     * Template:
     *   aws s3 sync <localFile>.parentDir <s3_bucket>/<localFile>.parentDir --exclude "*" --include "<localFile>.name" --profile genepattern
     * Example:
     *   aws s3 sync `pwd` s3://gpbeta`pwd` --exclude "*" --include "test.txt" --profile genepattern
     *   
     * @param localFile
     */
    private void copyFileToS3(final File localFile) {
        // aws s3 sync fq-local-path s3uri
        if (!localFile.isFile()) {
            log.error("Expecting a file not a directory, inputFile="+localFile);
            return;
        }
        if (!localFile.isAbsolute()) {
            log.error("Expecting a fully qualified file, inputFile="+localFile);
            return;
        }
        List<String> args=Arrays.asList( "s3", "sync",
                // from local path
                localFile.getParent(), 
                // to s3Uri
                s3_bucket+""+localFile.getParent(), 
                "--exclude", "*", "--include", localFile.getName() 
        );
        if (!Strings.isNullOrEmpty(profile)) {
            args.add("--profile");
            args.add(profile);
        }

        runCmd(cmdEnv, awsCmd, args);
    }
    
    private void copyDirToS3(final File localDir) {
        if (!localDir.isDirectory()) {
            log.error("Expecting a directory, localDir="+localDir);
            return;
        }
        if (!localDir.isAbsolute()) {
            log.error("Expecting a fully qualified path, localDir="+localDir);
            return;
        }
        List<String> args=Arrays.asList("s3", "sync",
            //// for debugging ...
            // "--dryrun",
            //// optional delete mode
            ////   --delete (boolean) Files that exist in the destination but not in the source are deleted during sync.
            // "--delete", 
            // from local path
            localDir.getPath(), 
            // to s3Uri
            s3_bucket+""+localDir.getPath(),
            // hard-coded default-excludes 
            "--exclude", "*~", 
            "--exclude", ".DS_Store",
            "--exclude", ".git*"
        );
        if (!Strings.isNullOrEmpty(profile)) {
            args.add("--profile");
            args.add(profile);
        }

        runCmd(cmdEnv, awsCmd, args); 
    }
    
    protected List<String> getSyncFromS3Args(final File localFile) {
        List<String> args=new ArrayList<String>();
        args.add("s3");
        args.add("sync");
        if (localFile.isFile()) {
            // from s3Uri
            args.add(s3_bucket+""+localFile.getParent());
            // to container local path
            args.add(localFile.getParent());
            // filter all but the file
            args.add("--exclude");
            args.add("*");
            args.add("--include");
            args.add(localFile.getName());
        }
        else if (localFile.isDirectory()) {
            // from s3Uri
            args.add(s3_bucket+""+localFile.getPath());
            // to container local path
            args.add(localFile.getPath());
        }
        if (!Strings.isNullOrEmpty(profile)) {
            args.add("--profile");
            args.add(profile);
        }
        return args;
    }

    public File getMetadataDir() {
        return metadataDir;
    }

    public static class Builder {
        String awsCmd="aws";
        String profile=null;
        Map<String,String> cmdEnv=null;
        String s3_bucket=null;
        String localPath=null;
        
        public Builder awsCmd(final String awsCmd) {
            this.awsCmd=awsCmd;
            return this;
        }

        public Builder withProfile(final String profile) {
            this.profile=profile;
            return this;
        }
        
        public Builder awsCliEnv(final Map<String,String> awsCliEnv) {
            this.cmdEnv=Collections.unmodifiableMap(awsCliEnv);
            return this;
        }
        
        public Builder withEnv(final String key, final String value) {
            if (cmdEnv==null) {
                cmdEnv=new HashMap<String,String>();
            }
            cmdEnv.put(key,value);
            return this;
        }
        
        public Builder from(final File file) {
            this.localPath=file.getPath();
            return this;
        }
        
        public Builder s3_bucket(final String s3_bucket) {
            this.s3_bucket=s3_bucket;
            return this;
        }
        
        public AwsS3Cmd build() {
            return new AwsS3Cmd(this);
        }
    }

}
