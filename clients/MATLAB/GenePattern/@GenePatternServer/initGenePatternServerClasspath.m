function initGenePatternServerClasspath(obj)
% set the GenePattern jar files into the java class path

% NOTE: since persistent and global variables don't seem to work for 
% class functions, we get the ugly structure below to avoid classpath
% errors when this method is called many times

  
initGenePatternPath
