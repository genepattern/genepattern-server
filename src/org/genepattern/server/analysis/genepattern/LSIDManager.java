package org.genepattern.server.analysis.genepattern;

import java.util.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.genepattern.server.analysis.ejb.AnalysisJobDataSource;
import org.genepattern.server.util.BeanReference;
import org.genepattern.server.util.OmnigeneException;
import org.genepattern.util.LSID;

import java.net.MalformedURLException;

import java.rmi.RemoteException;

/**
 * manage LSID creation and versioning (initially for the repository modules).
 * This manager needs to be able to create new IDs as well as version ids and ensure
 * that a particular ID and version is never given out more than once
 */
public class LSIDManager {
	public static String AUTHORITY_MINE = "mine";
	public static String AUTHORITY_BROAD = "broad"; 
	public static String AUTHORITY_FOREIGN = "foreign";
	public static String BROAD_AUTHORITY = "broad.mit.edu";

	private static LSIDManager inst = null;
	
	private static String authority = "broad-cancer-genomics";
	private static String namespace = "genepatternmodules";
	private static String initialVersion = "1";


	private LSIDManager(){
		String auth = System.getProperty("lsid.authority");
		if (auth != null) {
			authority = auth;
		}
	}


	public static LSIDManager getInstance(){
		if (inst == null){
			inst = new LSIDManager();
		}
		return inst;
	}	
	
	public String getAuthority(){
		return authority;
	}
	public String getNamespace(){
		return namespace;
	}

	public LSID createNewID() throws OmnigeneException, RemoteException {
		try {
			LSID newLSID = new LSID(authority, namespace, getNextID(), initialVersion);
			return newLSID;		
		} catch (MalformedURLException mue){
			mue.printStackTrace();
			throw new OmnigeneException("Unable to create new LSID: " + mue.getMessage());
		}	
	}
	
	// get the next ID in the sequence from the DB
	protected synchronized String getNextID()throws OmnigeneException, RemoteException {
		AnalysisJobDataSource ds = getDS();
		int nextId = ds.getNextLSIDIdentifier();
		return ""+nextId;
	}

	// get the next version for a particular LSID identifier from the DB
	protected synchronized String getNextVersionFromDB(LSID lsid) throws OmnigeneException, RemoteException {
		AnalysisJobDataSource ds = getDS();
		String nextVersion = ds.getNextLSIDVersion(lsid);
		return nextVersion;
	}

	
	public LSID getNextIDVersion(String id) throws OmnigeneException, RemoteException {
		try {
			LSID anId = new LSID(id);
			LSID nextId = getNextIDVersion(anId);
			return nextId;
		} catch (MalformedURLException mue){
			mue.printStackTrace();
			return null;
		}
	}

	public LSID getNextIDVersion(LSID lsid) throws OmnigeneException, RemoteException, MalformedURLException {
		// go to DB and get the next version for this identifier
		lsid.setVersion(getNextVersionFromDB(lsid));
		return lsid;		
	}

	public String getAuthorityType(LSID lsid) {
		String authorityType;
		if (lsid == null) {
			authorityType = AUTHORITY_MINE;
		} else {
			String lsidAuthority = lsid.getAuthority();
			if (lsidAuthority.equals(authority)) {
				authorityType = AUTHORITY_MINE;
			} else if (lsidAuthority.equals(BROAD_AUTHORITY)) {
				authorityType = AUTHORITY_BROAD;
			} else {
				authorityType = AUTHORITY_FOREIGN;
			}
		}
		return authorityType;
	}

	// compare authority types: 1=lsid1 is closer, 0=equal, -1=lsid2 is closer
	// closer is defined as mine > Broad > foreign
	public int compareAuthorities(LSID lsid1, LSID lsid2) {
		String at1 = getAuthorityType(lsid1);
		String at2 = getAuthorityType(lsid2);
		if (!at1.equals(at2)) {
			if (at1.equals(AUTHORITY_MINE)) return 1;
			if (at2.equals(AUTHORITY_MINE)) return -1;
			if (at1.equals(AUTHORITY_BROAD)) return 1;
			return -1;
		} else {
			return 0;
		}
	}

	public LSID getNearerLSID(LSID lsid1, LSID lsid2) {
		int authorityComparison = compareAuthorities(lsid1, lsid2);
		if (authorityComparison < 0) return lsid2;
		if (authorityComparison > 0) {
			// closer authority than lsid2.getAuthority()
			return lsid1;
		}
		// same authority, check identifier
		int identifierComparison = lsid1.getIdentifier().compareTo(lsid2.getIdentifier());
		if (identifierComparison < 0) return lsid2;
		if (identifierComparison > 0) {
			// greater identifier than lsid2.getIdentifier()
			return lsid1;
		}
		// same authority and identifier, check version
		int versionComparison = lsid1.compareTo(lsid2);
		if (versionComparison < 0) return lsid2;
		if (versionComparison > 0) {
			// later version than lsid2.getVersion()
			return lsid1;
		}
		return lsid1; // equal???
	}


    protected AnalysisJobDataSource getDS() throws OmnigeneException {
        AnalysisJobDataSource ds;
        try{
            ds = BeanReference.getAnalysisJobDataSourceEJB();
	    return ds;
        }
        catch(Exception e){
            throw new OmnigeneException("Unable to find analysisJobDataSource: "+e.getMessage());
        }
    }
	

}