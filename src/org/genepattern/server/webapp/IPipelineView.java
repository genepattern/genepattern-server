package org.genepattern.server.webapp;


import java.util.Collection;

import org.genepattern.analysis.TaskInfo;

public interface IPipelineView {
	public void init(Collection tmTasks, String userID);
	public void onSubmit();
	public void onCancel();
	public void begin();
	public void generateTask(TaskInfo taskInfo);
	public void end();
}