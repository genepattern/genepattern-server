package org.genepattern.server.genomespace;

import java.util.Map;

/**
 * Response information gathered from GenomeSpace login.
 * It must not reference any GenomeSpace core classes.
 */
public class GenomeSpaceLogin {
    private boolean unknownUser = true;
    private Map<String,Object> attributes;
    private String authenticationToken;
    private String username = "";
    private String email = null;

    public boolean isUnknownUser() {
        return unknownUser;
    }

    public void setUnknownUser(boolean unknownUser) {
        this.unknownUser = unknownUser;
    }

    public Map<String, Object> getAttributes() {

        return attributes;
    }
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
