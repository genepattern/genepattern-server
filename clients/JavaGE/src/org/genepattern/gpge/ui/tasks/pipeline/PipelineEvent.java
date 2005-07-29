package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.EventObject;

/**
 * An event that indicates a pipeline was changed
 * 
 * @author jgould
 * 
 */
public class PipelineEvent extends EventObject {

	public PipelineEvent(Object source) {
		super(source);
	}

}
