function jobResult = checkCompletion(obj, jobNumber)


javaJobResult = obj.javaInstance.checkCompletion(str2num(jobNumber));


if (isempty(javaJobResult)) 
	jobResult = [];
	return;
end

jobResult.javaInstance = javaJobResult;

localWorkingDir = pwd;
javaJobResult.downloadFiles(localWorkingDir);
jobResult.fileNames = char(javaJobResult.getOutputFileNames);
jobResult.hasStandardOut = javaJobResult.hasStandardOut;
jobResult.hasStandardError = javaJobResult.hasStandardError;

[num, wid] = size(jobResult.fileNames);
for i=1:num
	fileUrl = char( jobResult.javaInstance.getURL(i-1).toString() );

	jobResult.fileURLs{i} = fileUrl;
end 