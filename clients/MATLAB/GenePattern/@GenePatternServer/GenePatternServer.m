function obj=GenePatternServer(url, userID)

pathOK = initGenePatternPath;
if (~pathOK)
	disp('You must correct java classpath errors before creating a GenePatternServer instance');
	obj = [];
	return;
end


obj.url = url;
obj.userID = userID;
obj.javaInstance = [];
obj.latestModules = [];
obj.latestModulesByLSID = java.util.HashMap;
obj.allModules = [];
obj.methods = [];
obj = class(obj, 'GenePatternServer');


obj.javaInstance = org.genepattern.client.MatlabGPClient(url,userID);
obj.latestModules = obj.javaInstance.getLatestServices();
obj.allModules = obj.javaInstance.getServices();

% add a method by name for each of the latest
iter = obj.latestModules .keySet().iterator();

for i=0:obj.latestModules.size()-1
    taskId = iter.next();
    aTask = obj.latestModules.get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = strrep(char(taskInfo.getName()), '.', '_') ;
    eval(['obj.methods.' taskName '=aTask;']);
end

iter = obj.allModules .keySet().iterator();

for i=0:obj.allModules.size()-1
    taskId = iter.next();
    aTask = obj.latestModules.get(taskId); 

    [versionlessTaskId, ver] = stripVersion(obj, taskId);	

    altTask = obj.latestModulesByLSID.get(versionlessTaskId);
    if (~isempty(altTask))
      % make sure it is the latest version for this versionless id

	altLsid = altTask.getTaskInfo().getTaskInfoAttributes().get('LSID');
	[ignore, altVer] = stripVersion(obj, altLsid );	
 	if (ver > altVer) % this one is newer
		obj.latestModulesByLSID.put(versionlessTaskId , aTask);
	end

    else	
       obj.latestModulesByLSID.put(versionlessTaskId , aTask);
    end

end

