/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class Util {
    private static final Logger log = Logger.getLogger(Util.class);

    /**
     * Create a new userContext instance based on the current HTTP request.
     * This method has the effect of requiring a valid logged in gp user, because a 
     * RuntimeException will be thrown if the user is not logged in.
     * 
     * @param request
     * @return
     * @throws WebApplicationException if there is not a current user.
     */
    public static GpContext getUserContext(final HttpServletRequest request) {
        final String userId=(String) request.getSession().getAttribute("userid");
        if (userId==null || userId.length()==0) {
            //user not logged in, 403 - Forbidden
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        final boolean initIsAdmin=true;
        GpContext userContext = GpContext.getContextForUser(userId, initIsAdmin);
        return userContext;
    }
        
    public static GpContext getTaskContext(
            final HttpServletRequest request,
            final String taskNameOrLsid) 
    throws WebServiceException
    {
        GpContext taskContext=getUserContext( request );
        TaskInfo taskInfo = getTaskInfo( taskNameOrLsid, taskContext.getUserId() );
        taskContext.setTaskInfo(taskInfo);
        return taskContext;
    }

    private static TaskInfo getTaskInfo(final String taskLSID, final String username) 
    throws WebServiceException 
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }

    /**
     * Initialize a jobContext from the given request and jobId.
     * Throws WebApplicationException for the following scenarios:
     *     403, FORBIDDEN, if there is not an authenticated user in the session,
     *     404, NOT FOUND, if the jobId is not properly formatted or otherwise not in the database,
     *     403, FORBIDDEN, if the current authenticated user does not have permission to view the job
     *     
     * @param request
     * @param jobId
     * @return
     */
    public static GpContext getJobContext(final HttpServletRequest request, final String jobId) { 
        return doInTransaction(new Callable<GpContext> () {
            @Override
            public GpContext call() throws Exception { 

                final String userId=(String) request.getSession().getAttribute("userid");
                if (userId==null || userId.length()==0) {
                    log.debug("user not logged in, 403 - Forbidden");
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                }

                final boolean isAdmin = AuthorizationHelper.adminServer(userId);

                GpContext.Builder b=new GpContext.Builder();
                b.userId(userId);
                b.isAdmin(isAdmin);

                // validate job id
                if (jobId==null) {
                    throw new IllegalArgumentException("jobId==null");
                }
                final int jobNumber;
                try {
                    jobNumber=Integer.parseInt(jobId);
                }
                catch (Throwable t) {
                    String message="Incorrectly formatted jobId="+jobId;
                    log.debug(message);
                    throw new WebApplicationException(
                            Response.status(Response.Status.NOT_FOUND).entity(message).build());
                }

                //check jobInfo
                JobInfo jobInfo=null;
                try {
                    jobInfo = new AnalysisDAO().getJobInfo(jobNumber);
                }
                catch (Throwable t) {
                    log.debug("Error initializing jobInfo from db, jobNumber="+jobNumber, t);
                }
                if (jobInfo==null) {
                    final String message="No job with jobId="+jobId;
                    log.debug(message);
                    throw new WebApplicationException(
                            Response.status(Response.Status.NOT_FOUND).entity(message).build());
                }

                // check permissions
                PermissionsHelper perm = new PermissionsHelper(isAdmin, userId, jobNumber);
                if (!perm.canReadJob()) { 
                    final String message="User does not have permission to view job";
                    log.debug(message);
                    throw new WebApplicationException(
                            Response.status(Response.Status.FORBIDDEN).entity(message).build());
                }

                b.jobInfo(jobInfo);
                return b.build();
            }
        });
    }
    
    private static GpContext doInTransaction(Callable<GpContext> c) {
        boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            return c.call();
        }
        catch (WebApplicationException w) {
            throw w;
        }
        catch (Throwable t) {
            log.error(t);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
