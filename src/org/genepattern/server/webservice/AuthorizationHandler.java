/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
