package org.genepattern.server.webapp.jsf;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;

public class PageMessages extends AbstractUIBean {

    Logger log = Logger.getLogger(PageMessages.class.getName());
    private String messageHeader;
    private String messageImage;
    private Severity severityLevel;

    public PageMessages() {
        messageHeader = null;

        // See if there are messages queued for the page
        severityLevel = getFacesContext().getMaximumSeverity();

        if (null != severityLevel) {
            log.debug("Severity Level Trapped: level = '" + severityLevel.toString() + "'");
            if (severityLevel.compareTo(FacesMessage.SEVERITY_INFO) == 0) {
                messageHeader = null;
                messageImage = "/skin/info_obj.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_WARN) == 0) {
                messageHeader = null;
                messageImage = "/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_ERROR) == 0) {
                messageHeader = "Error";
                messageImage = "/skin/warning.gif";
            }
            else if (severityLevel.compareTo(FacesMessage.SEVERITY_FATAL) == 0) {
                messageHeader = "Fatal Error";
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