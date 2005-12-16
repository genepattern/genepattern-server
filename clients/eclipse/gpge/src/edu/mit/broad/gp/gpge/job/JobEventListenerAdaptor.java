/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


/*
 * Created on Jul 21, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.job;

import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;

/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class JobEventListenerAdaptor implements JobEventListener {

    /* (non-Javadoc)
     * @see edu.mit.broad.gp.gpge.job.JobEventListener#jobFinished(edu.mit.genome.gp.ui.analysis.AnalysisJob)
     */
    public void jobFinished(AnalysisJob job) {
       
    }

    public void jobStatusChange(JobInfo jobInfo){
        
    }
    
    
}
