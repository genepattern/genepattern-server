package org.genepattern.server.webapp.rest.api.v1.user;

/* ******************************************************************************
 * Copyright (c) 2003, 2017 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 * ******************************************************************************/

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    final static private Logger log = Logger.getLogger(UsersResource.class);
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Get the list of new users that registered between the specified start date and end date (in yyyy-MM-dd HH:mm:ss format).
     *
     * If no end date is provided, the query will use the present as the end date.
     * If no start date is provided, the query will assume one week ago.
     *
     * @param request
     * @param startDate
     * @param endDate
     * @return
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
        Date endDateObj = null;
        Date startDateObj = null;

        try {
            // If no end date is provided, assume the present
            if (endDate == null) {
                endDateObj = Calendar.getInstance().getTime();
            }
            else { // Otherwise, parse the date string
                endDateObj = dateFormat.parse(endDate);
            }

            // If no start date is provided, assume a week ago
            if (startDate == null) {
                long DAY_IN_MS = 1000 * 60 * 60 * 24;
                startDateObj = new Date(System.currentTimeMillis() - (7 * DAY_IN_MS));
            }
            else { // Otherwise, parse the date string
                startDateObj = dateFormat.parse(startDate);
            }
        }
        catch (ParseException e) {
            return Response.status(400).entity("{'error': 'Error parsing date or end date'}").build();
        }

        // Query the database
        UserDAO dao = new UserDAO();
        List<User> users = dao.getNewUsers(startDateObj, endDateObj);

        // Convert user list to JSON
        JSONObject returnJSON = new JSONObject();
        JSONArray usersJSONArray = new JSONArray();

        try {
            for (User u : users) usersJSONArray.put(new UserJSON(u));
            returnJSON.put("users", usersJSONArray);
        }
        catch (JSONException e) {
            return Response.status(500).entity("{'error': 'Could not build JSON object to return'}").build();
        }

        // Return the user list object
        return Response.ok().entity(returnJSON.toString()).build();
    }

    public class UserJSON extends JSONObject {
        public UserJSON(User user) throws JSONException {
            this.put("username", user.getUserId());
            this.put("email", user.getEmail());
            this.put("domain", extractDomain(user.getEmail()));
            this.put("last_login", formatDate(user.getLastLoginDate()));
            this.put("registered", formatDate(user.getRegistrationDate()));
        }

        private String formatDate(Date date) {
            if (date == null) return "";
            return dateFormat.format(date);
        }

        private String extractDomain(String email) {
            if (email == null) return "";
            String[] parts = email.split("@");
            return parts[parts.length-1];
        }
    }
}

