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
dset = edu.mit.broad.dataobj.DataFactory.createDataset(data, rowNames, columnNames);

% handle the case of gct data which does not have col descriptions
if (not (exist('colDescriptions')))
    colDescriptions = columnNames;
end


% this will be overwritten by the res file version
exData = edu.mit.broad.dataobj.microarray.ExpressionDataFactory.createExpressionData(dset, rowDescriptions, colDescriptions);

% select the writer based on the desired output format
if (strcmp(format, 'res'))
    writer = edu.mit.broad.io.microarray.ResWriter();
    callData = edu.mit.broad.dataobj.DataFactory.createIntMatrix(calls);
    exData = edu.mit.broad.dataobj.microarray.ExpressionDataFactory.createResExpressionData(dset, callData, rowDescriptions, colDescriptions);
    ext = '.res';
elseif (strcmp(format, 'gct'))
    writer = edu.mit.broad.io.microarray.GctWriter();
    ext='.gct';
elseif (strcmp(format, 'odf'))
    writer = edu.mit.broad.io.microarray.OdfDatasetWriter();
    ext='.odf';
elseif (strcmp(format,'mageml'))
    writer = edu.mit.broad.io.microarray.MAGEMLWriter();
    ext='.xml';
else
   return 
end

% add the file extension if missing or mismatched
plen = length(path);
startIdx = plen - 3;
if (startIdx < 1)
    startIdx = 1;
end
pathext = path(startIdx:plen);
if (not (strcmp(pathext, ext)))
    path = [path ext];
end

%write the file
fos =  java.io.FileOutputStream(path);
writer.write(exData, fos);
fos.flush();
fos.close();

