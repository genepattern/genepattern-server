/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.util.Date;

/**
 * Interface for recording an End-user license agreement. 
 * This abstraction layer makes it possible to implement different ways to record the agreement.
 * Examples include:
 *     for unit testing, a Mock record
 *     for prototyping, a JVM record, which is lost on server shutdown
 *     for production, a local record, stored to a DB
 *     for production, when needed, a remote record, via a web service call.
 *     
 * @author pcarr
 */
public interface RecordEula {
    
    //TODO: void recordLicenseAgreement(Context context, EulaInfo eula) throws Exception;

    /**
     * This method is called when a user accepts the End-user license agreement for a module;
     * for example, when the 'Ok' button is clicked in the GUI.
     * 
     * @param userId, the user who accepted the agreement.
     * @param eula, the End-user license agreement for the module or pipeline.
     * 
     * @throws Exception
     */
    void recordLicenseAgreement(String userId, EulaInfo eula) throws Exception;
    
    /**
     * This method is called before showing the module in a job submit form, and 
     * also before adding a new job to the queue.
     * 
     * @param userId
     * @param eula
     * @return true, if the user has already accepted the EULA for the given module.
     * @throws Exception
     */
    boolean hasUserAgreed(String userId, EulaInfo eula) throws Exception;
    
    /**
     * Helper method, get the date that the user accepted the license agreement.
     * Not sure if we need this yet, but it is invoked in the junit tests to confirm
     * that we are indeed storing the date in the local GP DB.
     * 
     * @param userId
     * @param eula
     * @return the date that the user agreed to the license, otherwise null, if they haven't agreed.
     * 
     * @throws Exception
     */
    Date getUserAgreementDate(String userId, EulaInfo eula) throws Exception;
    
    
    //helper methods for logging status of POST to remote server
    /**
     * This method is optionally called after successful call to recordLicenseAgreement,
     * it means 'schedule POST callback to remote server'.
     * @param userId
     * @param eula
     */
    void addToRemoteQueue(String userId, EulaInfo eula, String remoteUrl) throws Exception;
    
    /**
     * This method is optionally called after successful POST to 
     */
    void updateRemoteQueue(String userId, EulaInfo eula, String remoteUrl, boolean success) throws Exception;

}
