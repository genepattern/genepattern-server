/*
 * TaskSubmitter.java
 *
 * Created on April 1, 2003, 11:34 PM
 */
package org.genepattern.gpge.ui.tasks;

import org.genepattern.analysis.OmnigeneException;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.client.AnalysisJob;
import org.genepattern.client.AnalysisService;
import org.genepattern.client.RequestHandler;


/** defines a method that will submit the task and also a method that will
 * determine the suitability of applying this TaskSubmitter to the AnalysisService
 *
 * @author kohm
 */
    public interface TaskSubmitter {
        /** submits the task */
        public AnalysisJob submitTask(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) throws OmnigeneException, WebServiceException, java.io.IOException;
        /** determines if this submitter is acceptable for the AnalysisService */
        public boolean check(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler);
    }

