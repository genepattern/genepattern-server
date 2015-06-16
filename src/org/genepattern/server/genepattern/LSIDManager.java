/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.genepattern;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

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


	private static LSIDManager inst = null;

	private static LSIDUtil lsidUtil = LSIDUtil.getInstance();

	private static String initialVersion = "1";

	private LSIDManager() {

	}

	public static LSIDManager getInstance() {
		if (inst == null) {
			inst = new LSIDManager();
		}
		return inst;
	}

	public String getAuthority() {
		return lsidUtil.getAuthority();
	}

	
	public LSID createNewID(String namespace) throws OmnigeneException {
		try {
			LSID newLSID = new LSID(getAuthority(), namespace,
					getNextID(namespace), initialVersion);
			return newLSID;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
			throw new OmnigeneException("Unable to create new LSID: "
					+ mue.getMessage());
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

	// get the next ID in the sequence from the DB
	protected synchronized String getNextID(String namespace) throws OmnigeneException {

	// XXX handle suites as well

		int nextId = getNextLSIDIdentifier(namespace);
		return "" + nextId;
	}

   
    
    /**
     * get the next available LSID version for a given identifer from the
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

	// compare authority types: 1=lsid1 is closer, 0=equal, -1=lsid2 is closer
	// closer is defined as mine > Broad > foreign
	public int compareAuthorities(LSID lsid1, LSID lsid2) {
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
