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


package org.genepattern.server;

/**
 * <p>
 * Title: AnalysisServiceException.java
 * </p>
 * <p>
 * Description: Super Exception class for all analysis service exception.
 * </p>
 * 
 * @author Hui Gong
 * @version 1.0
 */

public class AnalysisServiceException extends Exception {

	public AnalysisServiceException() {
		super();
	}

	public AnalysisServiceException(String s) {
		super(s);
	}
}