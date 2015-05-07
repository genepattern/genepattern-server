/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.GsAccount;
import org.genepattern.server.domain.GsAccountDAO;

/**
 * Managers all GenomeSpace interaction with the GenePattern database
 * @author tabor
 *
 */
public class GenomeSpaceDatabaseManager {
    private static final Logger log = Logger.getLogger(GenomeSpaceDatabaseManager.class);
    
    /**
     * Return the GenomeSpace token associated with the given GenePattern username
     * @param gpUsername
     * @return
     */
    public static String getGSToken(String gpUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGPUserId(gpUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }
        
        return account.getToken();
    }
    
    /**
     * Return the timestamp of the GenomeSpace token associated with the given GenePattern username
     * @param gpUsername
     * @return
     */
    public static Date getTokenTimestamp(String gpUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGPUserId(gpUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }
        
        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }

        return account.getTokenTimestamp();
    }
    
    /**
     * Returns the GenomeSpace username associated with the given GenePattern username
     * @param gpUsername
     * @return
     */
    public static String getGSUsername(String gpUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGPUserId(gpUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }

        if (account == null) {
            log.error("Unable to get the GsAccount from the database for the user");
            return null;
        }

        return account.getGsUserId();
    }
    
    /**
     * Determines if the given GenePattern account is associated with a GenomeSpace account
     * @param gpUsername
     * @return
     */
    public static boolean isGPAccountAssociated(String gpUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGPUserId(gpUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }

        if (account == null) return false;
        if (account.getGsUserId() == null) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * Determines if the given GenomeSpace account is associated with a GenePattern account
     * @param gsUsername
     * @return
     */
    public static boolean isGSAccountAssociated(String gsUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGSUserId(gsUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }

        if (account == null) return false;
        if (account.getGpUserId() == null) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * Returns the associated GenePattern username for the given GenomeSpace username
     * @param gsUsername
     * @return
     */
    public static String getGPUsername(String gsUsername) {
        GsAccount account = null;
        
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            account = new GsAccountDAO().getByGSUserId(gsUsername);
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error in querying database for GenomeSpace: " + t.getMessage());
        }
        finally {
            if (!inTransaction) HibernateUtil.closeCurrentSession();
        }
        
        if (account == null) return null;
        return account.getGpUserId();
    }
    
    /**
     * Update the database for the given GenomeSpace username and token, deleting other associations for that GS account
     * @param gpUsername
     * @param gsAuthenticationToken
     */
    public static void updateDatabase(String gpUsername, String gsAuthenticationToken, String gsUsername, String email) {
        boolean inTransaction = HibernateUtil.isInTransaction();
        try {
            GsAccountDAO dao = new GsAccountDAO();
            GsAccount account = new GsAccountDAO().getByGPUserId(gpUsername);
            if (account == null) account = new GsAccount();
            account.setGpUserId(gpUsername);
            account.setToken(gsAuthenticationToken);
            account.setTokenTimestamp(new Date());
            account.setGsUserId(gsUsername);
            account.setEmail(email);
            dao.deleteExtraGSAssociation(gsUsername, gpUsername);
            dao.saveOrUpdate(account);
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            log.error("Error updating the GenomeSpace information in the database: " + t.getMessage());
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
}
