function testRunExtractColumnNames(obj)

fullnameandpath = mfilename('fullpath');
nmlen = length(fullnameandpath); 
localDir = fullnameandpath(1:(nmlen-25));
infile = [localDir 'smaller.res'];

% 1. leave the output as default
p1.input_filename= infile;
res = runAnalysis(obj.server,'ExtractColumnNames',p1);

if (not (strcmp('smaller.txt',res.fileNames)))
   error('Output file not right or absent: smaller.txt'); 
end

% 2. set the output file name
p2.input_filename= infile;
p2.output = 'ecn2out.txt';
res = runAnalysis(obj.server,'ExtractColumnNames',p2);

if (not (strcmp('ecn2out.txt',res.fileNames)))
   error('Output file not right or absent: smaller.txt'); 
end

% 3. using the getMethodParameters to start
p3 = getMethodParameters(obj.server, 'ExtractColumnNames');
p3.input_filename= infile;
p3.output = 'ecn3out.txt';
res = runAnalysis(obj.server,'ExtractColumnNames',p3);

if (not (strcmp('ecn3out.txt',res.fileNames)))
   error('Output file not right or absent: smaller.txt'); 
end


% 4. try for expected exception with no input file
try
p4 = getMethodParameters(obj.server, 'ExtractColumnNames');
res = runAnalysis(obj.server,'ExtractColumnNames',p4);
error('Expected exception for missing input file didn''t happen');
catch
    % we expect this exception and should be here
end

