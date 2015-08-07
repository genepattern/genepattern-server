/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.util.GPConstants;

/**
 * @author Joshua Gould
 * @created May 3, 2004
 */
public abstract class TaskExecutor {

	public final static String leftDelimiter = "<";

	public final static String rightDelimiter = ">";

	public final static String ANY = "any";

	static boolean DEBUG = false;

	public final static String PARAM_INFO_STRING = "string";

	public final static String PARAM_INFO_CHECKBOX = "checkbox";

	public final static String[] PARAM_INFO_OPTIONAL = { "optional",
			PARAM_INFO_CHECKBOX };

	public final static String[] PARAM_INFO_DEFAULT_VALUE = { "default_value",
			PARAM_INFO_STRING };

	protected String userName;

	protected TaskInfo taskInfo;

	protected Map substitutions;

	public TaskExecutor(TaskInfo taskInfo, Map substitutions, String username) {
		this.taskInfo = taskInfo;
		this.substitutions = substitutions;
		this.userName = username;
	}

	/**
	 * @param taskInfo
	 *            Description of the Parameter
	 * @param paramName2ValueMap
	 *            Description of the Parameter
	 * @return the new command line
	 * @exception IOException
	 *                Description of the Exception
	 */
	protected String[] doCommandLineSubstitutions(TaskInfo taskInfo,
			Map paramName2ValueMap) throws IOException {
		// do input argument substitution in command line
		TaskInfoAttributes taskInfoAttributes = taskInfo
				.giveTaskInfoAttributes();
		String commandLine = taskInfoAttributes.get(GPConstants.COMMAND_LINE);
		if (DEBUG) {
			System.out.println(commandLine);
		}
		int start = 0;
		int end;
		String argValue = null;
		String variableName;

		StringTokenizer stCmd = new StringTokenizer(commandLine, " ");
		String[] cmd = new String[stCmd.countTokens()];
		int c = 0;
		while (stCmd.hasMoreTokens()) {
			cmd[c++] = stCmd.nextToken();
		}

		// replace variables in the command line from System.properties and the
		// substitutions HashMap
		for (c = 0; c < cmd.length; c++) {
			for (start = cmd[c].indexOf(leftDelimiter, 0); start != -1; start = cmd[c]
					.indexOf(leftDelimiter, start)) {
				end = cmd[c].indexOf(rightDelimiter, start);
				if (end == -1) {
					break;
				}

				variableName = cmd[c].substring(start + leftDelimiter.length(),
						end);
				argValue = System.getProperty(variableName);
				if (argValue == null) {
					argValue = (String) substitutions.get(variableName);
				}
				if (argValue != null) {
					cmd[c] = replace(cmd[c], cmd[c].substring(start, end
							+ rightDelimiter.length()), argValue);
				} else {
					System.err.println("Unable to find substitution for "
							+ variableName);
					start = end + rightDelimiter.length();
				}
			}
		}
		ArrayList temp = new ArrayList();
		for (int i = 0; i < cmd.length; i++) {
			if (!"".equals(cmd[i])) {
				temp.add(cmd[i]);
			}
		}
		return (String[]) temp.toArray(new String[0]);
	}

	/**
	 * replace all instances of "find" in "original" string and substitute
	 * "replace" for them
	 * 
	 * @param original
	 *            String before replacements are made
	 * @param find
	 *            String to search for
	 * @param replace
	 *            String to replace the sought string with
	 * @return String String with all replacements made
	 * @author Jim Lerner
	 */
	protected final static String replace(String original, String find,
			String replace) {
		StringBuffer res = new StringBuffer();
		int idx = 0;
		int i = 0;
		while (true) {
			i = idx;
			idx = original.indexOf(find, idx);
			if (idx == -1) {
				res.append(original.substring(i));
				break;
			} else {
				res.append(original.substring(i, idx));
				res.append(replace);
				idx += find.length();
			}
		}
		return res.toString();
	}

	public abstract void beforeExec() throws TaskExecException;

	public void exec() throws TaskExecException {
		beforeExec();
		ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
		TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
		if (!validateOS(tia.get(GPConstants.OS))) {
			throw new TaskExecException("Invalid OS");
		}
		if (!validateCPU(tia.get(GPConstants.CPU_TYPE))) {
			throw new TaskExecException("Invalid CPU");
		}
		String[] commandLine = null;

		try {
			commandLine = doCommandLineSubstitutions(taskInfo, substitutions);
		} catch (java.io.IOException e) {
			throw new TaskExecException(e);
		}
		if (DEBUG) {
			System.out.println(java.util.Arrays.asList(commandLine));
		}

		final String[] _commandLine = commandLine;

      doExec(commandLine);
		

	}

	protected static boolean validateCPU(String expected) {
		String actual = System.getProperty("os.arch");
		// eg. "x86", "i386", "ppc", "alpha", "sparc"

		if (expected.equals("")) {
			return true;
		}
		if (expected.equals(ANY)) {
			return true;
		}
		if (expected.equalsIgnoreCase(actual)) {
			return true;
		}

		String intelEnding = "86"; // x86, i386, i586, etc.
		if (expected.endsWith(intelEnding) && actual.endsWith(intelEnding)) {
			return true;
		}
		return false;
		//		if(System.getProperty(GPConstants.COMMAND_PREFIX, null) != null) {
		// FIXME
		//			return true;
		//	} // don't validate for LSF

	}

	protected static boolean validateOS(String expected) {
		String actual = System.getProperty("os.name");
		// eg. "Windows XP", "Linux", "Mac OS X", "OSF1"

		if (expected.equals("")) {
			return true;
		}
		if (expected.equals(ANY)) {
			return true;
		}
		if (expected.equalsIgnoreCase(actual)) {
			return true;
		}

		String MicrosoftBeginning = "Windows"; // Windows XP, Windows ME,
											   // Windows XP, Windows 2000, etc.
		if (expected.startsWith(MicrosoftBeginning)
				&& actual.startsWith(MicrosoftBeginning)) {
			return true;
		}

		//	if(System.getProperty(GPConstants.COMMAND_PREFIX, null) != null) {
		// FIXME
		//		return true;
		//	} // don't validate for LSF

		return false;
	}

	protected abstract void startOutputStreamThread(Process p);

	protected abstract void startErrorStreamThread(Process p);

	private void doExec(final String[] commandLine) {
      new Thread() {
         public void run() {
            try {
               final Process p = Runtime.getRuntime().exec(commandLine);
               startOutputStreamThread(p);
               startErrorStreamThread(p);
               p.waitFor();
            } catch(Exception e){
               e.printStackTrace();
            }
         }
      }.start();
	}

	static {
		if ("true".equals(System
				.getProperty("edu.mit.genome.gp.ui.analysis.debug"))) {
			DEBUG = true;
		}
	}
}
