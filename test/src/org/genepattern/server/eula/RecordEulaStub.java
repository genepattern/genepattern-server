package org.genepattern.server.eula;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.eula.dao.EulaRemoteQueue;

/**
 * For debugging and prototyping, this implementation records eula to an
 * in-memory Map. All license agreement info will be lost on server shutdown.
 * 
 * @author pcarr
 */
public class RecordEulaStub implements RecordEula {

    private Map<String,Date> acceptedEulas = new HashMap<String,Date>();
    private Map<EulaRemoteQueue.Key,EulaRemoteQueue> remoteRecordQueue = new HashMap<EulaRemoteQueue.Key,EulaRemoteQueue>();
    
    public RecordEulaStub() {
    }

    @Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        final String lsid = eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        acceptedEulas.put(uniq_key, new Date());
    }

    @Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        final String lsid=eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        return acceptedEulas.containsKey(uniq_key);
    }

    @Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        final String lsid=eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        return acceptedEulas.get(uniq_key);
    }

    @Override
    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {        
        EulaRemoteQueue.Key key = new EulaRemoteQueue.Key(eula.hashCode(), remoteUrl);
        EulaRemoteQueue val = new EulaRemoteQueue(key);
        remoteRecordQueue.put(key, val);
    }

    @Override
    public void updateRemoteQueue(String userId, EulaInfo eula, String remoteUrl, boolean success) throws Exception {
        EulaRemoteQueue.Key key = new EulaRemoteQueue.Key(eula.hashCode(), remoteUrl);
        EulaRemoteQueue val = remoteRecordQueue.get(key);
        if (val == null) {
            throw new Exception("couldn't find entry in DB");
        }
        if (val != null) {
            val.setRecorded(success);
            val.setDateRecorded(new Date());
            remoteRecordQueue.put(key, val);
        }
    }
}
