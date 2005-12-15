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

public class AbstractTypedGPGEMessage extends AbstractGPGEMessage {
	private int type;
	
	public AbstractTypedGPGEMessage(Object source, int type) {
		super(source);
		this.type = type;
	}
	
	public int getType() {
		return type;
	}

}
