package org.genepattern.server.webapp.rest.api.v1.user;

/* ******************************************************************************
 * Copyright (c) 2003, 2019 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 * ******************************************************************************/

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Resource for querying the list of users based on the requested criteria.
 *
 * @author Thorin Tabor
 */
@Path("/v1/users")
public class UsersResource {
    private static final Logger log = Logger.getLogger(UsersResource.class);
    private static final long DAY_IN_MS = 1000 * 60 * 60 * 24;

    /**
     * Get the list of new user registrations between the start and end dates. 
     * Dates are in ISO 8601 format, {yyyy-MM-dd}T{HH:mm:ss}.
     * 
     * If no end date is provided, the query will use the present as the end date.
     * If no start date is provided, the query will assume one week ago.
     * 
     * Template:
     *   GET {gpUrl}/gp/rest/v1/users/new?start={startDate}&end={endDate}
     * Example:
     *   GET http://127.0.0.1:8080/gp/rest/v1/users/new?start=2018-01-01T23:00:00&end=2019-01-01T23:00:00
     *   
     * Example querys:
     *   ?start=2018&end=2019   (dates between Jan 1, 2018 and Jan 1, 2019)
     *   ?start=2018-01&end=2018-02
     *   
     */
    @GET
    @Path("/new")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeError(@Context HttpServletRequest request, @QueryParam("start") String startDate, @QueryParam("end") String endDate) {
        // Ensure that the user making the query has admin permissions
        GpContext userContext = Util.getUserContext(request);
        boolean admin = userContext.isAdmin();

        // If not admin, return an error
        if (!admin) {
            return Response.status(401).entity("Only admins can query the user database!").build();
        }
        
        // Parse and assign the start and end dates
        final Date endDateObj;
        final Date startDateObj;
        try {
            final Date now=new Date();
            endDateObj = parseEndDate(now, endDate);
            startDateObj = parseStartDate(now, startDate);
        }
        catch (IllegalArgumentException e) {
            return Response.status(400).entity("{'error': 'Error parsing date or end date'}").build();            
        }
        catch (Throwable t) {
            log.error("Unexpected error parsing dates", t);
            return Response.status(400).entity("{'error': 'Error parsing date or end date'}").build();            
        }

        // Query the database
        final List<User> users = getNewUsers(startDateObj, endDateObj);
        // Convert user list to JSON
        final JSONObject returnJSON;
        final String jsonStr;
        try {
            returnJSON=toUsersJson(users);
            int indentFactor=2;
            jsonStr=returnJSON.toString(indentFactor);
        }
        catch (Throwable t) {
            log.error("Error formatting JSON output", t);
            return Response.status(500)
                .entity("{'error': 'Could not build JSON object to return'}")
            .build();
        }

        // Return the user list object
        return Response.ok()
            .entity(jsonStr)
        .build();
    }

    protected static Date parseEndDate(final String endDate) {
        if (Strings.isNullOrEmpty(endDate)) {
            return Calendar.getInstance().getTime();
        }
        return DateUtil.parseDate(endDate);
    }

    protected static Date parseEndDate(final Date now, final String endDate) {
        // If no end date is provided, assume the present
        if (Strings.isNullOrEmpty(endDate)) {
            return now;
        }
        return DateUtil.parseDate(endDate);
    }

    protected static Date sevenDaysBefore(final Date now) {
        return new Date(now.getTime() - (7 * DAY_IN_MS));
    }

    protected static Date parseStartDate(final Date now, final String startDate) 
    {
        // If no start date is provided, assume a week ago
        if (Strings.isNullOrEmpty(startDate)) {
            return sevenDaysBefore(now);
        }
        return DateUtil.parseDate(startDate);
    }

    // Query the database
    protected static List<User> getNewUsers(final Date startDate, final Date endDate) {
        final boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            // Query the database
            final UserDAO dao = new UserDAO(HibernateUtil.instance());
            return dao.getNewUsers(startDate, endDate);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    // Convert user list to JSON
    protected static JSONObject toUsersJson(final List<User> users) 
    throws JSONException
    {
        JSONObject returnJSON = new JSONObject();
        JSONArray usersJSONArray = new JSONArray();

        for (final User u : users) {
            usersJSONArray.put(new UserJSON(u));
        }
        returnJSON.put("users", usersJSONArray);
        return returnJSON;
    }

    public static class UserJSON extends JSONObject {
        public UserJSON(User user) throws JSONException {
            this.put("username", user.getUserId());
            this.put("email", user.getEmail());
            this.put("domain", extractDomain(user.getEmail()));
            this.put("last_login", formatDate(user.getLastLoginDate()));
            this.put("registered", formatDate(user.getRegistrationDate()));
        }

        public static String formatDate(final Date date) {
            return DateUtil.toIso8601(date);
        }

        private String extractDomain(String email) {
            if (email == null) return "";
            String[] parts = email.split("@");
            return parts[parts.length-1];
        }
    }
}

