<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 java.io.*,
		 java.net.*,
		 java.net.MalformedURLException,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.ParseException,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>

<h2> Downloading zip files to temp directory: c:\temp\modules</h2>

<%
String userID = "GenePattern";
	
String moduleZipDir = "c:/temp/modules/";
InstallTasksCollectionUtils collection = null;
InstallTask[] tasks = null;
HashMap latestVersions = new HashMap();

try {
	collection = new InstallTasksCollectionUtils(userID, true);

	tasks = collection.getAvailableModules();
} catch (Exception e) {
	e.printStackTrace();
} 

// loop over them to grab only the latest
for (int i=0; i < tasks.length; i++){
	InstallTask task = tasks[i];

	String lsidnv = (new LSID(task.getLSID())).toStringNoVersion();
	String v2 = task.getLSIDVersion();

	InstallTask altTask = (InstallTask)latestVersions.get(lsidnv);
	if (altTask == null) {
		latestVersions.put(lsidnv, task);
	} else {
		String v1 = altTask.getLSIDVersion();
		if ((new Integer(v2)).intValue() > (new Integer(v1)).intValue()) latestVersions.put(lsidnv, task);
	}
}
for (Iterator iter = latestVersions.keySet().iterator(); iter.hasNext(); ){
	String key  = (String)iter.next();
	InstallTask task = (InstallTask)latestVersions.get(key);
	String zipFile = downloadZip(moduleZipDir , task.getName(), task.getURL()); 

	out.println("<br>" + task.getName() + "   " + zipFile);


}

%>



<%! 
public static String downloadZip(String dir, String name, String zipURL) throws IOException {
	    File zipFile = null;
	    long downloadedBytes = 0;

	    try {
		zipFile = new File(dir, name+".zip");
		
		FileOutputStream os = new FileOutputStream(zipFile);
		URLConnection uc = new URL(zipURL).openConnection();
		long downloadSize = -1;
		Map headerFields = uc.getHeaderFields();
		for (Iterator itHeaders = headerFields.keySet().iterator(); itHeaders.hasNext(); ) {
			String hname = (String)itHeaders.next();
			String hvalue = uc.getHeaderField(name);
			System.out.println(hname + "=" + hvalue);
		}
		if (uc instanceof HttpURLConnection) {
			downloadSize = ((HttpURLConnection)uc).getHeaderFieldInt("Content-Length", -1);
		}		
		InputStream is = uc.getInputStream();
		byte[] buf = new byte[100000];
		int i;
		long lastPercent = 0;
		while ((i = is.read(buf, 0, buf.length)) > 0) {			
			downloadedBytes += i;
			os.write(buf, 0, i);
		}
		is.close();
		os.close();
		return zipFile.getPath();
	    } catch (IOException ioe) {
	    	zipFile.delete();
	    	throw ioe;
	    } finally {
		System.out.println("downloaded " + downloadedBytes + " bytes");
	    }
	}
%>