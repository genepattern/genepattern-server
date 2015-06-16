/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.auth;

/**
 * Exception thrown during authentication process.
 * 
 * @author pcarr
 */
public class AuthenticationException extends Exception {
    public enum Type {
        SERVICE_NOT_AVAILABLE,
        INVALID_USERNAME,
        INVALID_CREDENTIALS;
        
        public String toString() {
            String str = this.name().toLowerCase().replace('_', ' ');
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
    }
    
    private Type type = null;
    private String message = "";

    public AuthenticationException(Type type) {
        this(type, type.toString());
    }
    public AuthenticationException(Type type, String message) {
        super(type.name());
        this.type = type;
        this.message = message;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getLocalizedMessage() {
        return message;
    }
}
