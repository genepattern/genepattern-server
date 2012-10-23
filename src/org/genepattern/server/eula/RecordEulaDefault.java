package org.genepattern.server.eula;

import java.util.Date;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.RecordEulaToRemoteServer;

public class RecordEulaDefault implements RecordEula {
    private RecordEula local;
    
    public RecordEulaDefault() {
        local=new RecordEulaToDb();
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        //1) first, record local record
        local.recordLicenseAgreement(userId, eula);

        //2) then post remote record
        RecordEulaToRemoteServer post=new RecordEulaToRemoteServer();
        post.recordLicenseAgreement(userId, eula);
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
