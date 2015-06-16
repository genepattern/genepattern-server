/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.server.dm.webupload.WebUploadPath;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

/**
 * Help class for generating GpFilePath instances from older (although still current in GP-3.3.3) style url paths which contain
 * '/getFile.jsp'.
 * 
 * Template: <GenePatternURL>getFile.jsp?task=<lsid>&job=<job_no>&file=<userid>_run<random_number>.tmp/<filename>
 * 
 * Web upload files are files which were uploaded as part of the basic HTTP submit form for a new job.
 * Example:
 *     http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1222&file=test_run89....546.tmp/all_aml_test.gct
 *     <GenePatternURL>getFile.jsp?task=&job=1222&file=test_run89....546.tmp/all_aml_test.gct
 *     
 * Tasklib files are files which are copied into the taskLib when creating a new provenance pipeline.
 * Example:
 *     http://127.0.0.1:8080/getFile.jsp?task=urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:85:2&file=all_aml_test.cls
 *     <GenePatternURL>getFile.jsp?task=urn:lsid:8080.gp-trunk-dev.120.0.0.1:genepatternmodules:85:2&file=all_aml_test.cls
 * 
 * @author pcarr
 */
public class GetFileDotJspUtil {
    private static Logger log = Logger.getLogger(GetFileDotJspUtil.class);

    //helper methods for getting GpFilePath instances from legacy 'getFile.jsp' urls
    static public GpFilePath getRequestedGpFileObjFromGetFileDotJsp(String urlStr) throws Exception {
        String genePatternUrl = ServerConfigurationFactory.instance().getGpUrl();
        String gpPath=ServerConfigurationFactory.instance().getGpPath();
        //1) handle '<GenePatternURL>' literal
        if (urlStr.startsWith("<GenePatternURL>")) {
            urlStr = urlStr.replace("<GenePatternURL>", genePatternUrl);
        }
        
        //sanity-check, esp because this code is blindly called from the provenance finder
        if (!urlStr.startsWith(genePatternUrl)) {
            throw new Exception("Expecting a GenePatternURL: "+urlStr);
        }
        
        URI uri = null;
        try {
            uri = new URI(urlStr);
        }
        catch (URISyntaxException e) {
            log.error("Error initializing URI from urlStr: "+urlStr, e);
            throw new Exception("Error initializing URI from urlStr: "+urlStr);
        }
        
        String path = uri.getPath();
        //chop off the servlet context (e.g. '/gp')
        if (!path.startsWith(gpPath+"/getFile.jsp")) {
            throw new Exception("Expecting url to start with <GenePatternURL>getFile.jsp: "+urlStr);
        }

        String query = uri.getQuery();
        
        GpFilePath rval = getWebUploadPath(query);
        return rval;
    }
    
    static private GpFilePath getWebUploadPath(final String query) throws Exception {
        //extract the lsid
        String lsid="";
        int lsid_idx = query.indexOf("task=");
        if (lsid_idx >= 0) {
            lsid_idx += "task=".length();
            int lsid_to_idx = query.indexOf("&", lsid_idx);
            if (lsid_to_idx >= 0) {
                lsid = query.substring(lsid_idx, lsid_to_idx);
            }
        }
        //extract the job to which the files were uploaded
        String job="";
        int job_idx = query.indexOf("job=");
        if (job_idx >= 0) {
            job_idx += "job=".length();
            int job_to_idx = query.indexOf("&", job_idx);
            job = query.substring(job_idx, job_to_idx);
        }
        //extract the file
        String file="";
        int file_idx = query.indexOf("&file=");
        if (file_idx >= 0) {
            file_idx += "&file=".length();
        }
        file = query.substring(file_idx);
        file = URLDecoder.decode(file);
        return getWebUploadPath(lsid, job, file);
    }
    static private GpFilePath getWebUploadPath(final String lsid, final String job, final String file) throws Exception {
        if (lsid != null && lsid.length() > 0) {
            return getTaskLibPath(lsid, file);
        }
        return getWebUploadPath(job, file);
    }
    
    static private GpFilePath getTaskLibPath(final String lsid, final String file) {
        TaskInfo taskInfo = TaskInfoCache.instance().getTask(lsid);
        TasklibPath tasklibPath = new TasklibPath(taskInfo, file);
        return tasklibPath;
    }

    /**
     * <java.io.tmpdir>/<userid>_run[0-9]+.tmp/<filename>
     * 
     * Example input path: http://127.0.0.1:8080/gp/getFile.jsp?task=&job=1222&file=test_run89....546.tmp/all_aml_test.gct
     * Template: <GenePatternURL>getFile.jsp?task=<lsid>&job=<job_no>&file=<userid>_run<random_number>.tmp/<filename>
     * @param pathInfo
     * @return
     */
    static public GpFilePath getWebUploadPath(final String job, final String filepath) throws Exception { 
        // extract the userid of the user who originally uploaded the file
        //   need to do some extra steps just in case the userid contains '_run'
        String userid = "";
        int idx2 = filepath.indexOf(".tmp/");
        if (idx2 < 0) {
            throw new Exception("Expecting to find '.tmp/' in the pathInfo. pathInfo="+filepath);
        } 
        int idx1 = filepath.lastIndexOf("_run", idx2);
        if (idx1 < 0) {
            throw new Exception("Expecting to find '_run' in the pathInfo. pathInfo="+filepath);
        }
        userid = filepath.substring(0, idx1);
        return new WebUploadPath(job, userid, filepath);
    }

}
