/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
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
