/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.data.pipeline;

import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.genepattern.server.TaskIDNotFoundException;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Utility methods for pipeline validation and execution.
 * Moved some methods out of RunPipelineForJsp because we no longer use RunPipelineForJsp.
 * 
 * @author pcarr
 */
public class PipelineUtil {
    public static Logger log = Logger.getLogger(PipelineUtil.class);
    
    /**
     * Get the PipelineModel for the given lsid.
     * @deprecated should pass in a Hibernate session
     */
    public static PipelineModel getPipelineModel(final String lsid) 
    throws TaskIDNotFoundException, PipelineModelException
    {
        TaskInfo taskInfo = TaskInfoCache.instance().getTask(lsid);
        return getPipelineModel(taskInfo);
    }

    /**  @deprecated should pass in a Hibernate session */
    public static PipelineModel getPipelineModel(final TaskInfo taskInfo) 
    throws TaskIDNotFoundException, PipelineModelException
    {
        return getPipelineModel(HibernateUtil.instance(), taskInfo);
    }

    public static PipelineModel getPipelineModel(final HibernateSessionManager mgr, final TaskInfo taskInfo) 
    throws TaskIDNotFoundException, PipelineModelException
    {
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfo is null");
        }
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        if (tia == null) {
            throw new PipelineModelException("taskInfo.giveTaskInfoAttributes is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        if (serializedModel == null || serializedModel.length() == 0) {
            throw new PipelineModelException("Missing "+GPConstants.SERIALIZED_MODEL+" for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        final PipelineModel model = getPipelineModelFromSerializedModel(mgr, serializedModel);
        if (model == null) {
            throw new PipelineModelException("pipeline model is null for taskInfo.ID="+taskInfo.getID()+", taskInfo.name="+taskInfo.getName());
        }
        model.setLsid(taskInfo.getLsid());
        return model;
    }

    /**
     * Helper method for de-serializing the pipeline model with proper DB connection handling.
     * @param mgr
     * @param serializedModel
     * @return
     * @throws PipelineModelException
     */
    public static PipelineModel getPipelineModelFromSerializedModel(final HibernateSessionManager mgr, final String serializedModel) throws PipelineModelException {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            return PipelineModel.toPipelineModel(serializedModel);
        } 
        catch (Throwable t) {
            log.error(t);
            mgr.closeCurrentSession();
            throw new PipelineModelException(t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * Checks the given pipeline to see if all tasks that it uses are installed
     * on the server. Writes an error message to the given
     * <code>PrintWriter</code> if there are missing tasks.
     *
     * @param model  the pipeline model
     * @param out    the writer
     * @param userID the user id
     * @return <code>true</code> if the pipeline is missing tasks
     */
    public static boolean isMissingTasks(final PipelineModel model, final java.io.PrintWriter out, final String userID) throws Exception {
        LinkedHashMap<LSID, MissingTaskRecord> missingTasks = getMissingTasks(model, userID);
        boolean isMissingTasks = missingTasks.size() > 0;
        if (isMissingTasks && out != null) {
            out.println(
            "<font color='red' size=\"+1\"><b>Warning:</b></font><br>The following module versions do not exist on this server. Before running this pipeline you will need to edit the pipeline to use the available module version or install the required modules.");
            out.println("<table width='100%'  border='1'>");
            out.println(
            "<tr class=\"paleBackground\" ><td> Name </td><td> Required Version</td><td> Installed Version</td><td>LSID</td></tr>");
            out.println("<form method=\"post\" action=\"pages/taskCatalog.jsf\">");

            for(LSID missingLsid : missingTasks.keySet()) {
                String taskName = missingTasks.get(missingLsid).getName();
                SortedSet<LSID> installedVersions = missingTasks.get(missingLsid).getInstalledVersions();
                
                out.println("<input type=\"hidden\" name=\"lsid\" value=\"" + missingLsid + "\" /> ");
                out.println("<tr><td>" + taskName + "</td><td>" + missingLsid.getVersion() + "</td><td>");
                
                //insert installed versions
                boolean first = true;
                for(LSID installedVersion : installedVersions) {
                    if (first) {
                        first = false;
                    }
                    else {
                        out.print(", ");
                    }
                    out.print(""+installedVersion.getVersion());
                }
                out.println("</td><td> " +
                        missingLsid.toStringNoVersion() + "</td></tr>");
            }

            out.println("<tr class=\"paleBackground\" >");
            out.println("<td colspan='4' align='center' border = 'none'> <a href='pages/importTask.jsf'>Install from zip file </a>");
            out.println(" &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");
            out.println("<input type=\"hidden\" name=\"checkAll\" value=\"1\"  >");
            out.println("<input type=\"submit\" value=\"Install from repository\"  ></td></form>");
            out.println("</tr>");
            out.println("</table>");
        }
        return isMissingTasks;
    }

    
    /**  @deprecated should pass in a Hibernate session */
    public static LinkedHashMap<LSID, MissingTaskRecord> getMissingTasks(final PipelineModel model, final String userId) {
        return getMissingTasks(HibernateUtil.instance(), model, userId);
    }
    
    public static LinkedHashMap<LSID, MissingTaskRecord> getMissingTasks(final HibernateSessionManager mgr, final PipelineModel model, final String userId) {
        final List<JobSubmission> jobSubmissionTasks = model.getTasks();
        final LinkedHashMap<LSID, MissingTaskRecord> missingTasks = new LinkedHashMap<LSID, MissingTaskRecord>();
        for(final JobSubmission jobSubmission : jobSubmissionTasks) {
            TaskInfo taskInfo = null;
            try {
                taskInfo = TaskInfoCache.instance().getTask(mgr, jobSubmission.getLSID());
            }
            catch (TaskLSIDNotFoundException e) {
            }
            if (taskInfo == null) {
                try {
                    LSID missingLsid = new LSID(jobSubmission.getLSID());
                    if (!missingTasks.containsKey(missingLsid)) {
                        MissingTaskRecord missingTaskRecord = new MissingTaskRecord(jobSubmission.getName(), missingLsid);
                        missingTasks.put(missingLsid, missingTaskRecord);
                    }
                }
                catch (MalformedURLException e) {
                    log.error("Invalid lsid="+jobSubmission.getLSID(), e);
                }
            }
        }
        return missingTasks;
    }
    
    public static class MissingTaskRecord {
        String name = null;
        LSID lsid = null;
        SortedSet<LSID> installedVersions = null;
        
        public MissingTaskRecord(String name, LSID lsid) {
            this.name = name;
            this.lsid = lsid;
            installedVersions = getInstalledVersions(lsid);
        }
        
        public String getName() {
            return name;
        }

        public SortedSet<LSID> getInstalledVersions() {
            return installedVersions;
        }

        private static SortedSet<LSID> getInstalledVersions(LSID lsid) {
            SortedSet<LSID> versions = new TreeSet<LSID>();
            
            try {
            String lsidNoVersion = lsid.toStringNoVersion();
            
            String hql = "select lsid from org.genepattern.server.domain.TaskMaster where lsid like :lsid";
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setString("lsid", lsidNoVersion+"%");
            @SuppressWarnings("unchecked")
            List<String> lsidResults = query.list();
            
            for(String lsidResult : lsidResults) {
                try {
                    LSID sLSID = new LSID(lsidResult);
                    //String version = sLSID.getVersion();
                    versions.add(sLSID);
                }
                catch (MalformedURLException e) {
                    log.error("Invalid lsid version for lsidNoVersion="+lsidNoVersion+", lsid="+lsid, e);
                }
            } 
            }
            catch (Throwable t) {
                log.error("Error in getInstalledVersions, lsid="+lsid, t);
            }
            return versions;
        }
    }

    public static boolean isMissingTasks(PipelineModel model, String userID) {
        try {
            int numMissingTasks = getMissingTasks(model,userID).size();
            if (numMissingTasks > 0) {
                return true;
            }
            return false;
        }
        catch(OmnigeneException e) {
            return true;
        }
    }

}
