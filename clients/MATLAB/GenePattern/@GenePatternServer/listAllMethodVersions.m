function listAllMethodVersions(obj)


allTasks = obj.allModules;

iter = allTasks.keySet().iterator();

s=['All available method versions on GenePatternServer(' obj.url ') are\n\n'];

maxNameLen = 0;
maxIdLen = 0;
for i=0:allTasks.size()-1
    taskId = iter.next();
    aTask = allTasks .get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = char(taskInfo.getName());
    maxNameLen = max(maxNameLen, length(taskName));
    maxIdLen = max(maxIdLen, length(taskId));
end

iter = allTasks.keySet().iterator();
for i=0:allTasks.size()-1
    taskId = iter.next();
    aTask = allTasks.get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = char(taskInfo.getName());

    aTask = allTasks.get(taskId);
    desc = char(taskInfo.getDescription);
    
    s= strcat(s, '\t', taskName,'\t');
    
    nameLength = length(taskName);
    for j=1:4:(maxNameLen-nameLength)
        s=strcat(s,'\t');
    end

    s= strcat(s, taskId,'\t');
    
    idLength = length(taskId);
    for j=1:4:(maxIdLen-idLength)
        s=strcat(s,'\t');
    end

    s = strcat( s, desc , '\n');
end
sprintf(s)