package org.genepattern.server.webservice.server.dao;


public class DataAccessException extends RuntimeException {
    
    public DataAccessException(Exception cause) {
        super(cause);
    }

}
