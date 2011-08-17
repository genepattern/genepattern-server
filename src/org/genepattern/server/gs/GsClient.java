package org.genepattern.server.gs;

import java.io.InputStream;
import java.net.URL;

public interface GsClient {
    boolean isGenomeSpaceFile(URL url);
    InputStream getInputStream(String gpUserId, URL url) throws GsClientException;
}
