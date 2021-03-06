function url = getModuleFileURL(obj, pipelineName, fileName)
%
% returns the URL for the file saved with the pipeline
%
%

jUrl = obj.javaInstance.getModuleFileUrl(pipelineName, fileName);

url = char(jUrl.toString());