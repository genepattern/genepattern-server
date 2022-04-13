package org.genepattern.server.webapp.rest.api.v1.usagestats;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.dao.UsageStatsDAO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.httpclient.util.DateUtil;




import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import java.util.concurrent.Executor;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ManagedAsync;


/**
 * Resource for obtaining config on the client.
 *
 *  test like so: 
 *  
 *         curl --user ted: http://127.0.0.1:8180/gp/rest/v1/usagestats/user_summary/2018-04-14/2018-04-19
 *         
 *  more generally
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
    public Response userSummary(@Context final HttpServletRequest request,@PathParam("startDate") final String startDay, @PathParam("endDate") final String endDay) {
        final GpContext userContext = Util.getUserContext(request);
        if (! userContext.isAdmin()) {
            return Response.status(403).entity("Forbidden: User "+userContext.getUserId() + " is not authorized to access the summary usage data.Must be administrator.\n").build();
        }
        // This is used for deciding if jobs are internal or external
        final String internalDomain =  ServerConfigurationFactory.instance().getGPProperty(userContext, "internalDomainForStats", "broadinstitute.org");
        final long startTime = System.currentTimeMillis();
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                Writer writer = new BufferedWriter(new OutputStreamWriter(os));
                
                // write an empty bite so that the client connection gets a response.  Then it will keep-alive
                // by default.  Without this it can timeout while waiting.  We will repeat this a few times
                // before we get to the end of the queries and start writing for real
                writer.write(" ");
                writer.flush();
                
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
                    writer.write(object.toString());
                    writer.flush();
                    return;
                }
                object.put("ReportPeriodStart", startDay);
                object.put("ReportPeriodEnd", endDay);
                
                final HibernateSessionManager mgr = org.genepattern.server.database.HibernateUtil.instance();
                UsageStatsDAO ds = new UsageStatsDAO(mgr);
                JSONArray errors = new JSONArray();
                JSONArray executionTime = new JSONArray();
                
                String excludedUsers = getUserExclusionClause(userContext, ds);
                
                System.out.println("Excluded users: " + excludedUsers);
                
                writer.write(" ");
                writer.flush();
                try {            
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("NewUserRegistrations", ds.getRegistrationCountBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("NewUserRegistrations et (ms): " + (t2-t1));
                        System.out.println("NewUserRegistrations et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("TotalUsersCount", ds.getTotalRegistrationCount(excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("TotalUsersCount et (ms): " + (t2-t1));
                        System.out.println("TotalUsersCount et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("ReturningUsersCount",ds.getReturnLoginCountBetweenDates(startDate, endDate, excludedUsers));      
                        long t2 = System.currentTimeMillis();
                        executionTime.put("ReturningUsersCount et (ms): " + (t2-t1));
                        System.out.println("ReturningUsersCount et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        
                        object.put("NewUsersCount",ds.getReturnLoginCountBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("NewUsersCount et (ms): " + (t2-t1));
                        System.out.println("NewUsersCount et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("TotalJobs",ds.getTotalJobsRunCount(excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("TotalJobs et (ms): " + (t2-t1));
                        System.out.println("TotalJobs et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("JobsRun",ds.getJobsRunCountBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("JobsRun et (ms): " + (t2-t1));
                        System.out.println("JobsRun et (ms): " + (t2-t1));
                   } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("JobsCpu",ds.getTotalJobsCPUBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("totalCPU et (ms): " + (t2-t1));
                        System.out.println("totalCPU et (ms): " + (t2-t1));
                   } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    
                    
                    
                    
                    try {
                        long t1 = System.currentTimeMillis();
                        
                        object.put("InternalJobsRun",ds.getInternalJobsRunCountBetweenDates(startDate, endDate, excludedUsers, internalDomain));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("InternalJobsRun et (ms): " + (t2-t1));
                        System.out.println("InternalJobsRun et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("ExternalJobsRun",ds.getExternalJobsRunCountBetweenDates(startDate, endDate, excludedUsers, internalDomain));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("ExternalJobsRun et (ms): " + (t2-t1));
                        System.out.println("ExternalJobsRun et (ms): " + (t2-t1));
                     } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("NewUsers",ds.getUserRegistrationsBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("NewUsers et (ms): " + (t2-t1));
                        System.out.println("NewUsers et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("ModuleInstalls",ds.getModuleInstallsBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("ModuleInstalls et (ms): " + (t2-t1));
                        System.out.println("ModuleInstalls et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    
                    
                    
                    try {
                         long t1 = System.currentTimeMillis();
                         object.put("ModuleRunCounts",ds.getModuleRunCountsBetweenDates(startDate, endDate, excludedUsers));
                         long t2 = System.currentTimeMillis();
                         executionTime.put("ModuleRunCounts et (ms): " + (t2-t1));
                         System.out.println("ModuleRunCounts et (ms): " + (t2-t1));
                       } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("ModuleErrorCounts",ds.getModuleErrorCountsBetweenDates(startDate, endDate, excludedUsers));
                        
                        long t2 = System.currentTimeMillis();
                        executionTime.put("ModuleErrorCounts et (ms): " + (t2-t1));
                        System.out.println("ModuleErrorCounts et (ms): " + (t2-t1));
                     } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("UserRunCounts",ds.getUserRunCountsBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("UserRunCounts et (ms): " + (t2-t1));
                        System.out.println("UserRunCounts et (ms): " + (t2-t1));
                  } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("DomainRunCounts",ds.getModuleRunCountsBetweenDatesByDomain(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("DomainRunCounts et (ms): " + (t2-t1));
                        System.out.println("DomainRunCounts et (ms): " + (t2-t1));
                      
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    try {
                        long t1 = System.currentTimeMillis();
                        object.put("ModuleErrors",ds.getModuleErrorsBetweenDates(startDate, endDate, excludedUsers));
                        long t2 = System.currentTimeMillis();
                        executionTime.put("ModuleErrors et (ms): " + (t2-t1));
                        System.out.println("ModuleErrors et (ms): " + (t2-t1));
                    } catch (Exception e){
                        e.printStackTrace();
                        errors.put(e.getMessage());
                    }
                    writer.write(" ");
                    writer.flush();
                    object.put("ReportGenerationErrors", errors);
                    object.put("ReportGenerationTimings", executionTime);
                } catch (Exception e){
                    e.printStackTrace();
                    writer.write(e.getMessage());
                }
               
                
                
            } catch (JSONException e) {
                e.printStackTrace();
                writer.write(e.getMessage());
                log.error("Error producing JSON object for UsageStatsResource.user_summary()");
            }
            long t3 = System.currentTimeMillis();
            System.out.println("Queries done after: " + ((t3-startTime)/1000)+ "sec");
            
            writer.write(object.toString());
            
            long endTime = System.currentTimeMillis();
            System.out.println("builder took: " + ((endTime-t3)/1000)+ "sec");
            System.out.println("All done after: " + ((endTime-startTime)/1000)+ "sec");
            
            writer.flush();
            }
        };
        
        Response jaxrs = Response.ok(stream).type(MediaType.TEXT_PLAIN).build();
       
        return jaxrs;
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
        String matchingEmailPattern = ServerConfigurationFactory.instance().getGPProperty(userContext, "excludeUsersFromStatsEmailPattern", null);
        
        if ((propValue == null)  && (matchingEmailPattern == null)) return "";
        
        String[] user_ids = propValue.split("\\s+");
        for (String user : user_ids) {
            buff.append("\'");
            buff.append(user);
            buff.append("\', "); 
        }
        buff.append(" \'admin\') ");  // default to except
        
        String pattern = null;
        if (matchingEmailPattern != null){
            StringBuffer patternBuff = new StringBuffer("    ");
            
            patternBuff.append("\'");
            patternBuff.append(matchingEmailPattern);
            patternBuff.append("\' "); 
      
            pattern = patternBuff.toString();
        }
        
        
        
        // hand this off to the dao to get the user_ids of any users that match case insensitive on username or email
        return ds.generateUserExclusionClause(buff.toString(), pattern);
      
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
