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
import org.apache.axis.Message;
import org.apache.axis.MessageContext;

public class AuthenticationHandler extends org.apache.axis.handlers.BasicHandler {

	public void init(){

        // Placeholder -- nothing to do

		super.init();
	}


	public void invoke(MessageContext msgContext) throws AxisFault {
		Message requestMessage = msgContext.getCurrentMessage();
		
		String username = msgContext.getUsername();
        String password = msgContext.getPassword();
        
		
	  	//System.out.println("\n\t AH   handler: " + methodSig + " called by " + username + " ok==>" + allowed);

		if (validateUserPassword(username, password)){
			throw new AxisFault("Error: Unknown user or invalid password.");
			
		}
	}
    
    /**
     * @todo - implementation.  Currently accepts any non-null user
     * @param user
     * @param password
     * @return
     */
    private boolean validateUserPassword(String user, String password) {
        
        return user != null && user.length() > 0;
        
    }
}