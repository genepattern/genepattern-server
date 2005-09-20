package org.genepattern.server.genepattern;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;
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

	public String getNamespace() {
		return lsidUtil.getNamespace();
	}

	public LSID createNewID() throws OmnigeneException, RemoteException {
		try {
			LSID newLSID = new LSID(getAuthority(), getNamespace(),
					getNextID(), initialVersion);
			return newLSID;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
			throw new OmnigeneException("Unable to create new LSID: "
					+ mue.getMessage());
		}
	}

	// get the next ID in the sequence from the DB
	protected synchronized String getNextID() throws OmnigeneException,
			RemoteException {
		AnalysisJobDataSource ds = getDS();
		int nextId = ds.getNextLSIDIdentifier();
		return "" + nextId;
	}

	// get the next version for a particular LSID identifier from the DB
	protected synchronized String getNextVersionFromDB(LSID lsid)
			throws OmnigeneException, RemoteException {
		AnalysisJobDataSource ds = getDS();
		String nextVersion = ds.getNextLSIDVersion(lsid);
		return nextVersion;
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
			RemoteException, MalformedURLException {
		// go to DB and get the next version for this identifier
		lsid.setVersion(getNextVersionFromDB(lsid));
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

	protected AnalysisJobDataSource getDS() throws OmnigeneException {
		AnalysisJobDataSource ds;
		try {
			ds = BeanReference.getAnalysisJobDataSourceEJB();
			return ds;
		} catch (Exception e) {
			throw new OmnigeneException(
					"Unable to find analysisJobDataSource: " + e.getMessage());
		}
	}

}