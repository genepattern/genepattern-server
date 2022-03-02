package org.genepattern.server.webapp.rest.api.v1.usagestats;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;


/**
 * A class and REST resource that can be used for arbitrary usage logging.  Since we
 * just want a really general thing that goes to a  log file that we can scan to 
 * pull out details. Its just a very generic api to allow things to be 
 * written to a log or logs
 * 
 * @author liefeld
 *
 */

@Path("/v1/usagelogging")
public class UsageLoggingResource {

    //final static private Logger log = Logger.getLogger(UsageLoggingResource.class);

    @GET
    @Path("/log")
    @Produces(MediaType.APPLICATION_JSON)
    public Response userSummary(@Context final HttpServletRequest request,@QueryParam("logname") final String logname, @QueryParam("loglevel") final String loglevel, @QueryParam("message") final List<String> messageList) {
        log(logname, loglevel, messageList);
        return Response.status(200).build();
    }

    public static void log(final String logname, final String loglevel, final List<String> messageList) {
        Logger log = Logger.getLogger("org.genepattern.server.webapp.rest.api.v1.usagestats."+logname);
        
        StringBuilder msg = new StringBuilder();
        for (int i=0; i < messageList.size(); i++){
            msg.append(messageList.get(i));
            if (i != (messageList.size()-1)) msg.append("\t");
        }
        String message = msg.toString();
         
        if ("trace".equalsIgnoreCase(loglevel)){
            log.trace(message);
        } else if ("info".equalsIgnoreCase(loglevel)){
            log.info(message);
        } else if ("error".equalsIgnoreCase(loglevel)){
            log.error(message);
        } else if ("debug".equalsIgnoreCase(loglevel)){
            log.debug(message);
        } else {
            // default to trace
            log.trace(message);
        }
    }
}
