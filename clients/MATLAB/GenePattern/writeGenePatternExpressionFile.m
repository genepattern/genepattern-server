function writeGenePatternExpressionFile(path, format, data, rowNames, columnNames, rowDescriptions, colDescriptions, calls)
% Write a res, gct, mage or odf file with data provided.
%
% Parameters
%   path                - filename (incl path) to write to
%   format              - One of res, gct, odf, mageml
%   data                - M by N matrix of doubles
%   rowNames            - M*1 array of Strings
%   columnNames         - N*1 array of Strings
%   rowDescriptions     - M*1 array of strings
%   columnDescriptions  - unless loading from a gct filefrom a MATLAB
%   matrix (if absent, columnNames will be used for this)
%   calls               - M*N matrix of ints for A/P calls (res format only)
% 
% Return
%   none.
global GenePatternPathSet

initGenePatternPath();

% handle the case of gct data which does not have col descriptions
if (not (exist('colDescriptions')))
    colDescriptions = columnNames;
end
[rownum, colnum] = size(data);
mu = org.genepattern.util.MatlabUtil();

exData = mu.asExpressionData(rownum, colnum,rowNames, rowDescriptions, columnNames, colDescriptions, data);

outfile = org.genepattern.io.IOUtil.write(exData, format, path, true);


