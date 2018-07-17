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
 * Utility class for copying local files and directories into S3.
 * Files are uploaded with the 'aws s3 sync' command based on this template:
 * 
 * <pre>
 *   aws s3 sync <LocalPath> <S3Prefix><LocalPath>
 *       [--exclude exclude-pattern]*
 *       [--include include-pattern]*
 *       [aws-profile] 
 * </pre>
 * 
 * See the <a 
 *   href="https://docs.aws.amazon.com/cli/latest/reference/s3/index.html"
 * >aws s3 reference</a> for more details about the <a 
 *   href="https://docs.aws.amazon.com/cli/latest/reference/s3/index.html#path-argument-type"
 * >path-argument-type</a>.
 * 
 * <h3>Example Jupyter Notebook</h3>
 * For more details open the <b>awscli_examples.ipynb</b> notebook which is saved in this project.
 * <ul>
 *   <li>./gp-jobrunner-awsbatch/awscli_examples.ipynb
 * </ul>
 * I use this notebook to test, develop, and document aws cli commands. If you have a
 * jupyter notebook app running locally, open this from your notebook dashboard.
 * You can also view a static (read only) html rendering in GitHub.
 * <ul>
 * <li><a href="https://github.com/genepattern/genepattern-server/blob/develop/gp-jobrunner-awsbatch/awscli_examples.ipynb"
 *      >gp-jobrunner-awsbatch/awscli_examples.ipynb (latest published version)</a> 
 * </ul>
 * 
 * <h3>Links</h3>
 * <ul>
 *   <li><a href="https://docs.aws.amazon.com/cli/latest/userguide/cli-environment.html">aws cli user guide</a>, for an overview
 *   <li><a href="https://docs.aws.amazon.com/cli/latest/reference/s3/index.html">aws s3 reference</a>
 *   <li><a href="https://docs.aws.amazon.com/cli/latest/reference/s3/sync.html">aws s3 sync command reference</a>
 *   <li><a href="http://jupyter.org">jupyter.org</a>, to learn about the Jupyter Notebook
 * </ul>
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
        if (b.cmdEnv==null) {
            this.cmdEnv=Collections.emptyMap();
        }
        else {
            this.cmdEnv=Collections.unmodifiableMap(b.cmdEnv);
        }
        this.s3_bucket=b.s3_bucket;
        if (Strings.isNullOrEmpty(this.s3_bucket)) {
            log.warn("s3_bucket is not set");
        }
        final String dirPath=cmdEnv.get("GP_JOB_METADATA_DIR");
        if (Strings.isNullOrEmpty(dirPath)) {
            log.warn("'GP_JOB_METADATA_DIR' not set, setting metadataDir=null");
            this.metadataDir=null;
        }
        else {
            this.metadataDir=new File(dirPath);
        }
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
    
    /**
     * get the 'aws s3 sync' command line args to copy the file from 
     * a source s3 bucket into a destination path on the local file system
     * 
     * Template
     *   aws s3 sync {s3_prefix}{src_path} {dest_prefix}{dest_path} --exclude "*" --include "{filename}"
     * Example 1: (default) dest_prefix not set
     *   aws s3 sync s3://gpbeta/temp /temp --exclude "*" --include "test.txt"
     * Example 2: dest_prefix=/local
     *   aws s3 sync s3://gpbeta/temp /local/temp --exclude "*" --include "test.txt"
     * 
     * @param localFile
     * @param destPrefix
     * @return
     */
    protected List<String> getSyncFromS3Args(final File localFile, final String destPrefix) {
        List<String> args=new ArrayList<String>();
        args.add("s3");
        args.add("sync");
        if (localFile.isFile()) {
            // from s3Uri
            args.add(s3_bucket+""+localFile.getParent());
            // to container local path
            args.add(Strings.nullToEmpty(destPrefix)+""+localFile.getParent());
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
            args.add(Strings.nullToEmpty(destPrefix)+""+localFile.getPath());
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
