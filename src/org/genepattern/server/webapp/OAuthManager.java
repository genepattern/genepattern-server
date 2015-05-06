package org.genepattern.server.webapp;

import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

import java.util.*;

/**
 * Manages active OAuth token sessions
 *
 * @author Thorin Tabor
 */
public class OAuthManager {
    private static Logger log = Logger.getLogger(OAuthManager.class);

    /**
     * This is a map of all currently valid tokens (plus as yet to be invalidated
     * expired ones), linked to their associated users and expiry dates.
     */
    private Map<String, OAuthSession> tokenSessionMap = null;

    /**
     * This is a map of authorization codes awaiting confirmation, including
     * associated user and expiry times.
     */
    private Map<String, OAuthSession> codeSessionMap = null;

    /**
     * Lazy load the singleton instance of the UserAccountManager class.
     */
    private static class Singleton {
        static OAuthManager oauthManager = new OAuthManager();
    }

    /**
     * private constructor requires call to {@link #instance()}.
     */
    private OAuthManager() {
        tokenSessionMap = new HashMap<String, OAuthSession>();
    }

    /**
     * @return the singleton instance of the UserAccountManager.
     */
    public static OAuthManager instance() {
        return Singleton.oauthManager;
    }

    /**
     * Given the current time and the time until a token expires,
     * return the time when the expiry occurs
     *
     * @param timeTillExpiry
     * @return
     */
    public static long calcExpiry(long timeTillExpiry) {
        Date now = new Date();
        return now.getTime() + timeTillExpiry;
    }

    /**
     * Given the set of requested scopes during OAuth authentication,
     * return the username of the scope requested
     *
     * @param scopes
     * @return
     */
    public static String userFromScope(Set<String> scopes) {
        // Only 1 user should ever be requested at a time with our REST API
        // All scopes should be users with our REST API.
        // So just return the first scope
        Iterator<String> it = scopes.iterator();
        return it.next();
    }

    /**
     * Returns true if all requested scopes are valid, throws OAuthProblemException otherwise.
     * Scopes are valid if only one is requested and it matches a valid user
     *
     * @param scopes
     * @throws OAuthProblemException
     * @return
     */
    public static boolean validateScopes(Set<String> scopes) throws OAuthProblemException {
        // Make sure scopes is of the correct length
        if (scopes.size() != 1) {
            throw OAuthProblemException.error("Invalid number of scopes requested: request 1");
        }

        // Get the scope
        Iterator<String> it = scopes.iterator();
        String scope = it.next();

        // Ensure that the scope is a valid user
        UserDAO dao = new UserDAO();
        User user = dao.findByIdIgnoreCase(scope);

        // Return if valid
        if (user == null) {
            throw OAuthProblemException.error("Invalid user scope requested");
        }
        else {
            return true;
        }
    }

    /**
     * Remove expired sessions from the session and code maps.
     * Intended for lazy culling through calls in other methods.
     */
    private void purgeExpiredSessions() {
        long now = (new Date()).getTime();

        for (String token : tokenSessionMap.keySet()) {
            OAuthSession session = tokenSessionMap.get(token);
            if (session.getExpires() <= now) {
                tokenSessionMap.remove(token);
            }
        }

        for (String code : codeSessionMap.keySet()) {
            OAuthSession session = codeSessionMap.get(code);
            if (session.getExpires() <= now) {
                codeSessionMap.remove(code);
            }
        }
    }

    /**
     * Returns whether or not a token is currently valid
     *
     * @param token
     * @return
     */
    public boolean isTokenValid(String token) {
        // Lazily purge sessions
        purgeExpiredSessions();

        OAuthSession session = tokenSessionMap.get(token);
        return session != null;
    }

    /**
     * Returns the username associated with a valid token
     * Returns null if the token is not valid
     *
     * @param token
     * @return
     */
    public String getUsernameFromToken(String token) {
        // Lazily purge sessions
        purgeExpiredSessions();

        OAuthSession session = tokenSessionMap.get(token);

        // Return null if the token is not valid
        if (session == null) return null;

        return session.getUsername();
    }

    /**
     * Create a token session and add it to the session map
     *
     * @param user
     * @param token
     * @param expiry
     */
    public void createTokenSession(String user, String token, long expiry) {
        OAuthSession session = new OAuthSession(user, expiry);
        tokenSessionMap.put(token, session);
    }

    /**
     * Create a authorization code session and add it to the code map
     * @param user
     * @param code
     * @param expiry
     */
    public void createCodeSession(String user, String code, long expiry) {
        OAuthSession session = new OAuthSession(user, expiry);
        codeSessionMap.put(code, session);
    }

    /**
     * Class that represents a currently valid token or code session in OAuth
     */
    public class OAuthSession {
        private String username;
        private long expires;

        public OAuthSession(String username, long expires) {
            this.username = username;
            this.expires = expires;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public long getExpires() {
            return expires;
        }

        public void setExpires(long expires) {
            this.expires = expires;
        }
    }
}
