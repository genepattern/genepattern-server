/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

/**
 * Check for and create, if missing, the GenePattern server database.
 * 
 * @author Jim Lerner
 */

public class CreateDatabase extends CommandLineAction {
	/**
	 * args[0]: directory in which to find zip files assumes resources directory
	 * is at same level, containing genepattern.properties
	 */
	public static void main(String[] args) {
		CreateDatabase task = new CreateDatabase();
		task.run(args);
		return;
	}

	public void run(String[] args) {

		DEBUG = (System.getProperty("DEBUG") != null);
		try {

			// attempt to connect to the database, starting it if necessary,
			// and check for and create the schema, if necessary
			preRun(args);

			// shutdown the database if it was just started by preRun
			postRun(args);

		} catch (Throwable e) {
			System.err.println(e.getMessage() + " in CreateDatabase");
			e.printStackTrace();
			System.exit(1);
		}
	}
}

