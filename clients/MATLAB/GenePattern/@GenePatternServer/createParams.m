function ar = createParams(obj, methodNameOrId, params)
%
% private fun to turn a struct of params into a java array
%

method = getMethod(obj, methodNameOrId);

paramInfo = method.getTaskInfo.getParameterInfoArray;
pcount = length(paramInfo);
ar = javaArray('org.genepattern.webservice.Parameter',length(pcount));

idx=1;
for i=1:pcount
    param = paramInfo(i);
    
    nom = char(param.getName());
    modNom = strrep(nom,'.','_');
    attrs = param.getAttributes;
    defaultValue = attrs.get('default_value');


    values = param.getValue();
    if (not (isempty(values)))
	rem=char(values);
	if (not(isempty(rem)))

 	  while true
   		[t, rem] = strtok(rem, ';');
		
  		if isempty(t),  break;  end
   		eqidx = regexpi(t,'=');
		if (not (isempty(eqidx)))
			if (not(exist('paramValueMap')))
				paramValueMap = java.util.HashMap;
			end
		
			val = java.lang.String( t(1:eqidx-1) );
			key = java.lang.String( t(eqidx+1:length(t)));
			paramValueMap.put(key,val);
			paramValueMap.put(val,val);

		end %if	
	  end %while
	end %if 
    end %if 


    try
        value = params.(modNom);
    catch
        if (not(isempty(defaultValue)))
           value = defaultValue;
        else 
           optional = attrs.get('optional');
           if (not (strcmp(optional, 'on')))
               error(['Non-optional parameter ' modNom ' must be provided']);
           end
        end
    end
    


 % map choice lists
    if (exist('paramValueMap'))
	    if (not(isempty(paramValueMap)))
		v = java.lang.String(value);
		value = paramValueMap.get(v);
	    end 
    end	


   % if (~isempty(value))
        ar(idx) = org.genepattern.webservice.Parameter(nom,value);
        idx = idx+1;
 %   end
    clear paramValueMap
end

