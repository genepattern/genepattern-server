
package org.genepattern.server.analysis;

/**
 * This Exception is used when Jobid is not found in analysis service
 *
 * @author Rajesh Kuttan
 * @version 1.0
 */
import org.genepattern.webservice.OmnigeneException;

public class NoTaskFoundException extends OmnigeneException {

    /** Creates new NoTasksFoundException */
    public NoTaskFoundException() {
        super();
    }


    public NoTaskFoundException(String strMessage)
    {
        super(strMessage);
    }

    public NoTaskFoundException(int errno)
     {
    super(errno);
     }
}








