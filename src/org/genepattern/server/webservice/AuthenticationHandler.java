/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webapp.jsf.EncryptionUtil;

public class AuthenticationHandler extends org.apache.axis.handlers.BasicHandler {
    boolean passwordRequired = false;

    private static Logger log = Logger.getLogger(AuthenticationHandler.class);

    public void init() {
        super.init();
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));

    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        
        String username = msgContext.getUsername();
        String password = msgContext.getPassword();

        if (!validateUserPassword(username, password)) {
            throw new AxisFault("Error: Unknown user or invalid password.");

        }
    }

    /**
     * @todo - implementation. Currently accepts any non-null user
     * @param user
     * @param password
     * @return
     */
    private boolean validateUserPassword(String user, String password) {
        if (!passwordRequired) {
            return true;
        }

        User up = (new UserDAO()).findById(user);
        if (up == null || password == null) {
            return false;
        }

        try {
            return (java.util.Arrays.equals(EncryptionUtil.encrypt(password), up.getPassword()));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}