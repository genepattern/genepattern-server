/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import java.util.Date;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.message.ISystemAlert;
import org.genepattern.server.message.SystemAlertFactory;
import org.genepattern.server.message.SystemMessage;

/**
 * Administrative interface to send and clear system alert messages.
 * @author pcarr
 */
public class SystemMessageAdminBean {
    private static Logger log = Logger.getLogger(SystemMessageAdminBean.class);    
    
    private SystemMessage systemMessage = new SystemMessage();

    public SystemMessageAdminBean() {
        try {
            //1. check to see if there is already a current message
            systemMessage = SystemAlertFactory.getSystemAlert().getSystemMessage();
            if (systemMessage == null) {
                //2. otherwise initialize
                Date now = new Date();
                systemMessage = new SystemMessage();
                systemMessage.setMessage("");
                systemMessage.setStartTime(now);
            }
        }
        catch (Exception e) {
            log.error(e);
        }
    }
    
    public SystemMessage getSystemMessage() {
        return systemMessage;
    }
    
    /**
     * Save system message to the database.
     */
    public void save(ActionEvent evt) {
        ISystemAlert sysAlert = SystemAlertFactory.getSystemAlert();
        try {
            sysAlert.setSystemAlertMessage(systemMessage);
        }
        catch (Exception e) {
            reportException(e, evt);
        }
    }
    
    /**
     * Delete system message from the database.
     * @param evt
     * @throws Exception
     */
    public void delete(ActionEvent evt) {
        ISystemAlert sysAlert = SystemAlertFactory.getSystemAlert();
        try {
            sysAlert.deleteSystemAlertMessage();
            systemMessage = new SystemMessage();
            systemMessage.setMessage("");
            systemMessage.setStartTime(new Date());
        }
        catch (Exception e) {
            reportException(e, evt);
        }
    }
    
    private void reportException(Exception e, ActionEvent evt) {
        //1. update server logs
        log.error(e.getLocalizedMessage(), e);
        //2. notify web client
        FacesContext context = FacesContext.getCurrentInstance();
        String clientId = evt.getComponent().getClientId(context);
        FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getLocalizedMessage(), e.getLocalizedMessage());
        context.addMessage(clientId, fm);
    }
    
    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }
}
