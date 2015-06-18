/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.log;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.rest.api.v1.Util;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Resource for logging from the client. This can be used to debug errors experienced by users.
 * Allows logging at levels: ERROR, WARN, DEBUG and INFO.
 *
 * @author Thorin Tabor
 */
@Path("/v1/log")
public class LogResource {
    final static private Logger log = Logger.getLogger(LogResource.class);

    @POST
    @Path("/error")
    public Response writeError(@Context HttpServletRequest request, String message) {
        GpContext userContext = Util.getUserContext(request);
        String user = userContext.getUserId();
        String agent = request.getHeader("User-Agent");
        log.error("CLIENT LOG (" + user + " - " + agent + ") " + message);
        return Response.ok().build();
    }

    @POST
    @Path("/warn")
    public Response writeWarning(@Context HttpServletRequest request, String message) {
        GpContext userContext = Util.getUserContext(request);
        String user = userContext.getUserId();
        String agent = request.getHeader("User-Agent");
        log.warn("CLIENT LOG (" + user + " - " + agent + ") " + message);
        return Response.ok().build();
    }

    @POST
    @Path("/debug")
    public Response writeDebug(@Context HttpServletRequest request, String message) {
        GpContext userContext = Util.getUserContext(request);
        String user = userContext.getUserId();
        String agent = request.getHeader("User-Agent");
        log.debug("CLIENT LOG (" + user + " - " + agent + ") " + message);
        return Response.ok().build();
    }

    @POST
    @Path("/info")
    public Response writeInfo(@Context HttpServletRequest request, String message) {
        GpContext userContext = Util.getUserContext(request);
        String user = userContext.getUserId();
        String agent = request.getHeader("User-Agent");
        log.info("CLIENT LOG (" + user + " - " + agent + ") " + message);
        return Response.ok().build();
    }
}
