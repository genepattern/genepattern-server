/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

public class ServerConfigurationException extends Exception {
    public ServerConfigurationException() {
        super();
    }
    public ServerConfigurationException(String message) {
        super(message);
    }
    public ServerConfigurationException(String message, Throwable t) {
        super(message, t);
    }
}
