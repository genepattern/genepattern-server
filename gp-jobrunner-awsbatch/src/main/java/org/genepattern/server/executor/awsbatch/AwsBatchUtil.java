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
        
        // special-case, sync the entire wrapper-scripts directory
        final File wrapperScripts=getGPFileProperty(gpJob, GpConfig.PROP_WRAPPER_SCRIPTS_DIR);
        if (wrapperScripts != null && wrapperScripts.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("gpJobNo="+gpJob.getGpJobNo()+", sync wrapper_scripts directory: "+wrapperScripts);
            }
            inputFiles.add(wrapperScripts);
        }

//        // special-case, include <run-with-env> support files
//        final Set<File> additionalSupportFiles=getWrapperScripts(gpJob);
//        inputFiles.addAll(additionalSupportFiles);
//        // special-case, include selected <aws-batch-scripts>
//        inputFiles.addAll(getAwsBatchWrapperScripts(gpJob));
        return Collections.unmodifiableSet(inputFiles);
    }

    protected static Set<File> getJobInputFiles(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+gpJob.getGpJobNo()+", listing local file paths ...");
        }

        // linked hash set preserves insertion order
        final Set<File> jobInputFiles = new LinkedHashSet<File>();
        for(final String localFilePath : gpJob.getJobContext().getLocalFilePaths()) {
            if (Strings.isNullOrEmpty(localFilePath)) {
                // skip empty path
            }
            else {
                final File file=new File(localFilePath);
                if (file != null && file.exists()) {
                    jobInputFiles.add(file);
                }
                else {
                    log.error("gpJobNo="+gpJob.getGpJobNo()+", file doesn't exist for localFilePath='"+localFilePath+"'");
                }
            }
        }
        return Collections.unmodifiableSet(jobInputFiles);
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
     * @return
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
