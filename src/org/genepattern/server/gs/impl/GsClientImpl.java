package org.genepattern.server.gs.impl;

import java.io.InputStream;
import java.net.URL;

import org.genepattern.server.gs.GsClient;
import org.genepattern.server.gs.GsClientException;
import org.genepattern.server.webapp.genomespace.GenomeSpaceJobHelper;
import org.genomespace.client.DataManagerClient;
import org.genomespace.client.GsSession;
import org.genomespace.client.exceptions.InternalServerException;

/**
 * Concrete implementation of the GsClient interface.
 * This class, and related classes directly reference classes in the GenomeSpace CDK.
 * 
 * @author pcarr
 *
 */
public class GsClientImpl implements GsClient {
    public boolean isGenomeSpaceFile(URL url) {
        return GenomeSpaceJobHelper.isGenomeSpaceFile(url);
    }
    
    public InputStream getInputStream(String gpUserId, URL url) throws GsClientException {
        InputStream is = null;
        String token = GenomeSpaceJobHelper.getGSToken(gpUserId);
        if (token == null) {
            throw new GsClientException("Unable to get the GenomeSpace session token needed to access GenomeSpace files");
        }
        else {
            GsSession session;
            try {
                session = new GsSession(token);
            }
            catch (InternalServerException e) {
                throw new GsClientException("Unable to initialize GenomeSpace session", e);
            }
            DataManagerClient dmc = session.getDataManagerClient();
            is = dmc.getInputStream(url);
        }
        return is;
    }

}
