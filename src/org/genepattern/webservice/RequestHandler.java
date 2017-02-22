/*******************************************************************************
 * Copyright (c) 2003, 2017 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import org.apache.log4j.Logger;

/**
 * RequestHandler.java
 * <p>
 * Description: handles the requests sent to web services.
 * </p>
 * 
 * @author Hui Gong
 * @version $Revision$
 * @deprecated use AnalysisWebServiceProxy instead
 */

public class RequestHandler {
    private static final Logger log = Logger.getLogger(RequestHandler.class);
    private AnalysisWebServiceProxy _proxy;
    private String server;

    public RequestHandler(String server, String username, String password) {
        this.server = server;
        try {
            _proxy = new AnalysisWebServiceProxy(server, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized String getURL() {
        return this.server;
    }

    public synchronized AnalysisService[] getAnalysisServices()
            throws WebServiceException {
        TaskInfo[] tasks = _proxy.getTasks();
        int i, size;
        size = tasks.length;
        AnalysisService[] services = new AnalysisService[size];
        for (i = 0; i < size; i++) {
            services[i] = new AnalysisService(server, tasks[i]);
        }
        return services;
    }

    public synchronized TaskInfo[] getTasks() throws WebServiceException {
        log.debug("Sending request for getTasks()");
        return _proxy.getTasks();
    }

    public synchronized JobInfo submitJob(int id, ParameterInfo[] parmInfos)
            throws WebServiceException {
        log.debug("Sending request to submitJob");
        return _proxy.submitJob(id, parmInfos);
    }

    public synchronized JobInfo checkStatus(int jobID)
            throws WebServiceException {
        log.debug("check job status " + jobID);
        return _proxy.checkStatus(jobID);
    }

    public synchronized String[] getResultFiles(int jobID)
            throws WebServiceException {
        log.debug("Getting result files");
        return _proxy.getResultFiles(jobID);
    }

}
