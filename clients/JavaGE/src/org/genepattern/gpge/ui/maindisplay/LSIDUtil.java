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


package org.genepattern.gpge.ui.maindisplay;

import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

/**
 * @author Joshua Gould
 */
public class LSIDUtil {
	private LSIDUtil() {
	}

	public static boolean isBroadTask(LSID lsid) {
		String authority = lsid.getAuthority();
		return "broad.mit.edu".equals(authority);
	}

	public static String getTaskId(TaskInfo task) {
		String lsid = (String) task.getTaskInfoAttributes().get(
				GPConstants.LSID);
		return lsid != null ? lsid : task.getName();
	}

	public static String getTaskString(TaskInfo task, boolean includeVersion) {
		return getTaskString(task, includeVersion, true);
	}

	public static String getTaskString(TaskInfo task, boolean includeVersion,
			boolean includeAuthority) {
		String taskName = task.getName();
		String lsidString = (String) task.getTaskInfoAttributes().get(
				GPConstants.LSID);
		if (lsidString != null) {
			try {
				org.genepattern.util.LSID lsid = new org.genepattern.util.LSID(
						lsidString);

				if (includeAuthority && !LSIDUtil.isBroadTask(lsid)) {
					String authority = lsid.getAuthority();
					taskName += " (" + authority + ")";
				}

				if (includeVersion) {
					taskName += ", version " + lsid.getVersion();
				}
			} catch (Exception e) {
			}
		}
		return taskName;
	}

}