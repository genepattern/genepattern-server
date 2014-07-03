package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.util.JobResultsFilenameFilter;

/**
 * Hibernate mapping class for saving job output files.
 * @author pcarr
 *
 */
@Entity
@IdClass(JobOutputFilePk.class)
@Table(name="job_output", uniqueConstraints={@UniqueConstraint(columnNames={"gp_job_no", "path"})})
public class JobOutputFile {
    private static final Logger log = Logger.getLogger(JobOutputFile.class);

    public static JobOutputFile from(final String jobId, File jobDir, final String relativePath) throws IOException {
        return from(jobId, jobDir, new File(relativePath));
    }
    
    public static JobOutputFile from(final String jobId, File jobDir, File outputFile) throws IOException {
        if (!jobDir.isAbsolute()) {
            log.warn("expecting absolute path to job directory");
            jobDir=jobDir.getAbsoluteFile();
        }
        
        if (outputFile.isAbsolute()) {
            log.warn("expecting relative path to job result file");
        }
        else {
            outputFile = new File(jobDir, outputFile.getPath());
        }

        Path jobDirPath=jobDir.toPath();
        Path outputFilePath=outputFile.getAbsoluteFile().toPath();
        BasicFileAttributes attrs=null;
        try {
            attrs=Files.readAttributes(outputFilePath, BasicFileAttributes.class);
        }
        catch (Throwable t) {
            log.debug("error getting attributes for outputFilePath="+outputFilePath.toString(), t);
        }

        Path relativePath=jobDirPath.relativize(outputFilePath);
        return from(jobId, relativePath, attrs);
    }

    public static JobResultsFilenameFilter initDefaultFilter() {
        JobResultsFilenameFilter filenameFilter = new JobResultsFilenameFilter();
        filenameFilter.addExactMatch("gp_execution_log.txt");
        filenameFilter.addGlob("*.pipeline_execution_log.html");
        return filenameFilter;
    }

    public static JobResultsFilenameFilter initFilterFromConfig(final GpConfig gpConfig, GpContext gpContext) {
        JobResultsFilenameFilter filenameFilter = initDefaultFilter();
        if (gpConfig == null) {
            log.warn("gpConfig == null");
        }
        else {
            if (gpContext == null) {
                log.debug("gpContext == null");
                gpContext = GpContext.getServerContext();
            }
            Value globPatterns = gpConfig.getValue(gpContext, "job.FilenameFilter");
            if (globPatterns != null) {
                for(String globPattern : globPatterns.getValues()) {
                    filenameFilter.addGlob(globPattern);
                }
            }
        }
        return filenameFilter;
    }
    
    public static JobOutputFile from(final String jobId, final Path relativePath, final BasicFileAttributes attrs) {
        JobOutputFile out=new JobOutputFile();
        out.gpJobNo=Integer.parseInt(jobId);
        out.path=FileUtil.getPath(relativePath, "/");
        
        if (attrs != null) {
            out.lastModified=new Date(attrs.lastModifiedTime().toMillis());
            out.fileLength=attrs.size();
            if (attrs.isDirectory()) {
                out.kind="directory";
            }
        }

        return out;
    }
    
    // ---  primary key fields -------
    @Id
    @Column(name="gp_job_no", nullable=false)
    private Integer gpJobNo=0;
    @Id
    private String path="";
    // ---  end primary key fields -------
    private String gpFileType="";  // STDOUT, STDERR, LOG, FILE, DIR
    private String extension="";
    private String kind="";
    @Column(name="file_length")
    private long fileLength=0L;
    @Column(name="last_modified")
    private Date lastModified=new Date();

    public Integer getGpJobNo() {
        return gpJobNo;
    }
    public void setGpJobNo(Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getGpFileType() {
        return gpFileType;
    }
    public void setGpFileType(String gpFileType) {
        this.gpFileType = gpFileType;
    }
    public String getExtension() {
        return extension;
    }
    public void setExtension(String extension) {
        this.extension = extension;
    }
    public String getKind() {
        return kind;
    }
    public void setKind(String kind) {
        this.kind = kind;
    }
    public long getFileLength() {
        return fileLength;
    }
    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }
    public Date getLastModified() {
        return lastModified;
    }
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    public boolean isHidden() {
        return hidden;
    }
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    private boolean hidden;
    private boolean deleted;

}
