package org.genepattern.server.webapp.rest.api.v1.usagestats;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.dao.UsageStatsDAO;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.httpclient.util.DateUtil;




import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Resource for obtaining config on the client.
 *
 *  test like so: 
 *  
 *         curl --user ted: http://127.0.0.1:8180/gp/rest/v1/usagestats/user_summary/2018-04-14/2018-04-19
 *         
 *  more gernerally
 *  
 *         curl --user <gpAdminUsername>: <genepattern-URL>/gp/rest/v1/usagestats/user_summary/YYYY-MM-DD/YYYY-MM-DD
 *
 *  where the first date is the start date and the second is the end date
 *  
 * @author Ted Liefeld
 */
@Path("/v1/usagestats")
public class UsageStatsResource {

    final static private Logger log = Logger.getLogger(UsageStatsResource.class);

    public static final Collection<String> DEFAULT_DATE_FORMATS = new ArrayList<String>();

    static {
      DEFAULT_DATE_FORMATS.add("yyyy-MM-dd");
    }
    
    
    /**
     * Get a JSON object representing summary data about the users from 12:00am on the start date to 11:59 pm on
     * the end date
     *
     **/
    @GET
    @Path("/user_summary/{startDate}/{endDate}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response userSummary(@Context HttpServletRequest request,@PathParam("startDate") String startDay, @PathParam("endDate") String endDay) {
        GpContext userContext = Util.getUserContext(request);
        if (! userContext.isAdmin()) {
            return Response.status(403).entity("Forbidden: User"+userContext.getUserId() + " is not authorized to access the summary usage data.Must be administrator").build();
        }
        // This is used for deciding if jobs are internal or external
        String internalDomain =  ServerConfigurationFactory.instance().getGPProperty(userContext, "internalDomainForStats", "broadinstitute.org");
        
        JSONObject object = new JSONObject();
        try {
            Date startDate = null, endDate=null;
            try {
                //Just for validation
                startDate = DateUtil.parseDate(startDay, DEFAULT_DATE_FORMATS);
                endDate = DateUtil.parseDate(endDay, DEFAULT_DATE_FORMATS);         
            } catch (Exception e) {
                // Does not look like a valid date
                e.printStackTrace();
                object.put("Error", e.getMessage());
                return Response.ok().entity(object.toString()).build();

            }
            object.put("ReportPeriodStart", startDay);
            object.put("ReportPeriodEnd", endDay);
            
            final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
            UsageStatsDAO ds = new UsageStatsDAO(mgr);
            String excludedUsers = getUserExclusionClause(userContext, ds);
            System.out.println("D " + excludedUsers);
            
            try {            
                object.put("NewUserRegistrations", ds.getRegistrationCountBetweenDates(startDate, endDate, excludedUsers));
                object.put("TotalUsersCount", ds.getTotalRegistrationCount(excludedUsers));
                object.put("ReturningUsersCount",ds.getReturnLoginCountBetweenDates(startDate, endDate, excludedUsers));
                object.put("NewUsersCount",ds.getReturnLoginCountBetweenDates(startDate, endDate, excludedUsers));
                object.put("TotalJobs",ds.getTotalJobsRunCount(excludedUsers));
                object.put("JobsRun",ds.getJobsRunCountBetweenDates(startDate, endDate, excludedUsers));
                object.put("InternalJobsRun",ds.getInternalJobsRunCountBetweenDates(startDate, endDate, excludedUsers, internalDomain));
                object.put("ExternalJobsRun",ds.getExternalJobsRunCountBetweenDates(startDate, endDate, excludedUsers, internalDomain));
                object.put("NewUsers",ds.getUserRegistrationsBetweenDates(startDate, endDate, excludedUsers));
                object.put("ModuleRunCounts",ds.getModuleRunCountsBetweenDates(startDate, endDate, excludedUsers));
                object.put("ModuleErrorCounts",ds.getModuleErrorCountsBetweenDates(startDate, endDate, excludedUsers));
                object.put("UserRunCounts",ds.getUserRunCountsBetweenDates(startDate, endDate, excludedUsers));
                object.put("DomainRunCounts",ds.getModuleRunCountsBetweenDatesByDomain(startDate, endDate, excludedUsers));
                object.put("ModuleErrors",ds.getModuleErrorsBetweenDates(startDate, endDate, excludedUsers));
                
            } catch (Exception e){
                e.printStackTrace();
                object.put("Error", e.getMessage());
            }
           
            
            
        } catch (JSONException e) {
            log.error("Error producing JSON object for UsageStatsResource.user_summary()");
        }
        return Response.ok().entity(object.toString()).build();
    }
    
    
    /**
     * Get the list of users who's actions are not included or reported in these reports.
     * This is set as a string list in the custom properties file and we'll ad the quotes 
     * and commas here
     * @param userContext
     * @return
     */
    private String getUserExclusionClause( GpContext userContext, UsageStatsDAO ds){
        StringBuffer buff = new StringBuffer("  (  ");
        String propValue = ServerConfigurationFactory.instance().getGPProperty(userContext, "excludeUsersFromStats", null);
        if (propValue == null) return "";
        
        String[] user_ids = propValue.split("\\s+");
        for (String user : user_ids) {
            buff.append("\'");
            buff.append(user);
            buff.append("\', "); 
        }
        buff.append(" \'admin\') ");  // default to except
        
        // hand this off to the dao to get the user_ids of any users that match case insensitive on username or email
        return ds.generateUserExclusionClause(buff.toString());
      
    }
    
    /**
     * Additional stats needed
     * 
     *  
     * 
     * - list of new user registrations  -- user_id, email
     * - jobs run by domain - count for jobs summed over the email domains of the users
     * - module job run counts- ordered by number, ordered by name alphabetically
     * - jobs run by user, username, email, module name, count  - ordered by count
     * - module error counts - ordered by number
     * - module error details - link to job page, link to stderr.txt, status (finished or error)
     * 
     * - auto exclude any users with @example.com
     * update queries to add USER_ID not in (Barbara Hill BobNonAdmin BobTest TestUserBob bhill 
     * bhill@broad.mit.edu bhilltest bobbie bistline bistline@broad.mit.edu jtb clewis 
     * dkjang admin GRRDtest GPDemo helga helgatest helgath ht_test hth hthorv gpuser 
     * heidi heidialso hkuehn jgould jgould2 jgould@broad.mit.edu jnedzel@broad.mit.edu jrobinso 
     * jrobinso@broad.mit.edu ted tedder tedtest mmreich mmr mwrobel@broad.mit.edu mnazaire nazaire 
     * nazaire@broad.mit.edu njendu pcarr pcarr@broad.mit.edu pedro test qgao qgao@broad.mit.edu reid jntest)
     * 
     * 
     */
    
   
    
    
}
