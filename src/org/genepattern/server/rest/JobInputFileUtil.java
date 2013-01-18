package org.genepattern.server.rest;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;

/**
 * Utility class for job input files which are uploaded directly from a web client
 * as part of the job submission,
 * or which are copied from an external url prior to job execution.
 * 
 * By default copy data files into the Uploads tab for the current user, relative to the uploadPath.
 * If an uploadPath is not provided, automatically generate one relative to the 'default_root_path'.
 * 
 * default_root_path="./tmp", sets the default location
 * E.g.
 * {Uploads}/tmp/{unique_id_for_job}/{param.name}.filelist
 * {Uploads}/tmp/{unique_id_for_job}/{param.name}/{idx}/{filename}
 * 
 * @author pcarr
 *
 */
public class JobInputFileUtil {
    final static private Logger log = Logger.getLogger(JobInputFileUtil.class);
    
    //Note: can't be 'temp', because of a problem with the existing implementation of the User Uploads tab
    final static public String DEFAULT_ROOT_PATH="tmp/";

    //to use instead of a jobId, because we don't have one yet
    private String uploadPath; 
    //for getting the currentUser, and optionally current task and current job
    private Context context;
    /**
     * The base input directory for a given job.
     */
    private GpFilePath inputFileDir;


    public JobInputFileUtil(final Context jobContext) throws Exception {
        this(jobContext, null);
    }

    public JobInputFileUtil(final Context jobContext, final String relativePath) throws Exception {
        this.context=jobContext;
        this.uploadPath=relativePath;
        this.inputFileDir=initInputFileDir();
    }
    
    public void setContext(Context jobContext) {
        this.context=jobContext;
    }
    public void setUploadPath(final String relativePath) {
        this.uploadPath=relativePath;
        //should be a relative path to a directory (always use '/' separator)
        if (!uploadPath.endsWith("/")) {
            uploadPath = uploadPath+"/";
        }
    }
    public String getUploadPath() {
        return uploadPath;
    }
    public JobInputFileUtil(final String uploadPath) {
        this.uploadPath=uploadPath;
        if (!uploadPath.endsWith("/")) {
            this.uploadPath = uploadPath+"/";
        }
    }

    private GpFilePath initInputFileDir() throws Exception {
        if (context==null) {
            throw new IllegalArgumentException("context==null");
        }
        if (uploadPath == null) {
            // automatically generate the uploadPath, so that it maps to a unique location
            // in the users uploads tab
            File rootPath=new File(DEFAULT_ROOT_PATH);
            GpFilePath parentDirPath=GpFileObjFactory.getUserUploadFile(context, rootPath);
            File parentDirFile=parentDirPath.getServerFile();
            if (!parentDirFile.exists()) {
                boolean success=parentDirFile.mkdirs();
                if (!success) {
                    log.error("false return value from mkdirs( "+parentDirFile.getAbsolutePath()+" )");
                    throw new Exception("Unable to create parent upload dir for job: "+parentDirFile.getPath());
                }
            }
            File tmpFile=File.createTempFile("run", null, parentDirFile);
            boolean success=tmpFile.delete();
            if (!success) {
                throw new Exception("Unable to create uplodate directory for job, couldn't delete the tmpFile: "+tmpFile.getPath());
            }
            success=tmpFile.mkdirs();
            if (!success) {
                throw new Exception("Unable to create upload directory for job: "+tmpFile.getPath());
            }
            this.uploadPath=DEFAULT_ROOT_PATH+tmpFile.getName()+"/";
        }
        GpFilePath gpFilePath=GpFileObjFactory.getUserUploadFile(context, new File(this.uploadPath));
        return gpFilePath;
    }

    public GpFilePath getDistinctPathForFilelist(final String paramName) throws Exception {
        initInputFileDir();
        File rel=new File(inputFileDir.getRelativeFile(), paramName+".filelist");
        GpFilePath input=GpFileObjFactory.getUserUploadFile(context, rel);
        
        File parentFile=input.getServerFile().getParentFile();
        if (!parentFile.exists()) {
            boolean success=parentFile.mkdirs();
            if (!success) {
                String message="Can't create upload dir for filelist: "+parentFile.getPath();
                log.debug(message);
                throw new Exception(message);
            }
        }
        File serverFile=input.getServerFile();
        if (serverFile.exists()) {
            String message="filelist file already exists: "+serverFile.getAbsolutePath();
            log.error(message);
            throw new Exception(message);
        } 
        return input;
    }

    /**
     * For a given index (into a file list, default first value is 0), and a given input file parameter name,
     * return a gpFilePath object to be used for saving the file into the user uploads tab.
     * 
     * @param idx
     * @param paramName
     * @return
     */
    public GpFilePath getDistinctPathForUploadParam(final Context jobContext, final int idx, final String paramName) throws Exception {
        initInputFileDir();
        File rel=inputFileDir.getRelativeFile();
        
        //naming convention
        File next=new File(rel,"paramName");
        if (idx>=0) {
            next=new File(next,""+idx);
        }
        GpFilePath input=GpFileObjFactory.getUserUploadFile(jobContext, next);
        return input;
    }
    
    static public GpFilePath getDistinctPathForExternalUrl(final Context jobContext, final URL url) throws Exception {
        File relPath=new File(DEFAULT_ROOT_PATH+"external/"+url.getHost()+"/"+url.getPath());
        GpFilePath input=GpFileObjFactory.getUserUploadFile(jobContext, relPath);
        return input;
    }

}
