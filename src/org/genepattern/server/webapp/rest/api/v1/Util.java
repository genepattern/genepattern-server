package org.genepattern.server.webapp.rest.api.v1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class Util {
    /**
     * Create a new userContext instance based on the current HTTP request.
     * This method has the effect of requiring a valid logged in gp user, because a 
     * RuntimeException will be thrown if the user is not logged in.
     * 
     * @param request
     * @return
     * @throws WebApplicationException if there is not a current user.
     */
    public static GpContext getUserContext(final HttpServletRequest request) {
        final String userId=(String) request.getSession().getAttribute("userid");
        if (userId==null || userId.length()==0) {
            //user not logged in, 403 - Forbidden
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        final boolean initIsAdmin=true;
        GpContext userContext = GpContext.getContextForUser(userId, initIsAdmin);
        return userContext;
    }
        
    public static GpContext getTaskContext(
            final HttpServletRequest request,
            final String taskNameOrLsid) 
    throws WebServiceException
    {
        GpContext taskContext=getUserContext( request );
        TaskInfo taskInfo = getTaskInfo( taskNameOrLsid, taskContext.getUserId() );
        taskContext.setTaskInfo(taskInfo);
        return taskContext;
    }

    private static TaskInfo getTaskInfo(final String taskLSID, final String username) 
    throws WebServiceException 
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
    
    /**
     * Outputs the date in 'ECMAScript 5 ISO-8601' format.
     * 
     * @param date
     * @return
     */
    public static String toIso8601(final Date date) {
        //TODO: optimize, no need to create a new dateFormat instance for each call
        //     however, DateFormat is not thread-safe
        //final TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        //df.setTimeZone(tz);
        String formattedDate = df.format(date);
        return formattedDate;
        
        //TimeZone tz=TimeZone.getTimeZone("UTC");
        //return DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.format(date);
    }

}
