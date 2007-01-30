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

package edu.mit.broad.gp.ws;

import org.genepattern.client.GPServer;
import org.genepattern.webservice.AdminProxy;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

public class Helper {
    protected AdminProxy adminProxy;

    protected TaskIntegratorProxy taskIntegratorProxy;

    protected GPServer gpServer;

    protected String userName = "test";

    protected String password = "test";

    protected String server = "http://127.0.0.1:8080";

    public Helper() {
        try {
            adminProxy = new AdminProxy(server, userName, password);
            taskIntegratorProxy = new TaskIntegratorProxy(server, userName, password);
            gpServer = new GPServer(server, userName, password);
        } catch (WebServiceException e) {
            e.printStackTrace();
        }
    }

}
