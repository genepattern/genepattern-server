/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

/**
 * install a set of zipped GenePattern tasks into the local GenePattern server.
 * create a new task database if necessary
 * 
 * @author Jim Lerner
 */

public class InstallTasks extends CommandLineAction implements FilenameFilter {
	static String authToExclude = null;

	/**
	 * args[0]: directory in which to find zip files assumes resources directory
	 * is at same level, containing genepattern.properties
	 */
	public static void main(String[] args) {
		authToExclude = System.getProperty("excludeTasksFromLSIDAuthority");
		System.out.println("\n\nEXCLUDE=" + authToExclude);
		InstallTasks installTasks = new InstallTasks();
		installTasks.run(args);
		return;
	}

	public boolean accept(File dir, String name) {
		boolean isZip = name.endsWith(".zip");
		if (!isZip)
			return false;
		if (authToExclude == null)
			return isZip;

		// filter further, look inside zip for manifest. read lsid and do not
		// accept
		// if it matches the authToExclude. This allows us (for example) to load
		// only modules not from broad at install time
		File zippy = new File(dir, name);
		try {
			Properties props = GenePatternAnalysisTask.getPropsFromZipFile(zippy.getCanonicalPath());

			String lsidStr = props.getProperty(GPConstants.LSID);
			if (lsidStr == null)
				return true;
			LSID lsid = new LSID(lsidStr);

			return !(lsid.getAuthority().startsWith(authToExclude));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	public void run(String[] args) {
		if (args.length < 1) {
			System.err
					.println("usage: InstallTasks [dir where modules are stored, peer to resources directory]");
			return;
		}

		DEBUG = (System.getProperty("DEBUG") != null);
		try {
			preRun(args);

			File moduleDir = new File(args[0]);
			File[] moduleList = moduleDir.listFiles(this);
			if (moduleList == null || moduleList.length == 0) {
				System.err
						.println("There are no modules meeting the criteria to install in "
								+ moduleDir.getCanonicalPath());
				return;
			}

			File module;
			for (int i = 0; i < moduleList.length; i++) {
				module = moduleList[i];
				System.out.println("Installing " + module.getName());
				String lsid = GenePatternAnalysisTask.installNewTask(module.getCanonicalPath(),
						"GenePattern", GPConstants.ACCESS_PUBLIC, null, null);
			}

		} catch (TaskInstallationException tie) {
			Vector vProblems = tie.getErrors();
			for (Enumeration eProblems = vProblems.elements(); eProblems
					.hasMoreElements();) {
				System.err.println(eProblems.nextElement());
			}
			System.err.println("");

		} catch (Throwable e) {
			System.err.println(e.getMessage() + " in InstallTasks");
			e.printStackTrace();
			System.exit(1);
		} finally {
			try {
				postRun(args);
			} catch (Exception e) {
			}
			System.exit(0);

		}
	}
}

