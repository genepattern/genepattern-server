<%@ page import="java.io.File,java.io.FileInputStream,
		 java.io.IOException,
		 org.genepattern.io.UniversalDecoder" 
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

FileInputStream in = null;
File f = null;
boolean DEBUG = false;
try {
	String filename = request.getParameter("filename");
	boolean justHeaders = (request.getParameter("justHeader") != null);
	if (filename == null || filename.length() == 0) return;
	if (DEBUG) System.out.println(new java.util.Date() + ": downloading " + filename + (justHeaders ? " headers" : " (complete file)"));
	String status = "";
	f = new File(System.getProperty("jobs"), filename);
        in = new java.io.FileInputStream(f);
	if (justHeaders) {
		// caller just wants the headers
		String headers = "";
		try {
			headers = UniversalDecoder.getFullHeader(in, filename);
		} catch (Throwable t) {
			status = t.toString();
		} finally {
			out.print(status);
			out.print("\n");
			out.print(headers); // could be empty string
			return;
		}
	}
	byte[] buf = new byte[100000];
	int i;
	i = in.read(buf);
	out.print("\n"); // write the success status (empty string), since we successfully read the first bufferfull
	while (i > -1) {
		out.print(new String(buf, 0, i)); // copy input file to response
		i = in.read(buf);
	}
	in.close();
	in = null;
	return;
} catch (IOException ioe) {
	try { System.err.println("loadDataSet.jsp: " + ioe + " loading " + f.getCanonicalPath()); } catch (IOException ignore) {}
	out.println("loadDataSet.jsp: " + ioe.toString());
	response.setHeader(UniversalDecoder.EXCEPTION, ioe.toString());
} finally {
	if (in != null) {
		try {
			in.close();
		} catch (IOException ioe) {
		}
	}
	if (DEBUG) System.out.println(new java.util.Date() + ": download complete");
}
%>