function jobResult = runAnalysis(obj, taskName, params, lsid)
% Execute an analysis on a remote GenePattern server
% automatically retrieves the output files from
% the analysis to the current working directory (pwd)
%


if (exist('lsid'))
    identifier = lsid;
else  
    identifier = taskName;
end

ar = createParams(obj, identifier, params);
method = getMethod(obj, identifier);


type = char(method.getTaskInfo().getTaskInfoAttributes().get('taskType'));
if (strcmp('Visualizer', type))
	jobResult= [];
	obj.javaInstance.runVisualizer(identifier,ar);
	return;
end
	
javaJobResult = obj.javaInstance.runAnalysis(identifier,ar);



jobResult.jobId = javaJobResult.getJobNumber();
jobResult.javaInstance = javaJobResult;

localWorkingDir = pwd;
jobResult.fileNames = javaJobResult.downloadFiles(localWorkingDir, false);
%  jobResult.fileNames = char(javaJobResult.getOutputFileNames);
jobResult.hasStandardOut = javaJobResult.hasStandardOut;
jobResult.hasStandardError = javaJobResult.hasStandardError;

[num, wid] = size(jobResult.fileNames);
if (jobResult.hasStandardOut) 
       num = num -1;
end
if (jobResult.hasStandardError) 
    num = num -1;
end

for i=1:num
	fileUrl = char( jobResult.javaInstance.getURL(i-1).toString() );

	jobResult.fileURLs{i} = fileUrl;
end 