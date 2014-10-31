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

import org.apache.axis.AxisFault;
import org.apache.axis.Handler;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;

public class AuthorizationHandler extends GenePatternHandlerBase {
    private IAuthorizationManager authManager = null;

    public void init() {
        super.init();
        String gpprops = (String) getOption("genepattern.properties");
        System.setProperty("genepattern.properties", gpprops);
        String className = (String) getOption("org.genepattern.AuthorizationManager");
        System.setProperty("org.genepattern.AuthorizationManager", className);
        authManager = AuthorizationManagerFactory.getAuthorizationManager();
    }

    public void invoke(MessageContext msgContext) throws AxisFault {
        Message requestMessage = msgContext.getCurrentMessage();

        String username = msgContext.getUsername();
        if (username == null)
            username = "";
        Handler serviceHandler = msgContext.getService();
        String serviceName = serviceHandler.getName();

        
        String methodSig = getOperation(msgContext);

        boolean allowed = authManager.isAllowed(methodSig, username);

        // System.out.println("\n\t AH handler: " + methodSig + " called by " +
        // username + " ok==>" + allowed);

        if (!allowed) {
            throw new AxisFault("User " + username + " does not have permission to execute " + methodSig);

        }

    }
}
