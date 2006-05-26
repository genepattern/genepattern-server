function jobNumber = runAnalysisNoWait(obj, taskName, params)
% Execute an analysis on a remote GenePattern server
% automatically retrieves the output files from
% the analysis to the current working directory (pwd)
%
ar = createParams(obj, taskName, params);


meth = obj.methods.(taskName);
type = char(meth.getTaskInfo().getTaskInfoAttributes().get('taskType'));
if (strcmp('Visualizer', type))
	jobResult= [];
	obj.javaInstance.runVisualizer(taskName,ar);
	return;
end
	

jobNumber = obj.javaInstance.runAnalysisNoWait(taskName,ar);



