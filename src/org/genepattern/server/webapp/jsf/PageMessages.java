/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;

public class PageMessages {

    Logger log = Logger.getLogger(PageMessages.class.getName());
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
                messageImage = "/skin/info_obj.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_WARN) == 0) {
                messageImage = "/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_ERROR) == 0) {
                messageImage = "/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_FATAL) == 0) {
                messageImage = "/skin/stop.gif";
            }
        }
        else {
            log.debug("Severity Level Trapped: level = 'null'");
        }
    }

    public Boolean getRenderMessage() {
        return new Boolean(StringUtils.isNotBlank(getMessageHeader()));
    }

    public String getMessageHeader() {
        return messageHeader;
    }

    public String getMessageImage() {
        return messageImage;
    }
}
