function describeMethod(obj, methodNameOrId,  showEmptyAttrs)
% describe a GenePattern method
% echo out the description, parameter names 
% and parameter types
%
%
method = getMethod(obj, methodNameOrId);

paramInfo = method.getTaskInfo.getParameterInfoArray;

methodName = char(method.getTaskInfo.getName());
methodId = char(method.getTaskInfo().getTaskInfoAttributes().get('LSID'));

methDesc = ['GenePattern Method:\t\t' methodName '\n\n'];
methDesc = strcat(methDesc, 'LSID:\t\t\t', methodId, '\n\n');

str = char(method.getTaskInfo.getDescription);
methDesc = strcat(methDesc, 'Description:\t', str, '\n\nParameters:\n\n');
pcount = length(paramInfo);

if (~exist('showEmptyAttrs'))
    showEmptyAttrs = false;
end

for i=1:pcount
    param = paramInfo(i);
    nom = char(param.getName());
    desc = char(param.getDescription);
    attrs = param.getAttributes;

    methDesc = strcat(methDesc, '\t', nom,'\n');
    methDesc = strcat(methDesc, '\t\t',desc,'\n');
    
    iter = attrs.keySet().iterator();
    for i=0:attrs.size()-1
        key = iter.next();
        val = attrs.get(key);
        
        if ((length(val) > 0) || (showEmptyAttrs))
            methDesc = strcat(methDesc, '\t\t',key,'=',val, '\n');
        end
    end    
end

sprintf(methDesc)