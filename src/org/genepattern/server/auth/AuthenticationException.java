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
    
    private Type type = null;

    public AuthenticationException(Type type, String...args) {
        super(type.name());
        
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
}
