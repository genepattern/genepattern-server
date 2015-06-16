/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.jobresult;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationException;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

/**
 * Internal representation of a job result file as a GpFilePath object.
 * Use of this class is deprecated, as soon as the dependency on the PARAMETER_INFO CLOB is removed.
 * @author pcarr
 *
 */
public class JobResultFile extends GpFilePath {
    public static Logger log = Logger.getLogger(JobResultFile.class);
    
    private String jobId = null;
    private File relativeFile = null;
    private File serverFile = null;
    private URI relativeUri = null;
    private boolean isWorkingDir = false;
    
    public JobResultFile(JobInfo jobInfo, File relativePath) throws ServerConfigurationException {
        String jobId = ""+jobInfo.getJobNumber();
        init(jobId, relativePath);
    }

    /**
     * Create a new JobResultFile (GpFilePath) instance from the given pathInfo, 
     *     /<jobId>/<relativePath>
     * The relative path should be encoded as a valid file system file (not as a URI).
     * @param pathInfo
     * @throws Exception
     */
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

    /**
     * Initialize the relative uri path for the given job result file. For example for jobId=5 which produces
     * and output file named 'all_aml_test.cvt.gct'
     *     initRelativePath("5", new File("all_aml_test.cvt.gct"));
     * will return
     *     /jobResults/5/all_aml_test.cvt.gct
     * 
     * This method encodes the File into a valid URI path component which can be used to construct the URL to the file.
     * 
     * @param jobId, the GP jobId
     * @param relativeFile, the File object as a relativePath to the file
     * @return
     */
    public static String initRelativePath(final String jobId, final File relativeFile) {
        String uriPath = "/jobResults/" + jobId + "/" + UrlUtil.encodeFilePath(relativeFile);
        return uriPath;
    }
    
    public static URI initRelativeUri(final String jobId, final File relativeFile) {
        final String uriPath=initRelativePath(jobId, relativeFile);
        try {
            return new URI( uriPath );
        }
        catch (URISyntaxException e) {
            log.error(e);
            throw new IllegalArgumentException(e);
        }
    }
    
    private void init(String jobId, File relativeFile) throws ServerConfigurationException {
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
        relativeUri = initRelativeUri(jobId, relativeFile);
        
        //TODO: get the working dir for the job, currently all jobs are stored relative to the "jobs" directory
        GpContext context = GpContext.getServerContext();
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

    public boolean canRead(boolean isAdmin, GpContext userContext) {
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

