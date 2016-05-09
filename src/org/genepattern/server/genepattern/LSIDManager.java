/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.TASK_NAMESPACE;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.LsidVersion;
import org.genepattern.webservice.OmnigeneException;

/**
 * manage LSID creation and versioning (initially for the repository modules).
 * This manager needs to be able to create new IDs as well as version ids and
 * ensure that a particular ID and version is never given out more than once
 */
public class LSIDManager {
    private static final Logger log = Logger.getLogger(LSIDManager.class);

    private LSIDManager() {
    }

    /** @deprecated pass in a HibernateSession */
    public static LSID getNextTaskLsid(final String requestedLSID)  throws java.rmi.RemoteException {
        return getNextTaskLsid(requestedLSID, LsidVersion.Increment.next);
    }

    public static LSID getNextTaskLsid(final String requestedLSID, final LsidVersion.Increment versionIncrement)  throws java.rmi.RemoteException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID, versionIncrement);
    }

    /** @deprecated pass in an increment */
    public static LSID getNextTaskLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String requestedLSID) 
    throws java.rmi.RemoteException {
        return getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID, LsidVersion.Increment.next);
    }
    
    public static LSID getNextTaskLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String requestedLSID, final LsidVersion.Increment versionIncrement) 
    throws java.rmi.RemoteException {
        return getNextLsid(mgr, gpConfig, gpContext, TASK_NAMESPACE, requestedLSID, versionIncrement);
    }

    public static LSID getNextLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String namespace, final String requestedLSID)
    throws java.rmi.RemoteException {
        return getNextLsid(mgr, gpConfig, gpContext, namespace, requestedLSID, LsidVersion.Increment.next);
    }
    
    /**
     * 
     * @param mgr
     * @param gpConfig
     * @param gpContext
     * @param namespace is one of TASK_NAMESPACE or SUITE_NAMESPACE or <custom_namespace>
     * @param requestedLSID
     * @return
     */
    public static LSID getNextLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String namespace, final String requestedLSID, final LsidVersion.Increment versionIncrement)
    throws java.rmi.RemoteException {
        LSID taskLSID = null;
        if (requestedLSID != null && requestedLSID.length() > 0) {
            try {
                taskLSID = new LSID(requestedLSID);
            } 
            catch (MalformedURLException mue) {
                log.error("Invalid requestedLsid='"+requestedLSID+"', Create a new one from scratch!", mue);
            }
        }
        final String lsidAuthority=gpConfig.getLsidAuthority(gpContext);
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            if (taskLSID == null) {
                taskLSID = createNewID(mgr, gpConfig, gpContext, namespace, versionIncrement.initialVersion());
            } 
            else if (lsidAuthority.equalsIgnoreCase(taskLSID.getAuthority())) {
                taskLSID = getNextIDVersion(mgr, requestedLSID, versionIncrement);
            } 
            else {
                taskLSID = createNewID(mgr, gpConfig, gpContext, namespace, versionIncrement.initialVersion());
            }
        }
        catch (Throwable t) {
            // for debugging
            if (log.isDebugEnabled()) {
                log.debug("Error in getNextLsid for requestedLSID='"+requestedLSID+"', namespace='"+namespace+"'", t);
            }
            throw t;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return taskLSID;
    }

    /** @deprecated call {@link #createNewID(HibernateSessionManager, GpConfig, GpContext, String)} instead */
    public static LSID createNewID(final String namespace) throws OmnigeneException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return createNewID(mgr, gpConfig, gpContext, namespace);
    }

    /** @deprecated should pass in an initialVersion */
    public static LSID createNewID(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String namespace) 
    throws OmnigeneException
    {
        final String initialVersion = "1";
        return createNewID(mgr, gpConfig, gpContext, namespace, initialVersion);
    }

    public static LSID createNewID(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String namespace, final String initialVersion) 
    throws OmnigeneException
    {
        try {
            final String authority=gpConfig.getLsidAuthority(gpContext);
            final String identifier=getNextID(mgr, gpConfig, namespace);
            final LSID newLSID = new LSID(authority, namespace, identifier, initialVersion);
            return newLSID;
        } 
        catch (final DbException e) {
            log.error(e);
            throw new OmnigeneException("Unable to create new LSID, DbException: " + e.getLocalizedMessage());
        }
        catch (final MalformedURLException mue) {
            log.error(mue);
            throw new OmnigeneException("Unable to create new LSID: " + mue.getMessage());
        }
    }

    /**
     * Get the next unique task or suite LSID identifier from the database.
     * 
     * @param namespace is one of TASK_NAMESPACE or SUITE_NAMESPACE or <custom_namespace>
     * @return
     * @throws OmnigeneException
     */
    public static synchronized int getNextLSIDIdentifier(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) 
    throws DbException {
        final String sequenceName=getSequenceName(namespace);
        return HibernateUtil.getNextSequenceValue(mgr, gpConfig, sequenceName);
    }

    protected static String getSequenceName(final String namespace) {
        final String seqName;
        if (GPConstants.TASK_NAMESPACE.equals(namespace)) {
            seqName="lsid_identifier_seq";
        }
        else if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            seqName="lsid_suite_identifier_seq";
        }
        else {
            seqName=namespace+"_seq";
            log.warn("custom Namespace for LSID: "+namespace+", sequenceName=" + seqName);
        }
        return seqName;
    }

    /**
     * Get the next ID in the sequence from the DB
     * @param namespace
     * @return
     * @throws OmnigeneException
     */
    protected static synchronized String getNextID(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) 
    throws DbException {
        int nextId = getNextLSIDIdentifier(mgr, gpConfig, namespace);
        return "" + nextId;
    }

    /**
     * Get the next available LSID version for a given identifier from the
     * database
     * 
     * @throws OmnigeneException
     * @throws RemoteException
     * @return int next version in sequence
     */
    protected static synchronized String getNextLSIDVersion(final HibernateSessionManager mgr, final LSID lsid) throws OmnigeneException {
        return getNextLSIDVersion(mgr, lsid, LsidVersion.Increment.next);
    }
    protected static synchronized String getNextLSIDVersion(final HibernateSessionManager mgr, final LSID lsid, final LsidVersion.Increment versionIncrement) throws OmnigeneException {
        String namespace = lsid.getNamespace();
        if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return getDS(mgr).getNextSuiteLSIDVersion(lsid);
        }
        else {
            return getDS(mgr).getNextTaskLSIDVersion(lsid, versionIncrement);
        }
    }

    protected static LSID getNextIDVersion(final HibernateSessionManager mgr, final String lsidString, final LsidVersion.Increment versionIncrement)
    throws OmnigeneException, RemoteException {
        try {
            final LSID lsid = new LSID(lsidString);
            final LSID nextLsid = getNextIDVersion(mgr, lsid, versionIncrement);
            return nextLsid;
        } 
        catch (MalformedURLException mue) {
            log.error("Error getNextIDVersion for id="+lsidString, mue);
            return null;
        }
    }

    /**
     * Increment lsid version to next unique version in the database.
     * @param lsid
     * @return the same lsid instance with a modified version
     * @throws MalformedURLException
     */
    protected static LSID getNextIDVersion(final HibernateSessionManager mgr, final LSID lsid, final LsidVersion.Increment versionIncrement) 
    throws OmnigeneException, MalformedURLException {
        final String lsidVersion=getNextLSIDVersion(mgr, lsid, versionIncrement);
        lsid.setVersion(lsidVersion);
        return lsid;
    }

    /** @deprecated call {@link LSIDUtil#getAuthorityType(GpConfig, GpContext, LSID)} instead */
    public static String getAuthorityType(final LSID lsid) {
        return LSIDUtil.getAuthorityType(lsid);
    }

    /** @deprecated call {@link LSIDUtil#getNearerLSID(GpConfig, GpContext, LSID, LSID)} instead */
    public static LSID getNearerLSID(LSID lsid1, LSID lsid2) {
        return LSIDUtil.getNearerLSID(lsid1, lsid2); // equal???
    }

    protected static AnalysisDAO getDS(final HibernateSessionManager mgr) throws OmnigeneException {
        AnalysisDAO ds;
        try {
            ds = new AnalysisDAO(mgr);
            return ds;
        } catch (Exception e) {
            throw new OmnigeneException(
                    "Unable to find analysisJobDataSource: " + e.getMessage());
        }
    }

}
