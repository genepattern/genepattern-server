package org.genepattern.gpge.ui.tasks.pipeline;

import java.util.EventListener;

/**
 * Listener for changes in a pipeline
 * 
 * @author jgould
 * 
 */
public interface PipelineListener extends EventListener {
	public void pipelineChanged(PipelineEvent e);
}
