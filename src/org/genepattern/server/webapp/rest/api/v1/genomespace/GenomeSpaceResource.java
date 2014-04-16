package org.genepattern.server.webapp.rest.api.v1.genomespace;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.JobInfoWrapper;
import org.genepattern.server.JobManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genomespace.*;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

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

        GenomeSpaceFile file = getFile(url, request.getSession());
        Object gsSessionObject = request.getSession().getAttribute(GenomeSpaceLoginManager.GS_SESSION_KEY);

        try {
            boolean success = GenomeSpaceClientFactory.getGenomeSpaceClient().deleteFile(gsSessionObject, file);
            if (success) {
                return Response.ok().entity("Deleted from GenomeSpace " + file.getName()).build();
            }
            else {
                return Response.status(500).entity("Unable to delete in GenomeSpace " + file.getName()).build();
            }
        }
        catch (GenomeSpaceException e) {
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

//    /**
//     * Create the specified subdirectory
//     * @param request
//     * @param path
//     * @return
//     */
//    @PUT
//    @Path("/createDirectory/{path:.+}")
//    public Response subdirectory(@Context HttpServletRequest request, @PathParam("path") String path) {
//        // Fix for when the preceding slash is missing from the path
//        if (!path.startsWith("/")) {
//            path = "/" + path;
//        }
//
//        try {
//            GpContext userContext=Util.getUserContext(request);
//            File relativePath = null;//extractUsersPath(userContext, path);
//            if (relativePath == null) {
//                //error
//                return Response.status(500).entity("Could not createDirectory: " + path).build();
//            }
//
//            // Create the directory
//            boolean success = DataManager.createSubdirectory(userContext, relativePath);
//
//            if (success) {
//                return Response.ok().entity("Created " + relativePath.getName()).build();
//            }
//            else {
//                return Response.status(500).entity("Could not create " + relativePath.getName()).build();
//            }
//        }
//        catch (Throwable t) {
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
//        }
//    }
}
