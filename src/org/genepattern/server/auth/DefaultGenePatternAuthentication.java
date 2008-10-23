package org.genepattern.server.auth;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

/**
 * Default GenePattern Authentication which authenticates a user based on username and the encrypted password
 * stored in the GenePattern database.
 * 
 * The input credentials are not encrypted.
 * 
 * @author pcarr
 */
public class DefaultGenePatternAuthentication implements IAuthenticationPlugin {
    private boolean passwordRequired = true;
    
    public DefaultGenePatternAuthentication() {
        //TODO: make sure this configuration parameter is well-documented
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));
    }
    
    public void setPasswordRequired(boolean b) {
        this.passwordRequired = b;
    }

    public boolean authenticate(String username, byte[] password) throws AuthenticationException {
        if (username == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "Missing required parmameter: username");
        }

        if (passwordRequired && password == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_CREDENTIALS, "Missing required parmameter: password");
        }

        User user = null;
        try {
            user = (new UserDAO()).findById(username);
        }
        catch (Error e) {
            throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
        }
        if (user == null) {
            throw new AuthenticationException(AuthenticationException.Type.INVALID_USERNAME, "User '"+username+"' is not registered.");
        }

        if (passwordRequired) {
            String rawPasswordString = new String(password);
            byte[] encryptedPassword = null;
            try {
                encryptedPassword = EncryptionUtil.encrypt(rawPasswordString);
            }
            catch (NoSuchAlgorithmException e) {
                throw new AuthenticationException(AuthenticationException.Type.SERVICE_NOT_AVAILABLE, e.getLocalizedMessage());
            }
            boolean validPassword = Arrays.equals(encryptedPassword, user.getPassword());
            if (!validPassword) {
                throw new AuthenticationException(AuthenticationException.Type.INVALID_CREDENTIALS);
            }
            return true;
        }
        else {
            return true;
        }
    }
}
