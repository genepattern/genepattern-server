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


package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.EventObject;

/**
 * An event that indicates a pipeline was changed
 * 
 * @author jgould
 * 
 */
public class PipelineEvent extends EventObject {

	public static final int DELETE = 0;
	public static final int INSERT = 1;
	public static final int MOVE = 2;
	public static final int REPLACE = 3;
	
	private int type;
	private int firstRow;
	private int lastRow;
	
	public PipelineEvent(Object source, int type, int row) {
		super(source);
		this.type = type;
		this.firstRow = row;
		this.lastRow = row;
	}
	
	public PipelineEvent(Object source, int type, int firstRow, int lastRow) {
		super(source);
		this.type = type;
		this.firstRow = firstRow;
		this.lastRow = lastRow;
	}
	

	public int getFirstRow() {
		return firstRow;
	}
	
	public int getLastRow() {
		return lastRow;
	}

	
	public int getType() {
		return type;
	}

	
}
