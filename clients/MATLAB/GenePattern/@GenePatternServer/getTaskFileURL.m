function url = getTaskFileURL(obj, pipelineName, fileName)
%
% returns the URL for the file saved with the pipeline
%
%

jUrl = obj.javaInstance.getTaskFileURL(pipelineName, fileName);

url = char(jUrl.toString());