package org.genepattern.server.licensemanager;

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
        void recordLicenseAgreement(String userId, String lsid) throws Exception;
        boolean hasUserAgreed(String userId, EulaInfo eula) throws Exception;
}
