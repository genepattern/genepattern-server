/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.handler;

import org.genepattern.server.JobIDNotFoundException;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

// import edu.mit.wi.omnigene.omnidas.*;

/**
 * Class used to get Job information based on jobID
 * 
 * @author rajesh kuttan
 * @version 1.0
 */

public class GetJobStatusHandler extends RequestHandler {

    private int jobNo = 0;

    public GetJobStatusHandler() {
        super();
    }

    /**
     * Constructor accepts jobID
     * 
     * @param jobNo
     */
    public GetJobStatusHandler(int jobNo) {
        this.jobNo = jobNo;
    }

    /**
     * To fetch job information <CODE>JobInfo</CODE>
     * 
     * @throws JobIDNotFoundException
     * @throws OmnigeneException
     * @return <CODE>JobInfo</CODE>
     */
    public JobInfo executeRequest() throws JobIDNotFoundException, OmnigeneException {
        JobInfo ji = null;
        try {

            AnalysisDAO ds = new AnalysisDAO();

            ji = ds.getJobInfo(jobNo);
            ji.setParameterInfoArray(ParameterFormatConverter.getParameterInfoArray(ji.getParameterInfo()));
        } catch (JobIDNotFoundException jex) {
            System.out.println("GetJobStatusRequest(executeRequest): JobIDNotFoundException " + jobNo);
            throw jex;
        } catch (Exception ex) {
            System.out.println("GetJobStatusRequest(executeRequest): Error " + ex.getMessage());
            ex.printStackTrace();
            throw new OmnigeneException(ex.getMessage());
        }
        return ji;
    }

    public static void main(String args[]) {
        GetJobStatusHandler arequest = new GetJobStatusHandler(100);

        try {
            JobInfo ji = arequest.executeRequest();
            ParameterInfo[] parameterInfoArray = ji.getParameterInfoArray();
            System.out.println("Execute Result " + ji.getTaskID() + parameterInfoArray[0].getValue());
        } catch (JobIDNotFoundException jex) {
            System.out.println("Job id Exception caught");
        } catch (Exception e) {
            if (e instanceof JobIDNotFoundException)
                System.out.println("Rajesh found...........");
            e.printStackTrace();
        }
    }

}
