/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.TASK_NAMESPACE;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
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
    
    private static LSIDManager inst = null;
	private static final LSIDUtil lsidUtil = LSIDUtil.getInstance();
	private static final String initialVersion = "1";

	/** @deprecated pass in a HibernateSession */
    public static LSID getNextTaskLsid(final String requestedLSID)  throws java.rmi.RemoteException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        return getNextTaskLsid(mgr, gpConfig, requestedLSID);
    }

    public static LSID getNextTaskLsid(final HibernateSessionManager mgr, final GpConfig gpConfig, final String requestedLSID) throws java.rmi.RemoteException {
        LSID taskLSID = null;
        if (requestedLSID != null && requestedLSID.length() > 0) {
            try {
                taskLSID = new LSID(requestedLSID);
            } 
            catch (MalformedURLException mue) {
                log.error("Invalid requestedLsid='"+requestedLSID+"', Create a new one from scratch!", mue);
            }
        }
        final LSIDManager lsidManager = LSIDManager.getInstance();
        if (taskLSID == null) {
            taskLSID = lsidManager.createNewID(mgr, gpConfig, TASK_NAMESPACE);
        } 
        else if (lsidManager.getAuthority().equalsIgnoreCase(taskLSID.getAuthority())) {
            taskLSID = lsidManager.getNextIDVersion(mgr, requestedLSID);
        } 
        else {
            taskLSID = lsidManager.createNewID(mgr, gpConfig, TASK_NAMESPACE);
        }
        return taskLSID;
    }

    public static LSIDManager getInstance() {
        if (inst == null) {
            inst = new LSIDManager();
        }
        return inst;
    }

	private LSIDManager() {
	}

	public String getAuthority() {
		return lsidUtil.getAuthority();
	}
	
	/** @deprecated pass in a HibernateSession */
    public LSID createNewID(final String namespace) throws OmnigeneException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        return createNewID(mgr, gpConfig, namespace);
    }

    public LSID createNewID(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) {
		try {
		    final LSID newLSID = new LSID(getAuthority(), namespace,
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
	protected synchronized String getNextID(final HibernateSessionManager mgr, final GpConfig gpConfig, final String namespace) throws OmnigeneException {
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

    protected LSID getNextIDVersion(final HibernateSessionManager mgr, final String id) throws OmnigeneException,
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
	protected LSID getNextIDVersion(final HibernateSessionManager mgr, final LSID lsid) throws OmnigeneException, 
	            MalformedURLException {
	    String lsidVersion=getNextLSIDVersion(mgr, lsid);
	    lsid.setVersion(lsidVersion);
	    return lsid;
	}

	public String getAuthorityType(LSID lsid) {
		return lsidUtil.getAuthorityType(lsid);
	}

	/**
	 * Compare authority types: 1=lsid1 is closer, 0=equal, -1=lsid2 is closer
	 * closer is defined as mine > Broad > foreign
	 * 
	 * @param lsid1
	 * @param lsid2
	 * @return
	 */
	public int compareAuthorities(final LSID lsid1, final LSID lsid2) {
		return lsidUtil.compareAuthorities(lsid1, lsid2);
	}

	public LSID getNearerLSID(LSID lsid1, LSID lsid2) {
		return lsidUtil.getNearerLSID(lsid1, lsid2); // equal???
	}

	/** @deprecated */
    protected static AnalysisDAO getDS() throws OmnigeneException {
        final HibernateSessionManager mgr=HibernateUtil.instance();
        return getDS(mgr);
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
