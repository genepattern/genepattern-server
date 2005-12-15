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

