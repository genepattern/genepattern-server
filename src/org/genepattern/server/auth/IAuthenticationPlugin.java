package org.genepattern.server.auth;

public interface IAuthenticationPlugin {
    public boolean authenticate(String user, byte[] credentials) throws AuthenticationException;
}
