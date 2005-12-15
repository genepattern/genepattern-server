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

public class PreferenceChangeMessage extends AbstractTypedGPGEMessage {

	public static final int SHOW_PARAMETER_DESCRIPTIONS = 0;
	private boolean showDescriptions;
	
	public PreferenceChangeMessage(Object source, int type, boolean showDescriptions) {
		super(source, type);
		this.showDescriptions = showDescriptions;
	}
	
	public boolean getDescriptionsVisible() {
		return showDescriptions;
	}

}
