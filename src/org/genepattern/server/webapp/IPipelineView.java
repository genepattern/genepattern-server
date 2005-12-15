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


package org.genepattern.server.webapp;

import java.util.Collection;

import org.genepattern.webservice.TaskInfo;

public interface IPipelineView {
	public void init(Collection tmTasks, String userID);

	public void onSubmit();

	public void onCancel();

	public void begin();

	public void generateTask(TaskInfo taskInfo);

	public void end();
}