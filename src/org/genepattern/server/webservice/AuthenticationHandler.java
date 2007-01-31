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

import java.lang.reflect.Method;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.axis.AxisFault;
import org.apache.axis.Handler;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.i18n.Messages;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPEnvelope;
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
        String methodSig = getOperation(msgContext);
        
        if ("AdminService.getServiceInfo".equalsIgnoreCase(methodSig)){
            // this is always allowed, even anonymously
            
        } else {
            String username = msgContext.getUsername();
            String password = msgContext.getPassword();

            if (!validateUserPassword(username, password)) {
                throw new AxisFault("Error: Unknown user or invalid password.");

            }
        }
        
    }

    protected String getOperation(MessageContext msgContext) throws AxisFault{
        Message requestMessage = msgContext.getCurrentMessage();

      
        Handler serviceHandler = msgContext.getService();
        String serviceName = serviceHandler.getName();

        OperationDesc operation = msgContext.getOperation();
        SOAPService service = msgContext.getService();
        ServiceDesc serviceDesc = service.getServiceDescription();
        QName opQName = null;

        if (operation == null) {
            SOAPEnvelope reqEnv = requestMessage.getSOAPEnvelope();
            Vector bodyElements = reqEnv.getBodyElements();
            if (bodyElements.size() > 0) {
                MessageElement element = (MessageElement) bodyElements.get(0);
                if (element != null) {
                    opQName = new QName(element.getNamespaceURI(), element.getName());
                    operation = serviceDesc.getOperationByElementQName(opQName);
                }
            }
        }

        if (operation == null) {
            throw new AxisFault(Messages.getMessage("noOperationForQName", opQName == null ? "null" : opQName
                    .toString()));
        }

        Method method = operation.getMethod();
        String methodSig = serviceName + "." + method.getName();
        return methodSig;

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