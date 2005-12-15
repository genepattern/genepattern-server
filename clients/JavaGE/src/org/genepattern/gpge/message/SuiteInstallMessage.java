/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


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
