package org.genepattern.server.webapp.rest.api.v1.disk;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.webapp.rest.api.v1.Util;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Created by nazaire on 7/10/14.
 */
@Path("/"+DiskResource.URI_PATH)
public class DiskResource
{
    final static private Logger log = Logger.getLogger(DiskResource.class);
    final static public String URI_PATH="v1/disk";

    /**
     * This is a method to get the disk usage and quota for a user
     * @param request
     * curl --user {userid} {GenePatternURL}/rest/v1/disk
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DiskInfo getDiskInfo(@Context HttpServletRequest request) throws Exception
    {
        GpContext userContext = Util.getUserContext(request);
        DiskInfo diskInfo = null;

        try
        {
            diskInfo = DiskInfo.createDiskInfo(ServerConfigurationFactory.instance(), userContext);
        }
        catch(DbException db)
        {
            log.error(db);
            throw db;
        }

        return diskInfo;
    }
}