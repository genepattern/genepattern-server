package edu.mit.genome.gp.ui.analysis;

/**
 * Forwards main method to org.genepattern.server.webapp.RunPipeline
 * 
 * @author Joshua Gould
 */
public class RunPipeline {

	private RunPipeline() {
	}// prevent instantiation

	public static void main(String args[]) throws Exception {
		org.genepattern.server.webapp.RunPipeline.main(args);
	}
}

