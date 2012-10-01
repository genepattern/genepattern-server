package org.genepattern.server.licensemanager;

import java.util.HashSet;
import java.util.Set;

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

    private Set<String> acceptedEulas = new HashSet<String>();
    
    private RecordEulaStub() {
        //force singleton
    }
    public void recordLicenseAgreement(String userId, String lsid) throws Exception {
        String uniq_key = lsid+"_"+userId;
        acceptedEulas.add(uniq_key);
    }

    public boolean hasUserAgreed(String userId, EulaInfo eula) throws Exception {
        String lsid=eula.getModuleLsid();
        String uniq_key = lsid+"_"+userId;
        return acceptedEulas.contains(uniq_key);
    }

}
