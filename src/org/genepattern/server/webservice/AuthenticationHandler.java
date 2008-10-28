/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.AuthenticationException;

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
        // there are set in the server-config.wsdd as comma delimeted values
        // in the no.login.required parameter
        for (String meth: noLoginMethods){
            if (meth.equalsIgnoreCase(methodSig)) return;
        }
      
        String username = msgContext.getUsername();
        String password = msgContext.getPassword();

        if (log.isDebugEnabled()) {
            log.debug("\tusername: "+username);
            //don't log passwords!!!
            //log.debug("\tpassword: "+password);
            //what happened to log.trace()? 
            //for(StackTraceElement elem : Thread.currentThread().getStackTrace()) {
            //    log.trace("\tat "+elem.getClassName()+"."+elem.getMethodName()+
            //        "("+elem.getMethodName()+":"+elem.getLineNumber()+")");
            //}
        }

        boolean authenticated = false;
        byte[] credentials = password != null ? password.getBytes() : null;
        try {
            authenticated = UserAccountManager.instance().getAuthentication().authenticate(username, credentials);
        }
        catch (AuthenticationException e) {
            throw new AxisFault(e.getType().toString());
        }

        if (!authenticated) {
            throw new AxisFault("Error: Unknown user or invalid password.");
        }
    }
}
