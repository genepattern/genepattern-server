package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Strings;

/**
 * Utility methods initially implemented in the AWS Batch integration.
 * Under consideration for the core GenePattern Server code.
 * 
 * @author pcarr
 */
public class AwsBatchUtil {
    private static final Logger log = Logger.getLogger(AwsBatchUtil.class);

    /**
     * Round up to the nearest mebibyte (MiB).
     * Example usage:
     * <pre>
     *   String mib=""+numMiB(m);
     * </pre>
     * 
     * @param m the memory instance
     * @return the amount of memory in mebibytes
     */
    public static long numMiB(final Memory m) {
        long mib = (long) Math.ceil(
            (double) m.getNumBytes() / (double) Memory.Unit.m.getMultiplier()
        );
        return mib;
    }

    public static final Value getValue(final GpConfig gpConfig, final GpContext jobContext, final String key, final Value defaultValue) {
        return gpConfig.getValue(jobContext, key, defaultValue);
    }

    public static final String getProperty(final DrmJobSubmission gpJob, final String key, final String defaultValue) {
        return gpJob.getGpConfig().getGPProperty(gpJob.getJobContext(), key, defaultValue);
    }


    /** helper method because String#replaceAll expects a regex */
    protected static String replaceAll_quoted(final String str, final String literal, final String replacement) {
        if (Strings.isNullOrEmpty(str)) {
            return str;
        }
        return str.replaceAll(
            Pattern.quote(literal),
            Matcher.quoteReplacement(replacement)
        );
    }

    /** helper method because String#replaceFirst expects a regex */
    protected static String replaceFirst_quoted(final String str, final String literal, final String replacement) {
        if (Strings.isNullOrEmpty(str)) {
            return str;
        }
        return str.replaceFirst(
                Pattern.quote(literal),
                Matcher.quoteReplacement(replacement));
    }

    private static void makeSymLink(final File linkDir, final File target, final String linkName) throws CommandExecutorException {
        makeSymLink(linkDir, target.toPath(), linkName);
    }

    /**
     * Make symbolic link to the target file in the given directory.
     *     ln -s <targetFile> <targetFile.name>
     * @param linkDir
     * @param target
     * @param linkName
     * @throws CommandExecutorException
     */
    private static void makeSymLink(final File linkDir, final Path target, final String linkName) throws CommandExecutorException {
        try {
            Files.createSymbolicLink(
                // link
                linkDir.toPath().resolve(linkName), 
                // target
                target
            );
        }
        catch (Throwable t) {
            final String message="Error creating symlink to local input file='"+target+"' in directory='"+linkDir+"'";
            log.error(message, t);
            throw new CommandExecutorException(message, t);
        }
    }

    /**
     * Make symlinks 
     * @param inputDir - the local input directory to be sync'ed into aws s3
     * @param inputFiles - the list of job input files in the GP server local file system
     * @return 
     * @throws CommandExecutorException
     */
    private static Map<String, String> makeSymLinks(final File inputDir, final Set<File> inputFiles) throws CommandExecutorException {
        final Map<String,String> inputFileMap = new HashMap<String,String>();
        for (final File inputFile : inputFiles) {
            final File linkedFile = new File(inputDir, inputFile.getName());
            AwsBatchUtil.makeSymLink(inputDir, inputFile, inputFile.getName());
            inputFileMap.put(inputFile.getAbsolutePath(), linkedFile.getAbsolutePath());
        }
        return inputFileMap;
    }

    protected static GpContext initJobContext(final DrmJobRecord jobRecord) {
        JobInfo jobInfo = null;
        if (jobRecord!=null && jobRecord.getGpJobNo() != null) {
            try {
                jobInfo = new AnalysisDAO().getJobInfo(jobRecord.getGpJobNo());
            }
            catch (Throwable t) {
                log.debug("Error initializing jobInfo from db, jobNumber="+jobRecord.getGpJobNo(), t);
            }
        }
        final GpContext jobContext=new GpContext.Builder()
            .jobNumber(jobRecord.getGpJobNo())
            .jobInfo(jobInfo)
        .build();
        return jobContext;
    }

    protected static GpContext initJobContext(final DrmJobSubmission jobSubmission) {
        JobInfo jobInfo = null;
        if (jobSubmission!=null && jobSubmission.getGpJobNo() != null) {
            try {
                jobInfo = new AnalysisDAO().getJobInfo(jobSubmission.getGpJobNo());
            }
            catch (Throwable t) {
                log.debug("Error initializing jobInfo from db, jobNumber="+jobSubmission.getGpJobNo(), t);
            }
        }
        final GpContext jobContext=new GpContext.Builder()
            .jobNumber(jobSubmission.getGpJobNo())
            .jobInfo(jobInfo)
        .build();
        return jobContext;
    }

    /**
     * isUseS3NonLocalFiles is used to determine if we may be running analyses on files that are in S3 but not on the
     * local disk of the GP head node. This can be because they
     * were directly uploaded there, or for jobResults left there but not copied locally.
     * 
     * @param jobSubmission
     * @return
     */
    protected static boolean isUseS3NonLocalFiles (final DrmJobSubmission jobSubmission) {
        final GpContext jobContext=AwsBatchUtil.initJobContext(jobSubmission);
        GpConfig jobConfig = ServerConfigurationFactory.instance();
        
        final boolean directExternalUploadEnabled = (jobConfig.getGPIntegerProperty(jobContext, "direct_external_upload_trigger_size", -1) >= 0);
        final boolean directDownloadEnabled = (jobConfig.getGPProperty(jobContext, "download.aws.s3.downloader.class", null) != null);
        
        return (directDownloadEnabled || directExternalUploadEnabled);
        
    }
    
    /**
     * Get the list of input and support files to be copied into the docker container
     * before running the job
     * 
     * Note: this returns an unmodifiable set
     */
    protected static Set<File> getInputFiles(final DrmJobSubmission gpJob) {
        final Set<File> inputFiles = new LinkedHashSet<File>();
        final Set<File> jobInputFiles=getJobInputFiles(gpJob);
        inputFiles.addAll(jobInputFiles);
        
        // sync the entire taskLib directory
        final File taskLibDir=gpJob.getTaskLibDir().getAbsoluteFile();
        if (taskLibDir != null && taskLibDir.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+gpJob.getGpJobNo()+", sync taskLib directory: "+taskLibDir);
            }
            inputFiles.add(taskLibDir);
        }
        
        // special-case, sync the entire wrapper-scripts directory
        final File wrapperScripts=getGPFileProperty(gpJob, GpConfig.PROP_WRAPPER_SCRIPTS_DIR);
        if (wrapperScripts != null && wrapperScripts.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+gpJob.getGpJobNo()+", sync wrapper_scripts directory: "+wrapperScripts);
            }
            inputFiles.add(wrapperScripts);
        }
        
        // special-case, sync required patches
        // TODO: implement getPatchInfoForJob, instead of manual edit of config file
        final Value requiredPatches=gpJob.getValue("required-patches");
        if (requiredPatches!=null && requiredPatches.getNumValues() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+gpJob.getGpJobNo()+", adding required-patches ...");
            }
            for(final String requiredPatch : requiredPatches.getValues()) {
                // assume relative paths are relative to the <patches> directory
                final File requiredFile = getPatchFile(gpJob.getGpConfig(), gpJob.getJobContext(), requiredPatch);
                if (requiredFile==null) {
                    log.error("gpJobNo="+gpJob.getGpJobNo()+", patchFile is null, requiredPatch="+requiredPatch);
                }
                else if (!requiredFile.canRead()) {
                    log.error("gpJobNo="+gpJob.getGpJobNo()+", can't read file, requiredPatch="+requiredPatch);
                }
                else {
                    if (log.isDebugEnabled()) {
                        log.debug("     "+requiredFile);
                    }
                    inputFiles.add(requiredFile);                    
                }
            }
        }

//        // special-case, include <run-with-env> support files
//        final Set<File> additionalSupportFiles=getWrapperScripts(gpJob);
//        inputFiles.addAll(additionalSupportFiles);
//        // special-case, include selected <aws-batch-scripts>
//        inputFiles.addAll(getAwsBatchWrapperScripts(gpJob));
        return Collections.unmodifiableSet(inputFiles);
    }
    
    protected static Set<File> getInputFileParentDirectories(final DrmJobSubmission gpJob) { 
        final Set<File> inputFileParents = new LinkedHashSet<File>();
        Set<File> inputFiles = getInputFiles(gpJob);
        for (File infile: inputFiles){
            File parent = infile.getParentFile().getAbsoluteFile();
            if (parent.isDirectory() && parent.exists()){
                inputFileParents.add(parent);
            }
        }
        return Collections.unmodifiableSet(inputFileParents);
    }
    
    
    
    

    protected static Set<File> getJobInputFiles(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+gpJob.getGpJobNo()+", listing local file paths ...");
        }

        boolean filesMayBeInS3AndNotExistLocally = AwsBatchUtil.isUseS3NonLocalFiles(gpJob);
        
        // linked hash set preserves insertion order
        final Set<File> jobInputFiles = new LinkedHashSet<File>();
        for(final String localFilePath : gpJob.getJobContext().getLocalFilePaths()) {
            if (Strings.isNullOrEmpty(localFilePath)) {
                // skip empty path
            }
            else {
                final File file=new File(localFilePath);
                if (file != null && (file.exists() || filesMayBeInS3AndNotExistLocally)) {
                    jobInputFiles.add(file);
                }
                else {
                    // if the file is missing its normally an error.  However we do make an exception 
                    // if we are allowing direct S3 uploads.  In that case we need to make sure the 
                    // file is in the appropriate S3 directory even if not on the local disk
                    String directUploadBucket = AwsBatchUtil.getProperty(gpJob, "upload.aws.s3.bucket", null);
                    if (directUploadBucket != null){
                        boolean isPresentInS3 = s3FileExists(gpJob, localFilePath);
                        if (isPresentInS3){
                            jobInputFiles.add(file);
                        }
                    }
                    
                    
                    log.error("gpJobNo="+gpJob.getGpJobNo()+", file doesn't exist for localFilePath='"+localFilePath+"'");
                }
            }
        }
        return Collections.unmodifiableSet(jobInputFiles);
    }
    
    public static boolean s3FileExists(final DrmJobSubmission gpJob, final String localFilePath){
        // JTL XXX need to implement checking into S3 for the actual presence
        String directUploadBucket = AwsBatchUtil.getProperty(gpJob, "upload.aws.s3.bucket", null);
        String bucketRoot = AwsBatchUtil.getProperty(gpJob, "upload.aws.s3.bucket.root", null);
        String awsProfile = AwsBatchUtil.getProperty(gpJob, "upload.aws.s3.profile", null);
        
        
        return true;
    }
    
    

    /**
     * Get the File referenced by the configuration property, e.g.
     *   <wrapper-scripts>
     * Usage:
     *   File wrapperScripts=getDir(gpJob, "wrapper-scripts");
     * @return null if the property is not set
     */
    protected static File getGPFileProperty(final DrmJobSubmission gpJob, final String KEY) {
        return gpJob.getGpConfig().getGPFileProperty(gpJob.getJobContext(), KEY);
    }

    /**
     * Get the set of files relative to a configured file path, e.g. <wrapper-scripts>
     * Usage:
     *   getFileSet(gpJob, "wrapper-scripts", "env-custom.sh", "env-default.sh")
     * @param gpJob
     * @param KEY
     * @param includePaths
     * @return a set of files
     */
    protected static Set<File> getFileSet(final DrmJobSubmission gpJob, final String KEY, String... includePaths) {
        final File parentDir=getGPFileProperty(gpJob, KEY);
        return getFileSet(gpJob, KEY, parentDir, includePaths);
    }

    protected static Set<File> getFileSet(final DrmJobSubmission gpJob, final String KEY, final File parentDir, String... includePaths) {
        if (parentDir==null) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+gpJob.getGpJobNo()+", skipping <"+KEY+"> files, property not set");
            }
            return Collections.emptySet();
        } 
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+gpJob.getGpJobNo()+", including <"+KEY+"> files ...");
        }
        final Set<File> fileSet = new LinkedHashSet<File>();
        final Path parentPath=parentDir.toPath();
        for(final String path : includePaths) {
            if (Strings.isNullOrEmpty(path)) {
                // skip empty path
            }
            else {
                final File file=parentPath.resolve(path).toFile();
                if (file != null && file.exists()) {
                    if (log.isDebugEnabled()) {
                        log.debug("    <"+KEY+">/"+path);
                    }
                    fileSet.add(file);
                }
            }
        }
        return fileSet;
    }

    protected static Set<File> getWrapperScriptsDir(final DrmJobSubmission gpJob) {
        final File wrapperScripts=getGPFileProperty(gpJob, GpConfig.PROP_WRAPPER_SCRIPTS_DIR);
        final Set<File> fileSet = new LinkedHashSet<File>();
        fileSet.add(wrapperScripts);
        return fileSet;
    }

    /**
     * get the '<wrapper-scripts>' files to be copied into the container, e.g.
     *   <wrapper-scripts>/run-with-env.sh 
     * these are required for <run-with-env> modules, e.g.
     *   commandLine=<run-with-env> -u java/1.8 java ...
     */
    protected static Set<File> getWrapperScripts(final DrmJobSubmission gpJob) { 
        return getFileSet(gpJob, GpConfig.PROP_WRAPPER_SCRIPTS_DIR, 
            "run-with-env.sh",
            "env-hashmap.sh",
            "env-lookup.sh",
            "gp-common.sh",
            "env-default.sh",
            "env-custom.sh",
            gpJob.getGpConfig().getGPProperty(gpJob.getJobContext(), "env-custom", "env-custom.sh"),
            "R"
        );
    }
    
    /**
     * get the '<aws-batch-script-dir>' files to be copied into the container
     */
    protected static Set<File> getAwsBatchWrapperScripts(final DrmJobSubmission gpJob) {
        return getFileSet(gpJob, "aws-batch-script-dir", 
            "runS3OnBatch.sh"
        );
    } 

    /**
     * Resolve the path to the named patch directory or file. Paths are resolved
     * relative to the 'patches' directory.
     * 
     * @param gpConfig
     * @param jobContext
     * @param path 
     * @return the path relative to the patches directory
     */
    protected static File getPatchFile(final GpConfig gpConfig, final GpContext jobContext, final String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        
        // special-case: when 'path' is a relative path, make it relative to the patches directory
        File parent = gpConfig.getRootPluginDir(jobContext);
        return new File(parent, path);
    }

    protected static ParameterInfo getFormalParam(final Map<String,ParameterInfoRecord> paramInfoMap, final String pname) {
        if (paramInfoMap.containsKey(pname)) {
            return paramInfoMap.get(pname).getFormal();
        }
        return null;
    }

    protected static void logInputFiles(final Logger log, final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("listing input file values ...");
            final Map<String,ParameterInfoRecord> paramInfoMap =
                    ParameterInfoRecord.initParamInfoMap(gpJob.getJobContext().getTaskInfo());

            // walk through all of the input values
            final JobInput jobInput=gpJob.getJobContext().getJobInput();
            for(final Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {
                final String pname = entry.getKey().getFqName();
                final Param param = entry.getValue();
                final ParameterInfo formalParam = getFormalParam(paramInfoMap, pname);
                if (formalParam != null && formalParam.isInputFile()) {
                    int i=0;
                    for(final ParamValue paramValue : param.getValues()) {
                        log.debug("    "+pname+"["+(i++)+"]: "+paramValue.getValue());
                    }
                    // walk through all of the file list values
                    final HibernateSessionManager mgr = HibernateUtil.instance();
                    final GpConfig gpConfig=ServerConfigurationFactory.instance();
                    final GpContext taskContext=gpJob.getJobContext();
                    final ParameterInfoRecord parameterInfoRecord=paramInfoMap.get(pname);
                    //final Param inputParam;
                    final boolean initDefault=false;
                    final ParamListHelper plh=new ParamListHelper(mgr, gpConfig, taskContext, parameterInfoRecord, jobInput, param, initDefault);
                    if (plh.isCreateFilelist()) {
                        try {
                            final List<GpFilePath> values=ParamListHelper.getListOfValues(mgr, gpConfig, gpJob.getJobContext(), jobInput, formalParam, param, false);
                            int j=0;
                            for(final GpFilePath value : values) {
                                log.debug("    "+pname+"["+(j++)+"]: "+value.getServerFile());
                            }
                        }
                        catch (Exception e) {
                            log.error(e);
                        }
                    }
                }
            }
        }
    }

}
