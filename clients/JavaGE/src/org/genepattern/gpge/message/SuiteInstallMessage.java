package org.genepattern.gpge.message;

import java.net.MalformedURLException;

import org.genepattern.util.LSID;

/**
 * A message that indicates a new suite was installed on the server
 * 
 * @author Joshua Gould
 * 
 */
public class SuiteInstallMessage extends AbstractGPGEMessage {

	private LSID lsid;

	public SuiteInstallMessage(Object source, String lsid)
			throws MalformedURLException {
		super(source);
		this.lsid = new LSID(lsid);
	}

	public LSID getLsid() {
		return lsid;
	}

}
