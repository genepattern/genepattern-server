function parameters = getMethodParameters(obj, methodNameOrId)
% get the default parameters for a method
% Creates a structure with name and value for each
% parameter initialized with the default values
% allowing the user to fill in the values
% they want to execute on

method = getMethod(obj, methodNameOrId);

paramInfo = method.getTaskInfo.getParameterInfoArray;

pcount = length(paramInfo);
parameters = [];
for i=1:pcount
    param = paramInfo(i);
    nom = char(param.getName());
    attrs = param.getAttributes;
    defaultValue = attrs.get('default_value');
    modNom = strrep(nom,'.','_');
    parameters.(modNom)= defaultValue;
end

