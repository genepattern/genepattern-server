package org.genepattern.server.webapp.rest.api.v1;

import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Util {
    private static final Logger log = Logger.getLogger(Util.class);

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
    
//    /**
//     * The default ISO 8601 date formatter returned when getting JSON representations
//     * of jobs and result files.
//     * Using Joda Time without the milliseconds
//     * <pre>
//       yyyy-MM-dd'T'HH:mm:ssZZ
//     * </pre>
//     */
//    public static final DateTimeFormatter isoNoMillis=DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
//    public static final DateTimeFormatter isoNoMillisUtc=DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();
//    
//    /**
//     * Outputs the date in 'ECMAScript 5 ISO-8601' format, in the server's timezone.
//     * 
//     * @param date
//     * @return
//     */
//    public static String toIso8601(final Date date) {
//        return toIso8601(date, null);
//    }
//
//    /**
//     * 
//     * @param date
//     * @param TimeZone
//     * @return
//     */
//    public static String toIso8601(final Date date, final TimeZone tz) {
//        if (date==null) {
//            log.debug("Unexpected null date");
//            return "";
//        }
//        if (tz==null) {
//            return isoNoMillis.print(date.getTime());
//        }
//        DateTimeZone jodaTz=DateTimeZone.forTimeZone(tz);
//        if (jodaTz.getOffset(date.getTime())==0L) {
//            return isoNoMillisUtc.withZone(jodaTz).print(date.getTime());
//        }
//        else {
//            return isoNoMillis.withZone(jodaTz)
//                .print(date.getTime());
//        }
//    }

}
