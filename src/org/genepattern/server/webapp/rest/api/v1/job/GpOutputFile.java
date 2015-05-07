/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for generating the JSON representation for a job result file.
 * @author pcarr
 *
 */
public class GpOutputFile {
    private static final Logger log = Logger.getLogger(GpOutputFile.class);
    
    public static GpOutputFile fromGpfilePath(final String gpUrl, final GpFilePath gpFilePath, final ParameterInfo pinfo) {
        GpOutputFile f = new GpOutputFile();
        f.href=initHref(gpUrl, gpFilePath);
        f.rels=initRels(pinfo);
        f.path=gpFilePath.getRelativePath();
        f.fileLength=gpFilePath.getFileLength();
        f.lastModified=gpFilePath.getLastModified();
        f.kinds=new ArrayList<String>();
        f.kinds.add(gpFilePath.getKind());
        
        return f;
    }

    /**
     * Helper method, is the given pinfo the execution log.
     * @param pinfo
     * @return
     */
    public static boolean isExecutionLog(final ParameterInfo param) {
        boolean isExecutionLog = (
                param.getName().equals(GPConstants.TASKLOG) || 
                param.getName().endsWith(GPConstants.PIPELINE_TASKLOG_ENDING));
        return isExecutionLog;
    }
    
    private static String initHref(final String gpUrl, final GpFilePath gpFilePath) {
        final String href=gpUrl+gpFilePath.getRelativeUri().toString();
        return href;
    }
    
    private static List<Rel> initRels(final ParameterInfo pinfo) {
        List<Rel> rels=new ArrayList<Rel>();
        if (isExecutionLog(pinfo)) {
            // for the REST API, an execution log is not an output file
            rels.add(Rel.gp_logFile);
            return rels;
        }

        // stdout and stderr are also output files
        if (pinfo._isStderrFile()) {
            rels.add(Rel.gp_stderr);
        }
        else if (pinfo._isStdoutFile()) {
            rels.add(Rel.gp_stdout);
        }
        if (pinfo.isOutputFile()) {
            rels.add(Rel.gp_outputFile);
        }

        return rels;
    }

    private String href;
    private String path;
    private List<Rel> rels;
    private long fileLength;
    private Date lastModified;
    private List<String> kinds;
    
    /**
     * Create a JSON representation of a file.
     * 
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        //create a JSON representation of a file
        JSONObject o = new JSONObject();
        
        GpLink link=new GpLink.Builder()
            .href(href)
            .name(path)
            .addRels(rels)
        .build();

        o.put("link", link.toJson());
        o.put("fileLength", fileLength);
        o.put("lastModified", DateUtil.toIso8601(lastModified));
        // include relative path, to make it easier to work with files in sub directories
        try {  
            if (path != null) {
                o.put("path", path);
            }
        }
        catch (Throwable t) {
            log.error("Unexpected error in gpFilePath.relativePath", t);
        }

        JSONArray kindArr=new JSONArray();
        if (kinds != null) {
            for(final String kind : kinds) {
                kindArr.put(kind);
            }
        }
        o.put("kind", kindArr);
        
        return o;
    }

}
