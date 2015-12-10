package org.genepattern.server.dm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.util.SemanticUtil;
import org.junit.Test;

public class TestGpFilePathType {
    
    @Test
    public void buildServerFile() {
        final File file=FileUtil.getDataFile("all_aml/all_aml_test.gct").getAbsoluteFile();
        GpFilePathType gpFilePath=new GpFilePathType.Builder()
            .newServerFilePath(file)
        .build();
        
        assertEquals("name", "all_aml_test.gct", gpFilePath.getName()); 
        assertEquals("serverFile", file, gpFilePath.getServerFile());
        assertEquals("href", "/data/" + file.getPath(), gpFilePath.getHref());
        assertEquals("isDirectory", false, gpFilePath.isDirectory());
    }
    
    @Test
    public void buildJobResultFile() {
        final File jobDir=FileUtil.getDataFile("jobResults/0").getAbsoluteFile();
        GpFilePathType gpFilePath=new GpFilePathType.JobResultBuilder()
            // jobId from jobDir.name
            .jobDir(jobDir)
            .filePath("all_aml_test.comp.marker.odf")
        .build();
        assertEquals("name", "all_aml_test.comp.marker.odf", gpFilePath.getName());
        assertEquals("href", "/jobResults/0/all_aml_test.comp.marker.odf", gpFilePath.getHref());
        assertEquals("kind", "Comparative Marker Selection", gpFilePath.getKind()); 
                
        // build job dir href
        gpFilePath=new GpFilePathType.JobResultBuilder()
            .jobDir(jobDir)
            .filePath("")
        .build();
        assertEquals("the jobDir isDirectory", true, gpFilePath.isDirectory());
        assertEquals("name", "0", gpFilePath.getName());
        assertEquals("href", "/jobResults/0/", gpFilePath.getHref());
        assertEquals("kind", "directory", gpFilePath.getKind()); 
        
        // build nested dir href
        gpFilePath=new GpFilePathType.JobResultBuilder()
            .jobDir(jobDir)
            .filePath("a")
        .build();
        assertEquals("a nested dir isDirectory", true, gpFilePath.isDirectory());
        assertEquals("name", "a", gpFilePath.getName());
        assertEquals("href", "/jobResults/0/a/", gpFilePath.getHref());
        assertEquals("kind", "directory", gpFilePath.getKind()); 
    }

}

/**
 * Prototype re-write of the GpFilePath class; replace the abstract class and hierarchy with a Builder pattern; strive for immutable types.
 * 
 * Scenarios to keep in mind:
 *   -- initialize meta data from the file system
 *   
 *   -- load cached meta data from a DB in some cases; to avoid too many file system calls when enumerating lists of files,
 *      for example for building directory trees
 *   
 *   -- would like to use the class as a specification for a File to be created, e.g. when receiving new file upload in the GUI
 *   -- would like to use the class as a mapping between an external href value and a cached server file path
 *   
 *   -- special-cases:
 *   ---- partial uploads
 *   ---- hidden files for the data cache
 * 
 * @author pcarr
 *
 */
class GpFilePathType {

    /**
     * To solve the problem of identifying different types of GP server files; 
     *     phase 1, must support some kind of reverse lookup, by unique servletPath
     *     phase 2, could avoid this by saving the type as part of the ParamValue; must be persisted to/from the DB 
     */
    public static enum Type {
        USER_UPLOAD("/users", "/{userId}/{path}"),
        JOB_RESULT("/jobResults", "/{jobId}/{path}"),
        TASKLIB("/tasklib", "/{taskNameOrLsid}/{path}"),
        DATA("/data", "/{relativeOrAbsolutePath}");  // http://genepattern.broadinstitute.org/gp/data//xchip/gpprod/shared_data/workshop_files/all_aml_test.cls

        private Type(final String servletPath, final String pathTemplate) {
            this.servletPath=servletPath;
            this.pathTemplate=pathTemplate;
        }
        private final String servletPath;
        private final String pathTemplate;

        public String getServletPath() {
            return servletPath;
        }
        public String getPathTemplate() {
            return pathTemplate;
        }
    }

    /** 
     * required, the relative or fully qualified href to a data file 
     * if relative, it's relative to the baseGpUrl, should start with a '/', builder will append if necessary.
     * if absolute, assume it's an external URL
     * 
     * By analogy to File.isAbsolute() except that absolute hrefs are external files and relative hrefs
     * are relative to the baseGpUrl.
     */
    private String href=null; // internal is a relative path, external is fully qualified

    private Boolean isLocal=null;

    // optionally set the owner of the local file
    private String owner=null;

    // encoded as a file system file name, when constructing href from the name need to encode it
    // something like  baseGpHref + servletPath + UrlUtil.encodeURIcomponent(name)
    private String name=null;
    private String extension=null;
    private String kind=null;
    private Boolean isDirectory=null; // [ false | true | null=unknown ]

    //cached file metadata
    private File serverFile=null; // should be a fully qualified path
    private Date lastModified=null;
    private Long fileLength=null;
    
    public Boolean isLocal() {
        return isLocal;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public Boolean isDirectory() {
        return isDirectory;
    }
    
    public String getHref() {
        return href;
    }
    
    public File getServerFile() {
        return serverFile;
    }

    public String getName() {
        return name;
    }
    
    public String getKind() {
        return kind;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public Date getLastModified() {
        return lastModified;
    }

    public long getFileLength() {
        return fileLength;
    }
    
    
    /** must use fluent builder pattern to create new immutable instance. */
    private GpFilePathType() {}

    /** generic builder, note: could implement a different type of builder to be used as a replacement for GpFileObjectFactory. */
    public static class Builder {
         protected GpFilePathType instance=new GpFilePathType();

        public Builder href(final String href) {
            instance.href=href;
            return this;
        }

        // infer meta-data, including kind and isDirectory from serverFile
        public Builder serverFile(final File serverFile) {
            instance.serverFile=serverFile;
            return this;
        }

        public Builder isDirectory(final boolean isDirectory) {
            instance.isDirectory=isDirectory;
            return this;
        }

        /**
         * sets the metaData from a local file; prefer the file to be absolute and to exist.
         * 
         * @param file
         * @return
         */
        public Builder metaData(final File file) {
            if (file==null) {
                throw new IllegalArgumentException("file==null");
            }
            instance.name=file.getName();
            instance.extension=SemanticUtil.getExtension(file);
            if (file.exists()) {
                instance.kind=SemanticUtil.getKind(file, instance.extension);
                instance.isDirectory=file.isDirectory();
                instance.lastModified=new Date(file.lastModified());
                instance.fileLength=file.length();
            }
            return this;
        }
        
        /**
         * Initialize a new ServerFilePath
         * @param file
         * @return
         */
        public Builder newServerFilePath(final File file) {
            if (file==null) {
                throw new IllegalArgumentException("file==null");
            }
            if (!file.isAbsolute()) {
                throw new IllegalArgumentException("Expecting absolute file");
            }

            instance.isLocal=true;
            instance.serverFile=file;
            instance.href= Type.DATA.getServletPath()+ "/" + UrlUtil.encodeFilePath(file); 
            return metaData(file);
        }
        

        public GpFilePathType build() {
            return instance;
        }

    }
    
    /** initialize JobResult file from a directory listing on the file system. */
    public static class JobResultBuilder extends Builder {
        private String jobId=null;
        private File jobDir=null;
        private File filePath=null;
        
        public JobResultBuilder jobId(final String jobId) {
            this.jobId=jobId;
            return this;
        }
        
        /** the working directory for the job, required */
        public JobResultBuilder jobDir(final File jobDir) {
            this.jobDir=jobDir;
            if (jobId==null) {
                jobId=jobDir.getName();
            }
            return this;
        }
        
        /** 
         * usually a file name; path is relative to the job dir. e.g.
         *     'all_aml_test.gct',  <---- a file in the job dir
         *     'sub_dir/',          <---- a sub directory in the job dir
         *     'sub_dir/report.html' <---- a file in a sub directory
         *     '' <---- empty string means the path is to the jobDir
         */
        public JobResultBuilder filePath(final String filePath) {
            // usually a file name
            this.filePath=new File(filePath);
            if (this.filePath.isAbsolute()) {
                throw new IllegalArgumentException("Expecting a filename or relative path");
            }
            return this; 
        }
        
        public GpFilePathType build() {
            if (jobId==null) {
                throw new IllegalArgumentException("must set jobId");
            }
            if (filePath==null) {
                // warning: assuming you are building the jobDir instance
            }
            if (jobDir==null) {
                // can't set meta data, can construct href
            }

            
            if (jobDir != null && filePath != null) {
                // init meta data
                File fqFile=new File(jobDir, filePath.getPath());
                metaData(fqFile);
            }
            else if (filePath != null) { // jobDir is null
                metaData(filePath);
            }
            else if (jobDir != null) {  // filePath is  null
                throw new IllegalArgumentException("filePath is null, jobDir isn't, try passing in ./ as filePath");
            }
            else {
                throw new IllegalArgumentException("jobDir=null and filePath=null");
            }
            
            instance.href= Type.JOB_RESULT.getServletPath()+ "/" +  UrlUtil.encodeURIcomponent(jobId) +
                    "/" + UrlUtil.encodeFilePath(filePath);
            if (instance.isDirectory && !instance.href.endsWith("/")) {
                instance.href=instance.href+"/";
            }
            return instance;
        }
        
    }

}
