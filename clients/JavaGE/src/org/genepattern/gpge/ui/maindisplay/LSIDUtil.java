package org.genepattern.gpge.ui.maindisplay;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

/**
 *@author    Joshua Gould
 */
public class LSIDUtil {
	private LSIDUtil() { }


	public static boolean isBroadTask(LSID lsid) {
		String authority = lsid.getAuthority();
		return "broad.mit.edu".equals(authority);
	}
	
	public static String getTaskId(TaskInfo task) {
		String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
		return lsid!=null?lsid:task.getName();
	}


   public static String getTaskString(TaskInfo task, boolean includeVersion) {
     return getTaskString(task, includeVersion, true);
   }
   
	public static String getTaskString(TaskInfo task, boolean includeVersion, boolean includeAuthority) {
		String taskName = task.getName();
		String lsidString = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
		if(lsidString != null) {
			try {
				org.genepattern.util.LSID lsid = new org.genepattern.util.LSID(lsidString);
			
				if(includeAuthority && !LSIDUtil.isBroadTask(lsid)) {
					String authority = lsid.getAuthority();
					taskName += " (" + authority + ")";
				}
				
				if(includeVersion) {
					taskName += ", version " + lsid.getVersion();
				}
			} catch(Exception e) {}
		}
		return taskName;
	}
	
}
