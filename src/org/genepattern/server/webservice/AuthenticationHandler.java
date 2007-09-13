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

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class AuthenticationHandler extends GenePatternHandlerBase {
    boolean passwordRequired = false;

    private static Logger log = Logger.getLogger(AuthenticationHandler.class);
    ArrayList<String> noLoginMethods = new ArrayList<String>();
    
    public void init() {
        super.init();
        
        // some methods may be exempted from password protection such as
        // Admin.getServiceInfo
        // there are set in the server-config.wsdd as comma delimeted values
        // in the no.login.required parameter
        String noLogin = (String)this.getOption("no.login.required");
        StringTokenizer strtok = new StringTokenizer(noLogin, ",");
        while (strtok.hasMoreTokens()){
            noLoginMethods.add(strtok.nextToken());
        }
 
        // see if we care about passwords or not
        String prop = System.getProperty("require.password", "false").toLowerCase();
        passwordRequired = (prop.equals("true") || prop.equals("y") || prop.equals("yes"));

    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        String methodSig = getOperation(msgContext);
        
        // some methods may be exempted from password protection such as
        // Admin.getServiceInfo
        // there are set in the server-config.wsdd as comma delimeted values
        // in the no.login.required parameter
        for (String meth: noLoginMethods){
            if (meth.equalsIgnoreCase(methodSig)) return;
        }
      
        String username = msgContext.getUsername();
        String password = msgContext.getPassword();

        if (!validateUserPassword(username, password)) {
            throw new AxisFault("Error: Unknown user or invalid password.");
        }
    }

    /**
     * 
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
            byte[] encryptedPassword = EncryptionUtil.encrypt(password);
            if (java.util.Arrays.equals(encryptedPassword, up.getPassword())) {
                return true;
            }
        }
        catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        //if unable to validate the password it could be because the client
        //   passed in a string representation of the encrypted password
        //   try that instead
        byte[] encryptedPassword = EncryptionUtil.convertToByteArray(password);
        return java.util.Arrays.equals(encryptedPassword, up.getPassword());
    }
}