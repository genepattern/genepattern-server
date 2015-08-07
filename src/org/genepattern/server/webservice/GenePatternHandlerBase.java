/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

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
