function listMethods(obj)


allTasks = obj.latestModules;

iter = allTasks.keySet().iterator();

s=['The available methods on GenePatternServer(' obj.url ') are\n\n'];

maxNameLen = 0;
for i=0:allTasks.size()-1
    taskId = iter.next();
    aTask = obj.latestModules.get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = char(taskInfo.getName());
    maxNameLen = max(maxNameLen, length(taskName));
end

iter = allTasks.keySet().iterator();
for i=0:allTasks.size()-1
    taskId = iter.next();
    aTask = obj.latestModules.get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = char(taskInfo.getName());

    aTask = allTasks.get(taskId);
    desc = char(taskInfo.getDescription);
    
    s= strcat(s, '\t', taskName,'\t');
    
    nameLength = length(taskName);
    for j=1:4:(maxNameLen-nameLength)
        s=strcat(s,'\t');
    end
   
    s = strcat( s, desc , '\n');
end
sprintf(s)