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
    }
    
    public AuthenticationException(Type type, String...args) {
        super(type.name());
    }
}
