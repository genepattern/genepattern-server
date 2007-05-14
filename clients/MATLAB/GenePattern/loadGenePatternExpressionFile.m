function expressionDataset=loadGenePatternExpressionFile(path)
% Load a res, gct or odf file into a MATLAB structure
%
% Parameters
%   path    - full path to a res, gct or odf format expression file
%
% Return:  A MATLAB structure with the following elements
%   data                - M by N matrix of doubles
%   rowNames            - M*1 array of Strings
%   columnNames         - N*1 array of Strings
%   rowDescriptions     - M*1 array of strings
%   columnDescriptions  - unless loading from a gct file
%
global GenePatternPathSet
initGenePatternPath();

edata = org.genepattern.io.IOUtil.readDataset(path);
expressionDataset.data = edata.getArray();
[rowcount, colcount] = size(expressionDataset.data);

rowNames = edata.getRowNames();
rowDescriptions = edata.getRowDescriptions();

colNames = edata.getColumnNames();
if (not (extension == '.gct'))
    colDescriptions = edata.getColumnDescriptions();
end

expressionDataset.rowNames = cellstr(char(rowNames));
expressionDataset.columnNames = cellstr(char(colNames));
expressionDataset.rowDescriptions = cellstr(char(rowDescriptions));
if (not (extension == '.gct'))
    expressionDataset.columnDescriptions = cellstr(char(colDescriptions));
end