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
import org.apache.axis.handlers.*;
import org.apache.axis.description.OperationDesc;
import javax.xml.soap.SOAPPart;
import org.apache.axis.Handler;
import java.util.*;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.message.MessageElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import org.apache.axis.i18n.Messages;
import org.genepattern.server.util.*;

public class AuthorizationHandler extends org.apache.axis.handlers.BasicHandler {
	private IAuthorizationManager authManager = null;

	public void init(){
		try {
	   		String gpprops = (String)getOption("genepattern.properties");
			System.setProperty("genepattern.properties", gpprops);		

			String className = (String)getOption("org.genepattern.AuthorizationManagerFactory");
			Class cl = Class.forName(className);
			Constructor construct = cl.getConstructor(new Class[0]);
		
			IAuthorizationManagerFactory factory = (IAuthorizationManagerFactory)construct.newInstance(new Object[0]);

			authManager = factory.getAuthorizationManager();

			
			// authManager = new AuthorizationManager();

		} catch (Exception e){
		//	throw new ServletException(e);
		}

		super.init();
	}


	public void invoke(MessageContext msgContext) throws AxisFault {
		Message requestMessage = msgContext.getCurrentMessage();
		
		String username = msgContext.getUsername();
		if (username == null)	username = "";
		Handler serviceHandler = msgContext.getService();
 		String serviceName = serviceHandler.getName();
		
  		OperationDesc operation = msgContext.getOperation();
        	SOAPService service = msgContext.getService();
        	ServiceDesc serviceDesc = service.getServiceDescription();
        	QName opQName = null;

        	if (operation == null) {
            	SOAPEnvelope reqEnv = requestMessage .getSOAPEnvelope();
			Vector bodyElements = reqEnv.getBodyElements();
            	if(bodyElements.size() > 0) {
            	    MessageElement element = (MessageElement) bodyElements.get(0);
                		if (element != null) {
                    		opQName = new QName(element.getNamespaceURI(), element.getName());
                    		operation = serviceDesc.getOperationByElementQName(opQName);
                		}
            	}
        	}

        	if (operation == null) {
        	    throw new AxisFault(Messages.getMessage("noOperationForQName",
        	                        opQName == null ? "null" : opQName.toString()));
        	}
        
        	Method method = operation.getMethod();
		String methodSig = serviceName + "." + method.getName();

	 	boolean allowed = authManager.isAllowed(methodSig, username);

	  	//System.out.println("\n\t AH   handler: " + methodSig + " called by " + username + " ok==>" + allowed);

		if (!allowed){
			throw new AxisFault("User " + username +" does not have permission to execute " + methodSig);
			
		}

	}
}