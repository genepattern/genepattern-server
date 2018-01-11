/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;


public class DataAccessException extends RuntimeException {
    
    public DataAccessException(Exception cause) {
        super(cause);
    }

}
