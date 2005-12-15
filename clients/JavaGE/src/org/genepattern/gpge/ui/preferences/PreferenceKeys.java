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


package org.genepattern.gpge.ui.preferences;

/**
 * Keys for the properties stored in ~/gp/gp.properties
 * 
 * @author Joshua Gould
 */
public class PreferenceKeys {

	public final static String USER_NAME = "gp.user.name";

	public final static String SERVER = "gp.server";

	public final static String SHOW_JOB_COMPLETED_DIALOG = "show.job.completed.dialog";

	public final static String PROJECT_DIRS = "gp.project.dirs";

	public final static String MAIL_HELP_ADDRESS = "gp.mail.helpAddress";

	public final static String MAIL_HOST = "gp.mail.host";
   
	public final static String WINDOW_LAYOUT = "window.layout";
   
   public final static String AUTHORITY_MINE_COLOR = "authority.mine.color";
   public final static String AUTHORITY_FOREIGN_COLOR =
      "authority.foreign.color";
   public final static String AUTHORITY_BROAD_COLOR = "authority.broad.color";

   public final static String SHOW_PARAMETER_DESCRIPTIONS = "show.parameter.descriptions";
   
	private PreferenceKeys() {
	}

}