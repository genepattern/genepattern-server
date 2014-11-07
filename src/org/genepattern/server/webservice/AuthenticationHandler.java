/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class AuthenticationHandler extends GenePatternHandlerBase {
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
    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        String methodSig = getOperation(msgContext);
        if (log.isDebugEnabled()) {
            log.debug("\tmethodSig: "+methodSig);
       	}
        
        // some methods may be exempted from password protection such as
        // Admin.getServiceInfo
        // there are set in the server-config.wsdd as comma delimited values
        // in the no.login.required parameter
        for (String meth: noLoginMethods){
            if (meth.equalsIgnoreCase(methodSig)) return;
        }
      
        String username = msgContext.getUsername();
        String password = msgContext.getPassword();

        if (log.isDebugEnabled()) {
            log.debug("\tusername: "+username);
        }

        boolean authenticated = false;
        byte[] credentials = password != null ? password.getBytes() : null;
        try {
            log.debug("authenticating username="+username);
            authenticated = UserAccountManager.instance().getAuthentication().authenticate(username, credentials);
        }
        catch (AuthenticationException e) {
            //skip exception, special case for pipelines
            authenticated = false;
        }
        log.debug("authenticated="+authenticated);
        
        //special case for pipelines
        if (!authenticated) {
            byte[] encryptedPassword = EncryptionUtil.getInstance().getPipelineUserEncryptedPassword(new String(password));
            User user = (new UserDAO()).findById(username);
            authenticated = user != null && java.util.Arrays.equals(encryptedPassword, user.getPassword());
        }

        if (!authenticated) {
            throw new AxisFault("Error: Unknown user or invalid password.");
        }
        
        // special-case, define custom 'attachments.Directory'
        final boolean initIsAdmin=false;
        GpContext userContext=GpContext.createContextForUser(username, initIsAdmin);
        File soapAttDir=ServerConfigurationFactory.instance().getSoapAttDir(userContext);
        if (soapAttDir != null) {
            if (log.isDebugEnabled()) {
                log.debug("Setting "+MessageContext.ATTACHMENTS_DIR+"="+soapAttDir+" for user="+username);
            }
            msgContext.setProperty(MessageContext.ATTACHMENTS_DIR, soapAttDir.toString());
        }        
    }
}
