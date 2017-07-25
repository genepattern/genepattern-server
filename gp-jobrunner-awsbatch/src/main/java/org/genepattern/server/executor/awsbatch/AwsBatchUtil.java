package org.genepattern.server.executor.awsbatch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
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
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
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

//    public static final Value getOrDefault(final GpConfig gpConfig, final GpContext jobContext, final String key, final Value defaultValue) {
//        return gpConfig.getValue(jobContext, key, defaultValue);
//    }

    protected static File getWrapperScriptsDir(final GpConfig gpConfig, final GpContext jobContext) {
        return gpConfig.getGPFileProperty(jobContext, GpConfig.PROP_WRAPPER_SCRIPTS_DIR);
    }

    /**
     * Get the local path to the '<wrapper-scripts>' directory
     */
    protected static File getWrapperScriptsDir(final DrmJobSubmission gpJob) {
        return getWrapperScriptsDir(gpJob.getGpConfig(), gpJob.getJobContext());
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

    protected static void makeSymLink(final File linkDir, final File target, final String linkName) throws CommandExecutorException {
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
    protected static void makeSymLink(final File linkDir, final Path target, final String linkName) throws CommandExecutorException {
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
        // special-case, include <run-with-env> support files
        final Set<File> additionalSupportFiles=getWrapperScripts(gpJob);
        inputFiles.addAll(additionalSupportFiles);
        return Collections.unmodifiableSet(inputFiles);
    }

    protected static Set<File> getJobInputFiles(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+gpJob.getGpJobNo()+", listing local file paths ...");
        }

        // linked hash set preserves insertion order
        final Set<File> jobInputFiles = new LinkedHashSet<File>();
        for(final String localFilePath : gpJob.getJobContext().getLocalFilePaths()) {
            final File file=initJobInputFile(gpJob, localFilePath);
            if (file != null && file.exists()) {
                jobInputFiles.add(file);
            }
        }
        return Collections.unmodifiableSet(jobInputFiles);
    }
    
    // called-by getJobInputFiles
    protected static File initJobInputFile(final DrmJobSubmission gpJob, final String localFilePath) {
        log.debug("    localFilePath="+localFilePath);
        if (Strings.isNullOrEmpty(localFilePath)) {
            return null;
        }
        final File file=new File(localFilePath);
        if (file.exists()) {
            return file;
        }
        else {
            log.error("file doesn't exist, for gpJobNo="+gpJob.getGpJobNo()+", localFilePath="+localFilePath);
            return null;
        }
    }

    protected static Set<File> getWrapperScripts(final DrmJobSubmission gpJob) {
        if (log.isDebugEnabled()) {
            log.debug("gpJobNo="+gpJob.getGpJobNo()+", including <run-with-env> files ...");
        }
        final Set<File> wrapperScriptFiles = new LinkedHashSet<File>();
        final Path wrapperScripts=getWrapperScriptsDir(gpJob).toPath();
        final List<String> supportFiles=Arrays.asList(
            "run-with-env.sh",
            "env-hashmap.sh",
            "env-lookup.sh",
            "gp-common.sh",
            "env-default.sh",
            "env-custom.sh",
            gpJob.getGpConfig().getGPProperty(gpJob.getJobContext(), "env-custom", "env-custom.sh")
        );
        for(final String path : supportFiles) {
            if (Strings.isNullOrEmpty(path)) {
                // skip empty path
            }
            else {
                final File file=wrapperScripts.resolve(path).toFile();
                if (file != null && file.exists()) {
                    if (log.isDebugEnabled()) {
                        log.debug("    <wrapper-script>/"+path);
                    }
                    wrapperScriptFiles.add(file);
                }
            }
        }
        return Collections.unmodifiableSet(wrapperScriptFiles); 
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
                }
            }
        }
    }

}
