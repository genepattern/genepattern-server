/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.TASK_NAMESPACE;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.OmnigeneException;

/**
 * manage LSID creation and versioning (initially for the repository modules).
 * This manager needs to be able to create new IDs as well as version ids and
 * ensure that a particular ID and version is never given out more than once
 */
public class LSIDManager {
    private static final Logger log = Logger.getLogger(LSIDManager.class);
    
	private static final String initialVersion = "1";

    private LSIDManager() {
    }

	/** @deprecated pass in a HibernateSession */
    public static LSID getNextTaskLsid(final String requestedLSID)  throws java.rmi.RemoteException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return getNextTaskLsid(mgr, gpConfig, gpContext, requestedLSID);
    }

    public static LSID getNextTaskLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String requestedLSID) 
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
        if (taskLSID == null) {
            taskLSID = createNewID(mgr, gpConfig, gpContext, TASK_NAMESPACE);
        } 
        else if (lsidAuthority.equalsIgnoreCase(taskLSID.getAuthority())) {
            taskLSID = getNextIDVersion(mgr, requestedLSID);
        } 
        else {
            taskLSID = createNewID(mgr, gpConfig, gpContext, TASK_NAMESPACE);
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

    public static LSID createNewID(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, final String namespace) {
		try {
		    final String authority=gpConfig.getLsidAuthority(gpContext);
		    final LSID newLSID = new LSID(authority, namespace,
					getNextID(mgr, gpConfig, namespace), initialVersion);
			return newLSID;
		} 
		catch (final MalformedURLException mue) {
		    log.error(mue);
			throw new OmnigeneException("Unable to create new LSID: " + mue.getMessage());
		}
	}

	/**
	 * Get the next unique task or suite LSID identifier from the database.
	 * 
	 * @param namespace is one of TASK_NAMESPACE or SUITE_NAMESPACE
	 * @return
	 * @throws OmnigeneException
	 */
    private static synchronized int getNextLSIDIdentifier(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) throws OmnigeneException {
        final String sequenceName=getSequenceName(namespace);
        try {
            return HibernateUtil.getNextSequenceValue(mgr, gpConfig, sequenceName);
        }
        catch (Throwable t) {
            log.error(t);
            throw new OmnigeneException(""+t.getLocalizedMessage());
        }
    }

    protected static String getSequenceName(final String namespace) {
        if (GPConstants.TASK_NAMESPACE.equals(namespace)) {
            return "lsid_identifier_seq";
        }
        else if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return "lsid_suite_identifier_seq";
        }
        else {
            throw new OmnigeneException("unknown Namespace for LSID: " + namespace);
        }
    }

	/**
	 * Get the next ID in the sequence from the DB
	 * @param namespace
	 * @return
	 * @throws OmnigeneException
	 */
	protected static synchronized String getNextID(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) throws OmnigeneException {
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
        String namespace = lsid.getNamespace();
        if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return getDS(mgr).getNextSuiteLSIDVersion(lsid);
        }
        else {
            return getDS(mgr).getNextTaskLSIDVersion(lsid);
        }
    }

    protected static LSID getNextIDVersion(final HibernateSessionManager mgr, final String id) throws OmnigeneException,
            RemoteException {
        try {
            LSID anId = new LSID(id);
            LSID nextId = getNextIDVersion(mgr, anId);
            return nextId;
        } 
        catch (MalformedURLException mue) {
            log.error("Error getNextIDVersion for id="+id, mue);
            return null;
        }
    }

	/**
	 * Increment lsid version to next unique version in the database.
	 * @param lsid
	 * @return the same lsid instance with a modified version
	 * @throws MalformedURLException
	 */
	protected static LSID getNextIDVersion(final HibernateSessionManager mgr, final LSID lsid) throws OmnigeneException, 
	            MalformedURLException {
	    String lsidVersion=getNextLSIDVersion(mgr, lsid);
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
