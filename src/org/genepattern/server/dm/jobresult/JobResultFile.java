package org.genepattern.server.dm.jobresult;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class JobResultFile extends GpFilePath {
    public static Logger log = Logger.getLogger(JobResultFile.class);
    
    private String jobId = null;
    private File relativeFile = null;
    private File serverFile = null;
    private URI relativeUri = null;
    private boolean isWorkingDir = false;
    
    public JobResultFile(JobInfo jobInfo, File relativePath) throws ServerConfiguration.Exception {
        String jobId = ""+jobInfo.getJobNumber();
        init(jobId, relativePath);
    }

    public JobResultFile(String pathInfo) throws Exception {
        //e.g. /<jobId>/<relativePath>
        if (pathInfo == null) {
            throw new Exception("pathInfo == null");
        }
        if (!pathInfo.startsWith("/")) {
            throw new Exception("pathInfo must start with a '/'");
        }
        String jobId = "";
        String relativePath = ".";
        int idx = pathInfo.indexOf("/", 1);
        if (idx < 0) {
            jobId = pathInfo.substring(1);
            relativePath = ".";
        }
        else {
            jobId = pathInfo.substring(1, idx);
            if (pathInfo.length() > (idx + 1)) {
                relativePath = pathInfo.substring(idx + 1);
            }
        }
        init(jobId, new File(relativePath));
    }

    public JobResultFile(ParameterInfo outputParam) throws Exception {
        //circa gp-3.3.3 and earlier, value is of the form, <jobid>/<filepath>, e.g. "1531/Hind_0001.snp"
        this("/" + outputParam.getValue());
    }

    
    private void init(String jobId, File relativeFile) throws ServerConfiguration.Exception {
        if (relativeFile == null) {
            throw new IllegalArgumentException("invalid null arg, relativePath");
        }
        if (relativeFile.isAbsolute()) {
            throw new IllegalArgumentException("file must be a relative path");
        }
        this.jobId = jobId;
        this.relativeFile = relativeFile;
        this.isWorkingDir = relativeFile.getName().equals("") || relativeFile.getName().equals(".");

        //init the relativeUri
        String uriPath = "/jobResults/" + jobId + "/" + UrlUtil.encodeFilePath(relativeFile);
        try {
            relativeUri = new URI( uriPath );
        }
        catch (URISyntaxException e) {
            log.error(e);
            throw new IllegalArgumentException(e);
        }
        
        //TODO: get the working dir for the job, currently all jobs are stored relative to the "jobs" directory
        ServerConfiguration.Context context = ServerConfiguration.Context.getServerContext();
        File rootJobDir = ServerConfigurationFactory.instance().getRootJobDir(context);
        
        File jobDir = new File(rootJobDir, jobId);
        this.serverFile = new File(jobDir, relativeFile.getPath()); 
        //TODO: cache this in the DB
        this.initMetadata();
    }
    
    public String getJobId() {
        return jobId;
    }

    /**
     * @return true if this file object refers to the working directory for its job.
     */
    public boolean isWorkingDir() {
        return isWorkingDir;
    }

    public URI getRelativeUri() {
        return relativeUri;
    }

    public File getServerFile() {
        return serverFile;
    }

    public File getRelativeFile() {
        return relativeFile;
    }

    public boolean canRead(boolean isAdmin, Context userContext) {
        if (isAdmin) {
            return true;
        }
        try {
            int jobNumber = -1;
            jobNumber = Integer.parseInt(jobId);
            PermissionsHelper perm = new PermissionsHelper(isAdmin, userContext.getUserId(), jobNumber);
            return perm.canReadJob();
        }
        catch (Throwable t) {
            log.error("Error checking permissions for userId="+userContext.getUserId()+" and jobId="+jobId, t);
            return false;
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

    public String getFormFieldValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        return null;
    }

}

