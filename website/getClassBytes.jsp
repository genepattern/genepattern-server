<%@ page import="java.io.InputStream, 
		 java.io.File,
		 java.io.FileInputStream,
		 java.io.FilenameFilter,
		 java.io.InputStreamReader,
		 java.io.IOException,
		 java.net.URLEncoder,
		 java.util.Enumeration,
		 java.util.StringTokenizer,
		 java.util.jar.*,
		 java.util.zip.*,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask" 
session="false" contentType="text/text" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

boolean DEBUG = (request.getParameter("debug") != null);
String taskName = request.getParameter("taskName");
String className = request.getParameter("className");
String classPath = request.getParameter("classPath");
String LIBDIR = GPConstants.LEFT_DELIMITER + GPConstants.LIBDIR + GPConstants.RIGHT_DELIMITER;
if (classPath == null) classPath="";

if (DEBUG) System.out.println("1. Looking for " + taskName + "'s " + className + " class on " + classPath );
int i;
String startingPath = GenePatternAnalysisTask.getTaskLibDir(taskName, null, null) + File.separator;
StringBuffer after = new StringBuffer(classPath);
String[] fileList = new File(startingPath).list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.endsWith(".old");
				} });
if (DEBUG) System.out.println("fileList: " + startingPath + ": " + (fileList != null ? "" + fileList.length : "null"));
after.append(System.getProperty("path.separator"));
after.append(startingPath + ".");
if (fileList != null) {
	for (i = 0; i < fileList.length; i++) {
		after.append(System.getProperty("path.separator"));
		after.append(startingPath);
		after.append(fileList[i]);
	}
}
do {
	i = after.toString().indexOf(LIBDIR);
	if (i != -1) after = after.replace(i, LIBDIR.length()+i, startingPath);
} while (i != -1);
if (DEBUG) System.out.println("classpath after substitution: " + after.toString());
classPath = after.toString();
if (DEBUG) System.out.println("2. Looking for " + taskName + "'s " + className + " class on " + classPath );
// BUG: should this just be a semi-colon, rather than path.separator?  Problem is if a DOS drive-colon... entry is there...
StringTokenizer stClassPath = new StringTokenizer(classPath, System.getProperty("path.separator"));
String currentPath = null;
String lowerPath = null;
long fileLength = 0;

InputStream is = null;
File file = null;

outOfSearch:
while (stClassPath.hasMoreTokens()) {
	currentPath = stClassPath.nextToken();
	if (DEBUG) System.out.println("3. Looking for " + taskName + "'s " + className + " class on " + currentPath );
	lowerPath = currentPath.toLowerCase();
	try {
		if (lowerPath.endsWith(".jar") || lowerPath.endsWith(".zip")) {
			if (DEBUG) System.out.println("4. Looking in a " + currentPath.substring(currentPath.length() - 3) + " file.");
			try {
				JarFile jarFile = new JarFile(currentPath);
				ZipEntry zipEntry = null;
				for (Enumeration eEntries = jarFile.entries(); eEntries.hasMoreElements(); ) {
					zipEntry = (ZipEntry)eEntries.nextElement();
					//System.out.println("  " + zipEntry.getName());
					if (!zipEntry.getName().equals(className)) {
						continue;
					}
					if (DEBUG) System.out.println("found the jar file entry: " + zipEntry.getName());
					is = jarFile.getInputStream(zipEntry);
					fileLength = zipEntry.getSize();
					break outOfSearch;
				}			
			} catch (IOException ioe) {
			}
		} else {
			file = new File(currentPath, className);
			if (DEBUG) System.out.println("looking for " + file.toString() );
			if (!file.exists()) continue;
			if (DEBUG) System.out.println("found file " + file.toString() );
			is = new FileInputStream(file);
			fileLength = file.length();
			break outOfSearch;
		}			
	} catch (IOException ioe) {
		if (DEBUG) System.out.println(ioe );
	}
}
if (is == null) {
	System.out.println("couldn't find " + taskName + "'s " + className + " in " + classPath);
	return;
}

if (DEBUG) System.out.println("file size=" + fileLength);
try {
	byte[] buf = new byte[(int)fileLength];
	String output = null;
	int numRead = 0;
	while ((i = is.read(buf, numRead, buf.length-numRead)) > 0) {
//		if (DEBUG) System.out.println("read " + i + " bytes:");
		numRead += i;
	}
	is.close();
	output = new String(buf, 0, numRead, "iso-8859-1");
	// output a 6 digit length description first
	String lengthPrefix = "" + output.length();
	lengthPrefix = lengthPrefix + "      ".substring(lengthPrefix.length());
	out.print(lengthPrefix);
//	if (DEBUG) System.out.println("writing " + output.length() + " bytes.");
	out.print(output);
	return;
} catch (IOException ioe) {
	out.println(ioe );
	return;
}
%>