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

pathLen = length(path);
extension = path(pathLen - 3:pathLen);
edc = org.genepattern.io.expr.ExpressionDataCreator();

if (extension == '.res')
    reader = org.genepattern.io.expr.res.ResReader();
elseif (extension == '.gct')
    reader = org.genepattern.io.expr.gct.GctReader();    
elseif (extension == '.odf')
    reader = org.genepattern.io.expr.odf.OdfDatasetReader();
else
   return 
end
edata = reader.read(path, edc);
edataset = edata.getExpressionMatrix;
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