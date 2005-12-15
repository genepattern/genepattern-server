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


package org.genepattern.server.util;

import java.io.IOException;
import java.rmi.RemoteException;

import org.genepattern.server.webservice.server.dao.AnalysisHypersonicDAO;
import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.webservice.OmnigeneException;

/**
 * This utility class is used for getting reference to bean.
 * 
 * @author Rajesh Kuttan
 * @version 1.0
 */

final public class BeanReference {

	static private void init() throws OmnigeneException, IOException {
		if (false)
			throw new RemoteException();
		if (false)
			throw new IOException();
	}

	protected static AnalysisJobDataSource ds = null;

	public static AnalysisJobDataSource getAnalysisJobDataSourceEJB()
			throws RemoteException, OmnigeneException {

		try {

			init();
			if (ds == null) {
				ds = new AnalysisHypersonicDAO();
				//AnalysisManager.getInstance();
			}
			return ds;

		} catch (RemoteException remoteEx) {
			throw remoteEx;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new OmnigeneException(ex.toString());
		}

	}

	public static void main(String args[]) {
	}

}