
package org.genepattern.server.analysis;

/**
 * This Exception is used when Jobid is not found in analysis service
 *
 * @author Rajesh Kuttan
 * @version 1.0
 */
import org.genepattern.analysis.OmnigeneException;

public class JobIDNotFoundException extends OmnigeneException {

    /** Creates new JobIDNotFoundException */
    public JobIDNotFoundException() {
        super();
    }


    public JobIDNotFoundException(String strMessage)
    {
        super(strMessage);
    }

    public JobIDNotFoundException(int errno)
     {
    super(errno);
     }
}








