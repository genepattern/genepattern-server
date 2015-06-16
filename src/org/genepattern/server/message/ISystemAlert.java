/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.message;

import java.util.Date;

/**
 * Broadcast system alert message to online users.
 * @author pcarr
 */
public interface ISystemAlert {

    //admin interface
    /**
     * Send an alert message to all online users.
     */
    void setSystemAlertMessage(SystemMessage message) throws Exception;
    
    /**
     * Delete the system alert message.
     * @throws Exception
     */
    void deleteSystemAlertMessage() throws Exception;
    
    /**
     * Delete the system alert message if and only if it is flagged to be deleted on restart.
     * Call this on server restart.
     * @throws Exception
     */
    void deleteOnRestart() throws Exception;
 
    /**
     * Retrieve the system message
     * This is usually called by an admin user in order to edit/delete the message.
     * @return a SystemMessage or null if none is set.
     * @throws Exception
     */
    SystemMessage getSystemMessage() throws Exception;

     //client interface
    /**
     * Retrieve the active system message or null if no message is active.
     * This is usually called by a client in order to display the active message.
     * @param date
     * @return a SystemMessage or null of none is set.
     */
    SystemMessage getSystemMessage(Date date) throws Exception;
    
}
