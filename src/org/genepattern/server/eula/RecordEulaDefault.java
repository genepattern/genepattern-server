package org.genepattern.server.eula;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.RecordEulaToRemoteServerAsync;


public class RecordEulaDefault implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaDefault.class);
    private RecordEula local;
    private RecordEula remote;
    
    public RecordEulaDefault() {
        local=new RecordEulaToDb();
        remote=new RecordEulaToRemoteServerAsync();
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("recordLicenseAgreement("+userId+","+eula.getModuleLsid()+")");

        //1) first, record local record
        local.recordLicenseAgreement(userId, eula);

        //2) then post remote record
        remote.recordLicenseAgreement(userId, eula);
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

}
