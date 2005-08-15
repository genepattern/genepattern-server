package org.genepattern.gpge.message;

import java.net.MalformedURLException;

import org.genepattern.util.LSID;

/**
 * A message that indicates a new task was installed on the server
 * 
 * @author Joshua Gould
 * 
 */
public class TaskInstallMessage extends AbstractGPGEMessage {

	private LSID lsid;

	public TaskInstallMessage(Object source, String lsid)
			throws MalformedURLException {
		super(source);
		this.lsid = new LSID(lsid);
	}

	public LSID getLsid() {
		return lsid;
	}

}
