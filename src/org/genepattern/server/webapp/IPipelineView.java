/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


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
