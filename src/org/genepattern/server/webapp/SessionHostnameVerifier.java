/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class SessionHostnameVerifier implements HostnameVerifier {
    public boolean verify(String urlHostName, SSLSession session) {
        if (!urlHostName.equals(session.getPeerHost())) {
            System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
        }
        return true;
    } 
}
