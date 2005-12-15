<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


<%@ page import="java.io.*"
	session="false" contentType="text/plain" language="Java" %><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String purgeRequest = request.getParameter("purge");
	long purgeBefore;
	purgeBefore = (purgeRequest != null ? Long.parseLong(purgeRequest) : Long.MIN_VALUE);

	File logFile = new File("logs", "taskLog.txt");
	File newLogFile = new File("logs", "taskLog.new");
	FileWriter newLogWriter = null;

	BufferedReader logReader = new BufferedReader(new FileReader(logFile));
	long entryTimestamp;
	String line = null;
	int i;
	while ((line = logReader.readLine()) != null) {
		out.println(line);
		if (purgeRequest != null) {
			// find the delimiter between the millisecond timestamp and the next field
			i = line.indexOf(" ");

			// compare the timestamp to the cutoff
			entryTimestamp = Long.parseLong(line.substring(0, i));

			// if the new line is later than the timestamp, keep it
			if (entryTimestamp > purgeBefore) {
				// create the new log file if it doesn't exist
				if (newLogWriter == null) {
					newLogWriter = new FileWriter(newLogFile);
				}
				newLogWriter.write(line);
				newLogWriter.write("\n");
			}
		}
	}
	logReader.close();
	// if old ones are being deleted, then just keep the new log file
	if (purgeRequest != null) {
		logFile.delete();
	}
	if (newLogWriter != null) {
		newLogWriter.close();
		newLogFile.renameTo(logFile);
	}
	return;
%>