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
if (extension == '.res')
    reader = edu.mit.broad.io.microarray.ResReader();
elseif (extension == '.gct')
    reader = edu.mit.broad.io.microarray.GctReader();    
elseif (extension == '.odf')
    reader = edu.mit.broad.io.microarray.OdfDatasetReader();
else
   return 
end

edata = reader.read(path);
edataset = edata.getExpressionMatrix;
rowcount = edataset.getRowDimension;
colcount = edataset.getColumnDimension;

for i=0:rowcount-1
    rowNames(i+1) = edataset.getRowName(i); 
    rowDescriptions(i+1) = edata.getRowDescription(i); 
   
    for j=0:colcount-1
        if (i == 0)
            colNames(j+1) = edataset.getColumnName(j);
            if (not (extension == '.gct'))
                colDescriptions(j+1) = edata.getColumnDescription(j); 
            end
        end
        expressionMatrix(i+1,j+1) = edataset.get(i,j);
    end
end

expressionDataset.data = expressionMatrix;
expressionDataset.rowNames = cellstr(char(rowNames));
expressionDataset.columnNames = cellstr(char(colNames));
expressionDataset.rowDescriptions = cellstr(char(rowDescriptions));
if (not (extension == '.gct'))
    expressionDataset.columnDescriptions = cellstr(char(colDescriptions));
end
