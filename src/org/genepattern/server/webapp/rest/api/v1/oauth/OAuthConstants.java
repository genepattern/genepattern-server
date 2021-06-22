package org.genepattern.server.webapp.rest.api.v1.oauth;

public class OAuthConstants {
    
    //
    // these constants are used in config_custom.yaml
    //
    public static final String OAUTH_AUTHORIZE_URL_KEY = "oauth.authorize.url";
    public static final String OAUTH_TOKEN_URL_KEY = "oauth.token.url";
    public static final String OAUTH_CLIENT_ID_KEY = "oauth.client.id";
    public static final String OAUTH_CLIENT_SECRET_KEY = "oauth.client.secret";
    public static final String OAUTH_AUTHORIZE_SCOPES_KEY = "oauth.client.scopes";
    
    //
    // these constants are used in the UserProps table
    //
    public static final String  OAUTH_USER_ID_USERPROPS_KEY = "globus.user.id";
    public static final String  OAUTH_ACCESS_TOKEN_USERPROPS_KEY = "globus.access.token";
    public static final String  OAUTH_ID_PROVIDER_ID_USERPROPS_KEY = "globus.idprovider.id";
    public static final String  OAUTH_ID_PROVIDER_DISPLAY_USERPROPS_KEY = "globus.idprovider.display";
    public static final String  OAUTH_EMAIL_USERPROPS_KEY = "globus.user.email";
    
    //
    // these constants are used in the session and request attributes
    //
    public static final String  OAUTH_USER_ID_ATTR_KEY = "globus.identity";
    public static final String  OAUTH_EMAIL_ATTR_KEY = "globus.email";
    public static final String  OAUTH_TOKEN_ATTR_KEY = "globus.access.token";
    
}
