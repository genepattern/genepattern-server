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
	
	private int type;
	private int row;
	
	public PipelineEvent(Object source) {
		super(source);
	}
	
	public PipelineEvent(Object source, int type, int row) {
		super(source);
		this.type = type;
		this.row = row;
	}

	public int getRow() {
		return row;
	}
	
	public int getType() {
		return type;
	}

}
