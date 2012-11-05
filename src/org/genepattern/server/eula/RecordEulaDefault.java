package org.genepattern.server.eula;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.PostToBroad;
import org.genepattern.server.eula.remote.RecordEulaToRemoteServerAsync;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;


/**
 * The default method for recording EULA in the local GP server.
 * 
 * It saves a local record, and adds an entry to the queue, for remote record.
 * This is done in a single transaction, so that if we are not able to add the record to the remote queue,
 * the entire transaction will fail.
 * 
 * Note: the actual remote POST is done asynchronously in a different thread.
 * Note: the following configuration properties control remote POST
 *     # if there is no setting, use the default value, compiled in the source code
 *     # set the remoteUrl
 *     org.genepattern.server.eula.EulaManager.remoteUrl: http://vgpweb01.broadinstitute.org:3000/eulas
 *     # if the remoteUrl is an empty list, it means don't POST
 *     org.genepattern.server.eula.EulaManager.remoteUrl: []
 *     # can be a list, which means post to more than one remote URL
 *     org.genepattern.server.eula.EulaManager.remoteUrl: [ http://vgpweb01.broadinstitute.org:3000/eulas, <other> ]     
 * 
 * @author pcarr
 *
 */
public class RecordEulaDefault implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaDefault.class);
    final static public String PROP_REMOTE_URL="org.genepattern.server.eula.EulaManager.remoteUrl";
    
    private RecordEulaToDb local;
    private RecordEulaToRemoteServerAsync remote;
    
    public RecordEulaDefault() {
        local=new RecordEulaToDb();
        remote=new RecordEulaToRemoteServerAsync();
    }
    
    private Context getContextForEula(final String userId, final EulaInfo eula) {
        Context eulaContext=ServerConfiguration.Context.getContextForUser(userId);
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.giveTaskInfoAttributes().put(GPConstants.LSID, eula.getModuleLsid());
        taskInfo.setName(eula.getModuleName());
        eulaContext.setTaskInfo(taskInfo);
        return eulaContext;
    }

    private List<String> getRemoteUrls(final String userId, final EulaInfo eula) {
        Context eulaContext=getContextForEula(userId, eula);
        Value val=ServerConfiguration.instance().getValue(eulaContext, PROP_REMOTE_URL);
        if (val==null) {
            List<String> rval=new ArrayList<String>();
            rval.add(PostToBroad.DEFAULT_URL);
            return rval;
        }
        return val.getValues();
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("recordLicenseAgreement("+userId+","+eula.getModuleLsid()+")");
        
        List<String> remoteUrls=getRemoteUrls(userId, eula);

        //within one transaction,
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            //1) first, record local record,
            local.recordLicenseAgreement(userId, eula);
            //2) add DB entry to the 'eula_remote_queue'
            for (String remoteUrl : remoteUrls) {
                local.addToRemoteQueue(userId, eula, remoteUrl);
            }
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            else {
                log.debug("committing hibernate transaction, even though it was started before this method");
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            String message="Error recording eula to local GP server: "+t.getLocalizedMessage();
            log.error(message,t);
            HibernateUtil.rollbackTransaction();
            throw new Exception(message);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        //2) schedule asynchronous POST of remote record to each remoteUrl
        for (String remoteUrl : remoteUrls) {
            remote.recordLicenseAgreement(userId, eula, remoteUrl);
        }
    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        //delegate to local record
        return local.hasUserAgreed(userId, eula);
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        //delegate to local record
        return local.getUserAgreementDate(userId, eula);
    }

    //@Override
    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        throw new Exception("Not implemented!");
    }
    
    public void updateRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl, boolean success, int statusCode, String statusMessage) {
        //1) update eula_remote_queue table
        //2) insert into eula_remote_log table
    }

}
