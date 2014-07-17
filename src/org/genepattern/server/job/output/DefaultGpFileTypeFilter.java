package org.genepattern.server.job.output;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;

public class DefaultGpFileTypeFilter implements GpFileTypeFilter {
    private static final Logger log = Logger.getLogger(DefaultGpFileTypeFilter.class);
    
    private FilenameFilter filenameFilter=null;
    
    private String stdoutFilename="stdout.txt";
    private String stderrFilename="stderr.txt";
    private String executionLogFilename="gp_execution_log.txt";
    private String pipelineLogFilenameSuffix="_execution_log.html";
    
    public void setStdoutFilename(final String stdoutFilename) {
        this.stdoutFilename=stdoutFilename;
    }
    public void setStderrFilename(final String stderrFilename) {
        this.stderrFilename=stderrFilename;
    }
    public void setGpExecutionLogFilename(final String gpExecutionLogFilename) {
        this.executionLogFilename=gpExecutionLogFilename;
    }
    public void setPipelineLogFilenameSuffix(final String pipelineLogFilenameSuffix) {
        this.pipelineLogFilenameSuffix=pipelineLogFilenameSuffix;
    }
    
    public void setJobResultsFilenameFilter(FilenameFilter filenameFilter) {
        this.filenameFilter=filenameFilter;
    }

    @Override
    public boolean accept(File dir, String name) {
        if (filenameFilter==null) {
            return true;
        }
        return filenameFilter.accept(dir, name);
    }

    @Override
    public GpFileType getGpFileType(File jobDir, File relativePath, BasicFileAttributes attrs) {
        if (relativePath==null) {
            log.error("relativePath==null");
            return null;
        }
        if (relativePath.isAbsolute()) {
            log.error("expecting relative path, relativePath="+relativePath);
            return null;
        }
        
        boolean isHidden = filenameFilter == null ? false :
            !filenameFilter.accept(jobDir, relativePath.getName());

        if ("".equals(relativePath.getPath())) {
            return GpFileType.GP_JOB_DIR;
        }
        if (relativePath.getPath().equals(executionLogFilename)) {
            return GpFileType.GP_EXECUTION_LOG;
        }
        if (relativePath.getPath().endsWith(pipelineLogFilenameSuffix)) {
            return GpFileType.GP_PIPELINE_LOG;
        }
        if (relativePath.getPath().equals(stdoutFilename)) {
            return GpFileType.STDOUT;
        }
        if (relativePath.getPath().equals(stderrFilename)) {
            return GpFileType.STDERR;
        }
        if (attrs!=null && attrs.isDirectory()) {
            return !isHidden ? GpFileType.DIR : GpFileType.GP_HIDDEN_DIR;
        }
        if (attrs!=null && !attrs.isDirectory()) {
            return !isHidden ? GpFileType.FILE : GpFileType.GP_HIDDEN_FILE;
        }
        
        log.debug("Unknown gpFileType for file="+relativePath);
        return null;
    }

}
