function pathOK=initGenePatternPath(pathToJars)
%
% init GenePattern file support by registering the path
% to the directory containing the GenePattern jar files.  
% If a path is not given it will look for the jars inside
% the directory where this m-file lives (e.g. toolbox/GenePattern)
%
% Parameters
%   pathToJars  - (optional) path to the directory containing GenePattern jar files
% Return
%   None.
%
dynclasspath = javaclasspath();
staticclasspath = javaclasspath('-static');
statjarsfoundcount=0;
dynamicjarsfoundcount=0;
staticJarsFound = {};

jarFiles = char('gp-modules.jar','Jama-1.0.1.jar',...
    'MAGEstk-2003-10-24-v1.1-compatible.jar','trove.jar',...
    'axis.jar','GenePattern.jar','commons-codec-1.3.jar',...
    'commons-discovery-0.2.jar','commons-httpclient-3.1.jar','commons-logging.jar',...
    'gpwebservice.jar','jaxb-api-2.2.2.jar','jaxb-impl-2.2.3.jar','jaxrpc.jar','log4j-1.2.16.jar',...
    'javax.mail.jar','saaj.jar');

jarFileCount = size(jarFiles,1);


pathToJars = fullfile(fileparts(mfilename('fullpath')),'lib/');
   
for i=1:jarFileCount
   jarfile = [pathToJars deblank(jarFiles(i,:))];
   addFile = false;
   filename = deblank(jarFiles(i,:));


   [onpath, path] = isOnPath(dynclasspath, pathToJars, filename); 
   if (~onpath)
       addFile=true;
   end

   [onpath, path] = isOnPath(staticclasspath, pathToJars, filename);
   if (onpath)
	addFile=false;
	statjarsfoundcount = statjarsfoundcount +1;
	staticJarsFound{ statjarsfoundcount } = path;	
   end
   
   if (addFile)
	%disp(['Adding...' jarfile]);
	eval([ 'javaaddpath  ' jarfile ' -start']);
   end

end

if ((statjarsfoundcount >= 1) && (statjarsfoundcount ~= jarFileCount))
	disp('Found alternate versions of jar files used by GenePattern on the static java classpath; ');
	for i=1:statjarsfoundcount 
          disp(['    ' staticJarsFound{i}]);
	end
	disp(' ');
	disp('Due to the way MATLAB uses java, the jar files used by GenePattern must be either');
	disp('all on the static class path or all on the dynamic class path. Before running the ');
	disp('GenePattern client you should add all of its jar files located at ');
	disp(['    ' pathToJars]);
	disp('to the end of MATLABs static java classpath. To do this, copy the jar files');
	disp(['listed (below) to the end of the file ' matlabroot '/toolbox/local/classpath.txt']);
	disp(' ');
	for i=1:jarFileCount
   		disp( [pathToJars deblank(jarFiles(i,:))]);
	end

	disp(' ');
	disp('Restart MATLAB after making these changes.');
	pathOK=false;
else 
	pathOK = true;
end

import org.genepattern.*
end

function [onpath, path]=isOnPath(classpath2, pathToJars, jarfilename)
 jarfilepath = [pathToJars deblank(jarfilename)];
 
for j=1:length(classpath2)

    if (strcmp(classpath2(j), jarfilepath))
        onpath=true;
	  path=jarfilepath;
        return;
    end

    if (~isempty(strfind(char(classpath2(j)), jarfilename)))
	path=char(classpath2(j));
	onpath=true;
	return;
    end

end
jarfilename;
onpath = false;
path='';
return
end

