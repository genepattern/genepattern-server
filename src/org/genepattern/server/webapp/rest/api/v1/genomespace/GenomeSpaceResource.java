/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.genomespace;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.genomespace.*;
import org.genepattern.server.webapp.rest.api.v1.Util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * REST implementation of the /genomespace resource.
 * @author Thorin Tabor
 */
@Path("/" + GenomeSpaceResource.URI_PATH)
public class GenomeSpaceResource {
    final static private Logger log = Logger.getLogger(GenomeSpaceResource.class);
    final static public String URI_PATH = "v1/genomespace";

    private boolean isGenomeSpaceEnabled(GpContext userContext) {
        return GenomeSpaceClientFactory.isGenomeSpaceEnabled(userContext);
    }

    public GenomeSpaceFile getFile(URL url, HttpSession httpSession) {
        Object gsSession = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        if (gsSession == null) {
            log.error("ERROR: Null gsSession found in GenomeSpaceBean.getFile()");
            gsSession = GenomeSpaceManager.forceGsSession(httpSession);
        }
        url = encodeURLIfNecessary(url);
        return GenomeSpaceFileHelper.createFile(gsSession, url);
    }

    public GenomeSpaceFile getFile(String url, HttpSession httpSession) {
        try {
            return getFile(new URL(url), httpSession);
        }
        catch (MalformedURLException e) {
            log.error("Error trying to get a URL object in getFile() for " + url);
            return null;
        }
    }

    /**
     * If the URL has spaces that need encoded, encode them and return
     * @param url
     * @return
     */
    private URL encodeURLIfNecessary(URL url) {
        // If this is true, encoding is not needed
        if (url.toString().indexOf(" ") < 0) {
            return url;
        }

        // Do the encoding here
        URI uri;
        try {
            uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), null);
            return uri.toURL();
        }
        catch (Exception e) {
            log.error("Error trying to encode a URL: " + url);
            return url;
        }
    }

    /**
     * Delete the specified GenomeSpace file
     * @param request
     * @param url - the GenomeSpace url for the file
     * @return
     */
    @DELETE
    @Path("/delete")
    public Response deleteFile(@Context HttpServletRequest request, @QueryParam("url") String url) {
        GpContext userContext = Util.getUserContext(request);

        if (!isGenomeSpaceEnabled(userContext)) {
            return Response.status(500).entity("GenomeSpace is not enabled").build();
        }

        try {
            GenomeSpaceFile file = getFile(url, request.getSession());
            Object gsSessionObject = request.getSession().getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);

            boolean success = GenomeSpaceClientFactory.instance().deleteFile(gsSessionObject, file);
            if (success) {
                return Response.ok().entity("Deleted from GenomeSpace " + file.getName()).build();
            }
            else {
                return Response.status(500).entity("Unable to delete in GenomeSpace " + file.getName()).build();
            }
        }
        catch (Exception e) {
            return Response.status(500).entity(e.getLocalizedMessage()).build();
        }
    }

    /**
     * Delete the specified GenomeSpace file
     * @param request
     * @param url - the GenomeSpace url for the file
     * @return
     */
    @GET
    @Path("/tool")
    public Response sendToTool(@Context HttpServletRequest request, @Context HttpServletResponse response, @QueryParam("tool") String tool, @QueryParam("url") String url) {
        GpContext userContext = Util.getUserContext(request);

        if (!isGenomeSpaceEnabled(userContext)) {
            return Response.status(500).entity("GenomeSpace is not enabled").build();
        }

        if (url == null || tool == null) {
            return Response.status(500).entity("Null value forwarding to the GenomeSpace tool URL: " + url + " " + tool).build();
        }

        Object gsSessionObject = request.getSession().getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);

        try {
            GenomeSpaceFile file = getFile(url, request.getSession());
            URL redirectUrl = GenomeSpaceClientFactory.instance().getSendToToolUrl(gsSessionObject, file, tool);
            URI redirectUri = new URI(redirectUrl.toString());
            //response.sendRedirect(redirectUrl.toString());
            return Response.temporaryRedirect(redirectUri).build(); //.ok().entity("Deleted from GenomeSpace " + file.getName()).build();
        }
        catch (Exception e) {
            return Response.status(500).entity("Error forwarding to the GenomeSpace tool URL: " + e.getMessage()).build();
        }
    }

    /**
     * Create the specified subdirectory
     * @param request
     * @param url - The URL to the parent directory
     * @param name - The name of the new subdirectory
     * @return
     */
    @PUT
    @Path("/createDirectory")
    public Response subdirectory(@Context HttpServletRequest request, @QueryParam("url") String url, @QueryParam("name") String name) {
        GpContext userContext = Util.getUserContext(request);

        if (!isGenomeSpaceEnabled(userContext)) {
            return Response.status(500).entity("GenomeSpace is not enabled").build();
        }

        if (url == null || name == null || name.length() == 0) {
            return Response.status(500).entity("Please enter a valid subdirectory name").build();
        }

        // Get the parent directory
        GenomeSpaceFile parentDir = getFile(url, request.getSession());
        Object gsSessionObject = request.getSession().getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);

        try {
            GenomeSpaceClientFactory.instance().createDirectory(gsSessionObject, name, parentDir);
            return Response.ok().entity("Created directory " + name).build();
        }
        catch (GenomeSpaceException e) {
            return Response.status(500).entity(e.getLocalizedMessage()).build();
        }
    }

    /**
     * Refresh the cached GenomeSpace file tree
     *
     * @return - A response whether the refresh was successful
     */
    @PUT
    @Path("/refresh")
    public Response refresh(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        GenomeSpaceManager.forceFileRefresh(session);
        return Response.ok().entity("GenomeSpace file cache has been reset.").build();
    }

    /**
     * Saves a file from GenePattern to GenomeSpace
     *
     * @param request - The HttpSessionRequest
     * @param fileUrl - GenePattern URL to the file
     * @param dirUrl - GenomeSpace URL to the save directory
     * @return - A response containing the URL of the GenomeSpace directory
     */
    @POST
    @Path("/save")
    public Response saveFile(@Context HttpServletRequest request, @QueryParam("file") String fileUrl, @QueryParam("directory") String dirUrl) {
        HttpSession httpSession = request.getSession();
        GenomeSpaceManager.setLoggedIn(httpSession, true);
        GenomeSpaceFile directory = GenomeSpaceManager.getDirectory(httpSession, dirUrl);
        GpFilePath file = null;
        try {
            file = GpFileObjFactory.getRequestedGpFileObj(fileUrl);
        } catch (Exception e) {
            log.error("Unable to locate GenePattern file when saving to GenomeSpace: " + fileUrl);
            Response.serverError().entity("Unable to find GenePattern File: " + fileUrl).build();
        }

        Object gsSession = httpSession.getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);
        try {
            GenomeSpaceClientFactory.instance().saveFileToGenomeSpace(gsSession, file, directory);
        } catch (GenomeSpaceException e) {
            log.error("Unable to save file to GenomeSpace: " + dirUrl);
            Response.serverError().entity("Unable to save file to GenomeSpace: " + dirUrl).build();
        }

        return Response.ok().entity(dirUrl).build();
    }
}
