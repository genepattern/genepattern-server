
%
% Exampple of how we want to be able to use GP from MATLAB
%

% connect to a server
gp = GenePatternServer('http://localhost:5050')

%list its available method names
listMethods(gp)

% get a cell array with the methods as cellArray objects for low-level
% access
getMethods(gp)

%get info on one of its methods
%   echo description and parameter info to screen
describeMethod(gp, 'ExtractColumnNames')
% show all parameter attributes, even those with no value
describeMethod(gp,'ExtractColumnNames',true)


% perform an analysis on a file, preparing the params
% waiting for completion and then viewing the result files
% use the getMethodParameters method to get the parameter
% struct to use so you can see all param names and default values
params = getMethodParameters(gp, 'ExtractColumnNames');
params.input_filename='c:\smaller.res';
params.output='myColNames';
res = runAnalysis(gp, 'ExtractColumnNames', params);

% perform an analysis using a default for the output
% creating the struct for the params manually
clear params
params.input_filename='c:\smaller.res';
res = runAnalysis(gp, 'ExtractColumnNames', params);


% launch a visualizer on a file (local or remote)
p1.filename='c:\smaller.res';
p1.transpose='no';
runAnalysis(gp, 'HeatMapViewer', p1);

% perform an analysis on a matrix (automatically turning it
% into a file)


