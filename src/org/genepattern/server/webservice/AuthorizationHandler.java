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
import org.apache.axis.MessageContext;
import org.genepattern.server.util.AuthorizationManagerFactory;

public class AuthorizationHandler extends GenePatternHandlerBase {

    public void invoke(final MessageContext msgContext) throws AxisFault {
        String username = msgContext.getUsername();
        if (username == null) {
            username = "";
        }
        final String methodSig = getOperation(msgContext);

        final boolean allowed = AuthorizationManagerFactory.getAuthorizationManager().isAllowed(methodSig, username);
        if (!allowed) {
            throw new AxisFault("User " + username + " does not have permission to execute " + methodSig);
        }
    }
}
