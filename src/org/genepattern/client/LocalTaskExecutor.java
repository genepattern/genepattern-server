package org.genepattern.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.server.analysis.webservice.client.TaskIntegratorProxy;

/**
 *@author     Joshua Gould
 *@created    May 3, 2004
 */
public class LocalTaskExecutor extends TaskExecutor {
	private final static String JAVA = "java";
	private final static String JAVA_FLAGS = "java_flags";
	TaskIntegratorProxy taskIntegratorProxy;
	/**  url of server */
	String server = null;
	/**  names of support files */
	String[] supportFileNames = null;
	/**  last modification times of support files */
	long[] supportFileDates = null;
	/**  directory where job is run from */
	File sandboxdir = null;
	/**  directory that contains support files for task */
	File libdir = null;

	/**  LSID or task name if connecting to an old server */
	String taskId = null;
	/** name of file in libdir that stores task id */
	static final String ID_FILE_NAME = ".id";

	/** whether we are connecting to a GenePattern Server that does not have the TaskIntegrator SOAP interface */
	boolean oldServer = false;
	
	private static int MAX_FILE_NAME_LENGTH = 255;
	
	public LocalTaskExecutor(TaskInfo taskInfo, Map substitutions, String username, String server) throws java.net.MalformedURLException, org.apache.axis.AxisFault {
		super(taskInfo, substitutions, username);
		this.server = server;
		try {
			new URL(server);
		} catch(java.net.MalformedURLException mfe) {
			this.server = "http://" + this.server;
		}
		this.taskIntegratorProxy = new TaskIntegratorProxy(server, userName, false);
	}

	public void beforeExec() throws RunTaskException {
		try {
			Map attr = taskInfo.getTaskInfoAttributes();
			if(attr != null) {
				taskId = (String) attr.get(org.genepattern.util.GPConstants.LSID);
			}
			if(taskId == null) {
				taskId = taskInfo.getName();
			}
			String taskName = taskInfo.getName();
			sandboxdir = File.createTempFile(taskName, ".tmp");
			sandboxdir.delete();
			
			sandboxdir.mkdirs();
			sandboxdir.deleteOnExit();
			
			String libdirName = null;
			int length = taskName.length()+".libdir".length();
			if(length >= MAX_FILE_NAME_LENGTH) {
				libdirName = taskName.substring(0, taskName.length()-length-MAX_FILE_NAME_LENGTH) + ".libdir";
			} else {
				libdirName = taskName + ".libdir";
			}
			
			
			libdir = new File("libs", libdirName); 
			libdir.mkdirs();
			
			substitutions.put("libdir", libdir + File.separator); 
			if(substitutions.get(LocalTaskExecutor.JAVA) == null) {
				String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" +
						(System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
				substitutions.put(LocalTaskExecutor.JAVA, java);
			}

			if(substitutions.get(LocalTaskExecutor.JAVA_FLAGS) == null) {
				substitutions.put(LocalTaskExecutor.JAVA_FLAGS, "");
			}
			
			synchronizeTaskFiles();
			downloadInputFiles(taskInfo, substitutions);
		} catch(Throwable t) {
			throw new RunTaskException(t);
		}
	}


	protected void startOutputStreamThread(Process proc) {
		startReader(proc.getInputStream(), System.out);
	}


	protected void startErrorStreamThread(Process proc) {
		startReader(proc.getErrorStream(), System.err);
	}


	private void startReader(final InputStream is, final PrintStream out) {
		Thread copyThread = new Thread(
			new Runnable() {
				public void run() {
					BufferedReader in = new BufferedReader(new InputStreamReader(is));
					String line;
					// copy inputstream to outputstream

					try {
						while((line = in.readLine()) != null) {
							out.println(line);
						}
					} catch(IOException ioe) {
						System.err.println(ioe + " while reading from process stream");
					}
				}
			});
		copyThread.setDaemon(true);
		copyThread.start();
	}


	private static int indexOf(String[] values, String s) {
		for(int i = 0, length = values.length; i < length; i++) {
			if(values[i].equals(s)) {
				return i;
			}
		}
		return -1;
	}


	/**
	 *  Determines the 'best' name of a file at the given url.
	 *
	 *@param  url  Description of the Parameter
	 *@return      The uRLFileName
	 */
	static String getURLFileName(URL url) {
		String file = url.getFile();
		String baseName = file.substring(file.lastIndexOf("/") + 1);
		int j;
		try {
	 		if(baseName.indexOf("retrieveResults.jsp") != -1 && (j=file.lastIndexOf("filename=")) != -1) { // for http://18.103.3.29:8080/gp/retrieveResults.jsp?job=1122&filename=all_aml_wv_xval.odf
 				String temp = URLDecoder.decode(file.substring(j+ "filename=".length(),  file.length()), "UTF-8");
 				return new java.util.StringTokenizer(temp, "&").nextToken();
	 		}
			if(baseName.indexOf("getFile.jsp") != -1 && (j=file.lastIndexOf("file=")) != -1) { // for http://cmec5-ea2.broad.mit.edu:8080/gp/getFile.jsp?task=try.SOMClusterViewer.pipeline&file=ten.res
				String temp = URLDecoder.decode(file.substring(j+ "file=".length(),  file.length()), "UTF-8");
				return new java.util.StringTokenizer(temp, "&").nextToken();
			}
		} catch (UnsupportedEncodingException uee) {
			// ignore
		}
		String path = url.getPath();
		path = path.substring(path.lastIndexOf("/") + 1);
		if(path == null || path.equals("")) {
			return "index";
		}
		return path;
	}



	protected void downloadInputFiles(TaskInfo taskInfo, Map substitutions) throws Exception {
		// loop through params, download urls to sandbox directory and change substitions for downloaded files and for local files with file:/ protocol
		try {
			ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();

			for(int i = 0, length = formalParameters.length; i < length; i++) {
				String value = (String) substitutions.get(formalParameters[i].getName());
				if(value == null) {
					substitutions.put(formalParameters[i].getName(), "");
				}
				if(formalParameters[i].isInputFile()) {
					if(value == null || new java.io.File(value).exists()) { // value will be null when it's an optional input file
						continue;
					}
					URL url = null;
					try {
						url = new URL(value);
					} catch(MalformedURLException mfe) {}
					if(url != null && "file".equals(url.getProtocol())) {
						File f = new File(url.getFile());
						substitutions.put(formalParameters[i].getName(), f.getCanonicalPath());
					} else if(url != null) {
						File f = new File(sandboxdir, getURLFileName(url));
						downloadFile(url, f);
						f.deleteOnExit();
						substitutions.put(formalParameters[i].getName(), f.getCanonicalPath());
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

	}


	/**
	 *  Downloads the file at the given URL and saves it locally to the specified
	 *  file.
	 *
	 *@param  url              Description of the Parameter
	 *@param  destinationFile  Description of the Parameter
	 *@exception  IOException  Description of the Exception
	 */
	private void downloadFile(URL url, File destinationFile) throws IOException {
		InputStream is = null;
		FileOutputStream fos = null;
		File file = null;
		try {
			is = url.openStream();
			fos = new FileOutputStream(destinationFile);
			byte[] buf = new byte[100000];
			int j;
			while((j = is.read(buf, 0, buf.length)) > 0) {
				fos.write(buf, 0, j);
			}
		} finally {
			if(is != null) {
				is.close();
			}
			if(fos != null) {
				fos.close();
			}
		}
	}


	/**
	 *  Downloads the support files for the given task to libdir.
	 *
	
	 *@exception  IOException
	 *      Description of the Exception
	 *@exception  org.genepattern.analysis.WebServiceException
	 *      Description of the Exception
	 */
	 
	public void synchronizeTaskFiles() throws WebServiceException, java.rmi.RemoteException {
		String[] supportFileNames = null;
		long[] supportFileDates = null;
		
		synchronized(taskIntegratorProxy) {
			try {
				supportFileNames = taskIntegratorProxy.getSupportFileNames(taskId);
				supportFileDates = taskIntegratorProxy.getLastModificationTimes(taskId, supportFileNames);
			 } catch(org.apache.axis.AxisFault e) {
				 javax.xml.namespace.QName noService = new javax.xml.namespace.QName("http://xml.apache.org/axis/", "Server.NoService");
				 if(e.getFaultCode().equals(noService)) {
					 oldServer = true;
				 } else {
					 throw e; 
				 }
			 }
		}
		if(oldServer) {
			InputStream is = null;
			try {
				URL url = new URL(server + "/gp/getTaskInfo.jsp?name=" + taskInfo.getName());
				Properties props = new Properties();
				is = url.openStream();
				props.load(is);
				String serverTaskDir = props.getProperty("libdir");
				String supportFilesTokens = props.getProperty("support.files");
				String modDatesTokens = props.getProperty("support.file.dates");
				StringTokenizer st1 = new StringTokenizer(supportFilesTokens, ",");
				StringTokenizer st2 = new StringTokenizer(modDatesTokens, ",");
				int tokens = st1.countTokens();
				supportFileNames = new String[tokens];
				supportFileDates = new long[tokens];
				for(int i = 0; i < tokens; i++) {
					supportFileNames[i] = st1.nextToken().trim();
					supportFileDates[i] = Long.parseLong(st2.nextToken().trim());
				}
			} catch(IOException ioe) {
				throw new WebServiceException(ioe);	
			} finally {
				if(is!=null) {
					try {
						is.close();
					} catch(IOException e){}
				}
			}

		}
			
		Map fileName2DateMap = new HashMap();
		for(int i = 0; i < supportFileNames.length; i++) {
			fileName2DateMap.put(supportFileNames[i], new Long(supportFileDates[i]));
		}
		
		File idFile = new File(libdir, ID_FILE_NAME);
		String previousTaskId = null;
		if(idFile.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(idFile));
				previousTaskId = br.readLine();
			} catch(IOException e) {
			} finally {
				if(br!=null) {
					try {
						br.close();
					} catch(IOException x) {}
				}
			}
			
		}
		if(!taskId.equals(previousTaskId)) {
			File[] currentFiles = libdir.listFiles();
			for(int i = 0; i < currentFiles.length; i++) {
				currentFiles[i].delete();
			}	
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new FileWriter(idFile));
				pw.println(taskId);
			} catch(IOException e){
			} finally {
				if(pw!=null) {
					pw.close();	
				}
			}
		}
		
			
		File[] currentFiles = libdir.listFiles();
		
		int supf;

		// delete any currently downloaded files that are extraneous or out-of-date (older or newer)
		for(int curf = 0; curf < currentFiles.length; curf++) {
			boolean found = false;
			// if it isn't a support file, delete it
			for(supf = 0; supf < supportFileNames.length; supf++) {
				if(fileName2DateMap.containsKey(currentFiles[curf].getName())) {
					found = true;
					break;
				}
			}
			if(!found) {
				// delete extraneous file
				if(!currentFiles[curf].getName().equals(ID_FILE_NAME)) {
					currentFiles[curf].delete();
				}
			} else {
				long date = ((Long)(fileName2DateMap.get(currentFiles[curf].getName()))).longValue();
				if(currentFiles[curf].lastModified() != date) {
					// delete out-of-date file (either more recent or older)
					currentFiles[curf].delete();
				}
			}
		}

		// figure out which support files are not in the currently downloaded set and download them
		List downloadSupportFileIndices = new ArrayList();
		List downloadSupportFileNames = new ArrayList();
		
		for(supf = 0; supf < supportFileNames.length; supf++) {
			if(!new File(libdir, supportFileNames[supf]).exists()) {
				// need to download it
				downloadSupportFileIndices.add(new Integer(supf));
				downloadSupportFileNames.add(supportFileNames[supf]);
			}
		}
		
		for(int i = 0; i < downloadSupportFileNames.size(); i++) { // using jsps to download files is quicker than using SOAP
			try { 
				String fileName = (String) downloadSupportFileNames.get(i);	
				String task = null;
				if(oldServer) {
					task = taskInfo.getName();
				} else {
					task = (String) taskInfo.getTaskInfoAttributes().get(org.genepattern.util.GPConstants.LSID);
				}
				
				URL urlFile = new URL(server + "/gp/getFile.jsp?task=" + task + "&file=" + fileName);
				File dest = new File(libdir, fileName);
				downloadFile(urlFile, dest);
			} catch(IOException ioe) {
				throw new WebServiceException(ioe);
			} 
		}
		
		if(DEBUG) {
			System.out.println("Downloaded " + downloadSupportFileNames);	
		}
		for(int i = 0; i < downloadSupportFileNames.size(); i++) {
			String fileName = (String) downloadSupportFileNames.get(i);	
			long modificationTime = supportFileDates[((Integer)(downloadSupportFileIndices.get(i))).intValue()];
			new File(libdir, fileName).setLastModified(modificationTime);
		}
	}
	
	
	
}
