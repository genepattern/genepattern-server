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
    
    
    public static final String  OAUTH_USER_ID_USERPROPS_KEY = "globus.user.id";
    public static final String  OAUTH_ID_PROVIDER_ID_USERPROPS_KEY = "globus.idprovider.id";
    public static final String  OAUTH_ID_PROVIDER_DISPLAY_USERPROPS_KEY = "globus.idprovider.display";
    public static final String  OAUTH_EMAIL_USERPROPS_KEY = "globus.user.email";
    
    
    public static final String  OAUTH_USER_ID_ATTR_KEY = "globus.identity";
    public static final String  OAUTH_EMAIL_ATTR_KEY = "globus.email";
    public static final String  OAUTH_TOKEN_ATTR_KEY = "globus.access.token";
    public static final String  OAUTH_TRANSFER_TOKEN_ATTR_KEY = "globus.transfer.token";
    public static final String  OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY = "globus.transfer.refresh.token";
    public static final String  OAUTH_REFRESH_TOKEN_ATTR_KEY = "globus.refresh.token";
    
    
    // used for transfer in from other locations
    public static final String OAUTH_LOCAL_ENDPOINT_ID = "globus.local.endpoint.id";
    public static final String OAUTH_LOCAL_ENDPOINT_ROOT ="globus.local.endpoint.root";
    public static final String OAUTH_LOCAL_ENDPOINT_TYPE = "globus.local.endpoint.type";
    public static final String OAUTH_ENDPOINT_TYPE_LOCALFILE = "localfile";
    public static final String OAUTH_ENDPOINT_TYPE_S3 = "s3";
    
    
}
