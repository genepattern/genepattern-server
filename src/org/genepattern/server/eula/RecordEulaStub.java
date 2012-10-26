package org.genepattern.server.eula;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * For debugging and prototyping, this implementation records eula to an
 * in-memory Map. All license agreement info will be lost on server shutdown.
 * 
 * @author pcarr
 */
public class RecordEulaStub implements RecordEula {
    static public class Singleton {
        private static final RecordEulaStub INSTANCE = new RecordEulaStub();
        public static RecordEulaStub instance() {
            return INSTANCE;
        }
    }
    static public RecordEulaStub instance() {
        return Singleton.INSTANCE;
    } 

    private Map<String,Date> acceptedEulas = new HashMap<String,Date>();
    
    private RecordEulaStub() {
        //force singleton
    }
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        final String lsid = eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        acceptedEulas.put(uniq_key, new Date());
    }

    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        final String lsid=eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        return acceptedEulas.containsKey(uniq_key);
    }

    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        final String lsid=eula.getModuleLsid();
        final String uniq_key = lsid+"_"+userId;
        return acceptedEulas.get(uniq_key);
    }

    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        // TODO Auto-generated method stub
        throw new Exception("Not implemented!"); 
    }
}
