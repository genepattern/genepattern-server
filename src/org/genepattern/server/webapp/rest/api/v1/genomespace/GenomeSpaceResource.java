package org.genepattern.server.webapp.rest.api.v1.genomespace;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
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
            gsSession = GenomeSpaceBean.forceGsSession(httpSession);
        }
        url = encodeURLIfNecessary(url);
        return GenomeSpaceFileManager.createFile(gsSession, url);
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

            boolean success = GenomeSpaceClientFactory.getGenomeSpaceClient().deleteFile(gsSessionObject, file);
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
            URL redirectUrl = GenomeSpaceClientFactory.getGenomeSpaceClient().getSendToToolUrl(gsSessionObject, file, tool);
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
            GenomeSpaceClientFactory.getGenomeSpaceClient().createDirectory(gsSessionObject, name, parentDir);
            return Response.ok().entity("Created directory " + name).build();
        }
        catch (GenomeSpaceException e) {
            return Response.status(500).entity(e.getLocalizedMessage()).build();
        }
    }
}
