/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.TASK_NAMESPACE;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
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
	private static LSIDUtil lsidUtil = LSIDUtil.getInstance();
	private static String initialVersion = "1";

    public static LSID getNextTaskLsid(final String requestedLSID) throws java.rmi.RemoteException {
        LSID taskLSID = null;
        if (requestedLSID != null && requestedLSID.length() > 0) {
            try {
                taskLSID = new LSID(requestedLSID);
            } 
            catch (MalformedURLException mue) {
                log.error("Invalid requestedLsid='"+requestedLSID+"', Create a new one from scratch!", mue);
            }
        }
        LSIDManager lsidManager = LSIDManager.getInstance();
        if (taskLSID == null) {
            taskLSID = lsidManager.createNewID(TASK_NAMESPACE);
        } 
        else if (lsidManager.getAuthority().equalsIgnoreCase(taskLSID.getAuthority())) {
            taskLSID = lsidManager.getNextIDVersion(requestedLSID);
        } 
        else {
            taskLSID = lsidManager.createNewID(TASK_NAMESPACE);
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
	
	public LSID createNewID(final String namespace) throws OmnigeneException {
		try {
		    final LSID newLSID = new LSID(getAuthority(), namespace,
					getNextID(namespace), initialVersion);
			return newLSID;
		} 
		catch (MalformedURLException mue) {
			mue.printStackTrace();
			throw new OmnigeneException("Unable to create new LSID: " + mue.getMessage());
		}
	}

    private static synchronized int getNextLSIDIdentifier(String namespace) throws OmnigeneException {
        if (GPConstants.TASK_NAMESPACE.equals(namespace)) {
            return getDS().getNextTaskLSIDIdentifier();
        }
        else if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return getDS().getNextSuiteLSIDIdentifier();
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
	protected synchronized String getNextID(String namespace) throws OmnigeneException {
		int nextId = getNextLSIDIdentifier(namespace);
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
    protected static synchronized String getNextLSIDVersion(LSID lsid) throws OmnigeneException {
        String namespace = lsid.getNamespace();
        if (GPConstants.SUITE_NAMESPACE.equals(namespace)) {
            return getDS().getNextSuiteLSIDVersion(lsid);
        }
        else {
            return getDS().getNextTaskLSIDVersion(lsid);
        }
    }

	public LSID getNextIDVersion(String id) throws OmnigeneException,
			RemoteException {
		try {
			LSID anId = new LSID(id);
			LSID nextId = getNextIDVersion(anId);
			return nextId;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
			return null;
		}
	}

	public LSID getNextIDVersion(LSID lsid) throws OmnigeneException,
			 MalformedURLException {
		// go to DB and get the next version for this identifier
		lsid.setVersion(getNextLSIDVersion(lsid));
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

	protected static AnalysisDAO getDS() throws OmnigeneException {
        AnalysisDAO ds;
		try {
			ds = new AnalysisDAO();
			return ds;
		} catch (Exception e) {
			throw new OmnigeneException(
					"Unable to find analysisJobDataSource: " + e.getMessage());
		}
	}

}
