
package org.genepattern.server;


import org.genepattern.webservice.OmnigeneException;
/**
 * This Exception is used when Taskid is not found in analysis service
 *
 * @author Rajesh Kuttan
 * @version 1.0
 */

public class TaskIDNotFoundException extends OmnigeneException  {

    /** Creates new TaskIDNotFoundException */
    public TaskIDNotFoundException() {
    }

    public TaskIDNotFoundException(String strMessage)
    {
        super(strMessage);
    }

    public TaskIDNotFoundException(int errno)
     {
    super(errno);
     }
}




