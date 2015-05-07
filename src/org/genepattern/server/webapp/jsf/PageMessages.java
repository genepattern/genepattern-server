/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;

public class PageMessages {
    public static final String ERROR_MESSAGE_KEY =  "errorMessages";
    public static final Logger log = Logger.getLogger(PageMessages.class);
    private String messageHeader;
    private String messageImage;
    private Severity severityLevel;

    public PageMessages() {
        messageHeader = null;

        // See if there are messages queued for the page
        severityLevel = UIBeanHelper.getFacesContext().getMaximumSeverity();

        if (null != severityLevel) {
            log.debug("Severity Level Trapped: level = '" + severityLevel.toString() + "'");
            if (severityLevel.compareTo(FacesMessage.SEVERITY_INFO) == 0) {
                messageImage = "/website/skin/info_obj.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_WARN) == 0) {
                messageImage = "/website/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_ERROR) == 0) {
                messageImage = "/website/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_FATAL) == 0) {
                messageImage = "/website/skin/stop.gif";
            }
        }
        else {
            log.debug("Severity Level Trapped: level = 'null'");
        }
    }

    public Boolean getRenderMessage() {
        return true;
    }

    public String getMessageHeader() {
        String messages = (String) UIBeanHelper.getRequest().getSession().getAttribute(ERROR_MESSAGE_KEY);
        if (messages != null) {
            if (messageHeader == null || messageHeader.length() == 0) {
                messageHeader = messages;
            }
            else {
                messageHeader += messages;
            }
        }
        UIBeanHelper.getRequest().getSession().setAttribute(ERROR_MESSAGE_KEY, null);
        return messageHeader;
    }
    
    public boolean getRenderMessageImage() {
        if (messageImage == null) {
            return false;
        }
        else {
            return true;
        }
    }

    public String getMessageImage() {
        return messageImage;
    }
}
