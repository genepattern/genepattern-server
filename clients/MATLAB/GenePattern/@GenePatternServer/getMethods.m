function taskArray = getMethods(obj)


allTasks = obj.allModules;

iter = allTasks.keySet().iterator();
taskArray = [];

for i=0:allTasks.size()-1
    taskId = iter.next();
    aTask = allTasks.get(taskId);
    taskInfo = aTask.getTaskInfo();
    taskName = char(taskInfo.getName());

    task.lsid = char(taskId); 
    task.name = taskName;
    task.module = aTask;
    task.description = char(taskInfo.getDescription);
    
    taskArray = [taskArray; task];
end
