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
