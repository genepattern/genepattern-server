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
import java.util.ArrayList;
import java.util.StringTokenizer;
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

public abstract class GenePatternHandlerBase extends org.apache.axis.handlers.BasicHandler {
    

    
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
    
    
 
}