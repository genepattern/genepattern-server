function result=subsref(gp,s)
%
% overload the '.' operator to allow a user to call a
% GenePattern method using syntax like the following
%
%  gp.ExtractColumnNames(params)
%
% gp is a GenePatternServer 
% s(1) is the method name
% s(2) is the params
%

if (length(s) ~= 2)
    error('You must provide a parameter structure to the method')
    return
end
if (isempty(s(2).subs))
    error('You must provide a parameter structure to the method')
    return;
end

methodName = s(1).subs;
params = s(2).subs{:};
result = runAnalysis(gp,methodName,params);