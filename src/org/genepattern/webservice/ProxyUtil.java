/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.webservice;

import java.net.MalformedURLException;
import java.net.URL;

import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Helper class for the *Proxy classes.
 * @author pcarr
 */
public class ProxyUtil {
    /**
     * Create an endpoint url (String) from the given server url <protocol://host>, and the path to the service, relative to the gp context path.
     * 
     * @param url, e.g. 'http://genepattern.broadinstitute.org'
     * @param path, e.g. '/services/Analysis'
     * @return a String representing the URL of the web service, e.g. http://genepattern.broadinstitute.org/gp/services/Analysis
     * @throws MalformedURLException
     */
    public static String createEndpoint(String url, String path) throws MalformedURLException {
        URL inputUrl = new URL(url);
        String protocol = inputUrl.getProtocol();
        if (protocol == null || !protocol.startsWith("http")) {
            protocol = "http";
        }
        String portStr = "";
        if (inputUrl.getPort() != -1) {
            portStr = ":" + inputUrl.getPort();
        }
        String context = ServerConfigurationFactory.instance().getGpPath();
        if (!context.startsWith("/")) {
            context = "/" + context;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return inputUrl.getProtocol() + "://" + inputUrl.getHost() + portStr + context + path;
    }
}
