function method = getMethod(obj, methodNameOrId)
%
% private fun to turn a struct of params into a java array
%
if (isLSID(obj, methodNameOrId))
    method = obj.allModules.get(methodNameOrId);

    if (isempty(method))
    	method = obj.latestModulesByLSID.get(methodNameOrId);
    end
else 
    method = obj.methods.(methodNameOrId);
end

