/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }
    public ConfigurationException(Throwable t) {
        super(t);
    }
    public ConfigurationException(String message, Throwable t) {
        super(message,t);
    }
}
