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


package org.genepattern.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An abstract class for reading documents.
 * 
 * @author Joshua Gould
 */
public abstract class AbstractReader {
	List suffixes;

	String formatName;

	protected AbstractReader(String[] _suffixes, String _formatName) {
		suffixes = Collections.unmodifiableList(Arrays.asList(_suffixes));
		formatName = _formatName;
	}

	public String getFormatName() {
		return formatName;
	}

	public List getFileSuffixes() {
		return suffixes;
	}

}

