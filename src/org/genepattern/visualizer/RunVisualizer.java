package org.genepattern.visualizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.genepattern.visualizer.RunVisualizerConstants;


public class RunVisualizer {

	protected boolean DEBUG = false;
	protected HashMap params = null;
	protected URL source = null;
	protected String[] supportFileNames = null;
	protected long[] supportFileDates = null;

	protected static final String JAVA = "java";
	protected static final String JAVA_FLAGS = "java_flags";
	protected static final String ANY = "any";
	protected static final String leftDelimiter = "<";
	protected static final String rightDelimiter = ">";

	/**
	 * @param params: HashMap containing the following key/value pairs:
	 *		  RunVisualizerConstants.NAME=name of visualizer task
	 *		  RunVisualizerConstants.COMMAND_LINE=task command line (from TaskInfoAttributes)
	 *		  RunVisualizerConstants.DEBUG=1 if debug output to stdout is desired (otherwise omit)
	 *		  RunVisualizerConstants.OS=operating system choice from TaskInfoAttributes
	 *		  RunVisualizerConstants.CPU_TYPE=CPU choice from TaskInfoAttributes
	 *		  RunVisualizerConstants.LIBDIR=directory on server where the task lives
	 *		  RunVisualizerConstants.DOWNLOAD_FILES=CSV list of parameter names which are URLs that need downloading by client prior to execution
	 *		  RunVisualizerConstants.LSID=LSID of visualizer task
	 *		  PLUS all of the input parameters that the task requires (eg. input.filename, out.stub, etc.)
	 *
	 * @param source: URL to the server (eg. http://localhost:8080), used for downloading task support files
	 * @param supportFileNames: array of names (without paths) of required support files for this task
	 * @param supportFileDates: array of lastModified entries (longs) corresponding to each support file
	 * 
	 */

	public RunVisualizer(HashMap params, URL source, String[] supportFileNames, long[] supportFileDates) {
		this.params = params;
		this.source = source;
		this.supportFileNames = supportFileNames;
		this.supportFileDates = supportFileDates;
	}

	public void run() throws IOException, Exception {
		DEBUG = (params.get("DEBUG") != null);
		if (DEBUG) System.out.println("runVisualizer: starting");
		
		// download all of the files locally, preferably checking against a cache
		String libdir = downloadSupportFiles();

		// libdir is where all of the support files will be found, either local or remote
		params.put(RunVisualizerConstants.LIBDIR, libdir + File.separator);

		String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java" +
				(System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
		params.put(JAVA, java);
		params.put(JAVA_FLAGS, "");
		params.put("GENEPATTERN_PORT", "" + source.getPort());

		// check OS and CPU restrictions of TaskInfoAttributes against this server
		validateCPU(); // eg. "x86", "ppc", "alpha", "sparc"
		validateOS();  // eg. "Windows", "linux", "Mac OS X", "OSF1", "Solaris"

		String[] commandLine = doCommandLineSubstitutions();
		
		runCommand(commandLine);

		if (DEBUG) System.out.println("runVisualizer: done");
	}


	// TODO: report unused parameters
	// TODO: report unfound substitutions

	protected String[] doCommandLineSubstitutions() throws IOException {
		// do input argument substitution in command line
		String commandLine = (String)params.get(RunVisualizerConstants.COMMAND_LINE);

		HashMap hmDownloadables = downloadInputURLs();

		StringTokenizer stCmd = new StringTokenizer(commandLine, " ");
		ArrayList cmdArray = new ArrayList(stCmd.countTokens());
		int c = 0;
		while (stCmd.hasMoreTokens()) {
			cmdArray.add(stCmd.nextToken());
		}

		// replace variables in the command line from System.properties and the params HashMap
		for (c = 0; c < cmdArray.size(); c++) {
			String cmd = (String)cmdArray.get(c);
			cmd = variableSubstitution(cmd, hmDownloadables);
			cmdArray.set(c, cmd);
			// if there is nothing in a slot after substitutions, delete the slot entirely
			if (cmd.length() == 0) {
				cmdArray.remove(c);
				c--;
			}
		}
		cmdArray.trimToSize();
		return (String[])cmdArray.toArray(new String[0]);
	}


	protected String variableSubstitution(String var, HashMap hmDownloadables) {
		int start = 0 ;
		int end;
		String argValue = null;
		String variableName;
		for (start = var.indexOf(leftDelimiter, 0); start != -1; start = var.indexOf(leftDelimiter, start)) {
			end = var.indexOf(rightDelimiter, start);
			if (end == -1) break;
			//if (DEBUG) System.out.println("found " + var.substring(start, end + leftDelimiter.length()));
			variableName = var.substring(start+leftDelimiter.length(), end);
			argValue = System.getProperty(variableName);
			if (argValue == null) {
				argValue = (String)params.get(variableName);
			}
			if (argValue != null) {
				//if (DEBUG) System.out.println("replacing it with " + argValue);
				if (hmDownloadables != null && hmDownloadables.containsKey(variableName)) {
					try {
						argValue = ((File)hmDownloadables.get(variableName)).getCanonicalPath();
						if (DEBUG) System.out.println("replacing URL " + (String)params.get(variableName) + " with " + argValue);
					} catch (IOException ioe) {
						System.err.println(ioe + " while getting canonical path for " + ((File)hmDownloadables.get(variableName)).getName());
					}
				}
				var = replace(var, var.substring(start, end+rightDelimiter.length()), argValue);
			} else {
				System.err.println("Unable to find substitution for " + variableName);
				//throw new Exception("Unable to find substitution for " + variableName);
				start = end + rightDelimiter.length();
			}
		}
		return var;
	}

	protected void runCommand(String[] commandLine) throws IOException {
		Process p = null;
		int exitValue = -1;
		Thread stdoutReader = null;
		Thread stderrReader = null;

		if (DEBUG) {
			System.out.print("executing ");
			for (int i = 0; i < commandLine.length; i++) {
				System.out.print(commandLine[i] + " ");
			}
			System.out.println();
		}
		p = Runtime.getRuntime().exec(commandLine);

		stdoutReader = copyStream(p.getInputStream(), System.out);
		stderrReader = copyStream(p.getErrorStream(), System.err);

		// drain the output and error streams
		stdoutReader.start();
		stderrReader.start();
	}

	protected String downloadSupportFiles() throws IOException {

		String name = (String)params.get(RunVisualizerConstants.NAME);
		String lsid = (String)params.get(RunVisualizerConstants.LSID);

		// don't even bother using the local files since downloading is so fast and the caching is conservative

		String host = source.getHost();
		if (host.equals("localhost") || host.equals("127.0.0.1") ||
		    InetAddress.getLocalHost().equals(InetAddress.getByName(host))) {
			if (DEBUG) System.out.println("using local libdir: " + (String)params.get(RunVisualizerConstants.LIBDIR));
			return (String)params.get(RunVisualizerConstants.LIBDIR);
		}

		Date startDLTime = new Date();
		File fLibdir = new File(getTempDir(), name + ".libdir");
		fLibdir.mkdirs();
		//if (DEBUG) System.out.println("tempdir=" + fLibdir.getCanonicalPath());

		File[] currentFiles  = fLibdir.listFiles();
		int supf;

		// delete any currently downloaded files that are extraneous or out-of-date (older or newer)
		for (int curf = 0; curf < currentFiles.length; curf++) {
			boolean found = false;
			// if it isn't a support file, delete it
			for (supf = 0; supf < supportFileNames.length; supf++) {
				if (currentFiles[curf].getName().equals(supportFileNames[supf])) {
					found = true;
					break;
				}
			}
			if (!found) {
				// delete extraneous file
				if (DEBUG) System.out.println("deleting extraneous file " + currentFiles[curf].getCanonicalPath());
				currentFiles[curf].delete();
			} else {
				if (currentFiles[curf].lastModified() != supportFileDates[supf]) {
					// delete out-of-date file (either more recent or older)
					if (DEBUG) System.out.println("deleting out-of-date file " + currentFiles[curf].getCanonicalPath());
					currentFiles[curf].delete();
				}
			}
		}
		
		// figure out which support files are not in the currently downloaded set and download them
		for (supf = 0; supf < supportFileNames.length; supf++) {
			if (!new File(fLibdir, supportFileNames[supf]).exists()) {
				// need to download it
				if (DEBUG) System.out.print("downloading missing file " + supportFileNames[supf] + "...");
				Date startTime = new Date();
				URL urlFile = null;
				
				urlFile = new URL(source, "/gp/getFile.jsp?task=" + lsid + "&file=" + encode(supportFileNames[supf]));
				
				File file = downloadFile(urlFile, fLibdir, supportFileNames[supf]);
				file.setLastModified(supportFileDates[supf]);
				long downloadTime = new Date().getTime() - startTime.getTime();
				if (DEBUG) System.out.println(" received " + file.length() + " bytes in " + downloadTime/1000.0 + " seconds");
			}
		}

		if (DEBUG) System.out.println("Total download time " + (new Date().getTime() - startDLTime.getTime())/1000.0 + " seconds");
		return fLibdir.getCanonicalPath();
	}

	// download a URL to a local file and return a File object for it
	protected File downloadFile(URL url, File dir, String filename) throws IOException {
	    InputStream is = null;
	    FileOutputStream fos = null;
	    File file = null;
	    try {
		is = url.openStream();
		dir.mkdirs();
		file = new File(dir, filename);
		if (DEBUG) System.out.println("writing to " + file.getCanonicalPath());
		fos = new FileOutputStream(file);
		byte[] buf = new byte[100000];
		int j;
		while ((j = is.read(buf, 0, buf.length)) > 0) {
			fos.write(buf, 0, j);
		}
	   } finally {
		if (is != null) is.close();
		if (fos != null) fos.close();
	   }
	   return file;
	}

    protected boolean validateCPU() throws Exception {
	String expected = (String)params.get(RunVisualizerConstants.CPU_TYPE);
	String taskName = (String)params.get(RunVisualizerConstants.NAME);
	String actual = System.getProperty("os.arch");
	// eg. "x86", "i386", "ppc", "alpha", "sparc"

	if (expected.equals("")) return true;
	if (expected.equals(ANY)) return true;
	if (expected.equalsIgnoreCase(actual)) return true;

	String intelEnding = "86"; // x86, i386, i586, etc.
	if (expected.endsWith(intelEnding) && actual.endsWith(intelEnding)) return true;

	throw new Exception("Cannot run on this platform.  " + taskName + " requires a " + expected + " CPU, but this is a " + actual);
    }

    protected boolean validateOS() throws Exception {
	String expected = (String)params.get(RunVisualizerConstants.OS);
	String taskName = (String)params.get(RunVisualizerConstants.NAME);
	String actual = System.getProperty("os.name");
	// eg. "Windows XP", "Linux", "Mac OS X", "OSF1"

	if (expected.equals("")) return true;
	if (expected.equals(ANY)) return true;
	if (expected.equalsIgnoreCase(actual)) return true;

	String MicrosoftBeginning = "Windows"; // Windows XP, Windows ME, Windows XP, Windows 2000, etc.
	if (expected.startsWith(MicrosoftBeginning) && actual.startsWith(MicrosoftBeginning)) return true;

	throw new Exception("Cannot run on this platform.  " + taskName + " requires " + expected + " operating system, but this computer is running " + actual);
    }

    private static String decode(String s) {
       try {
             return URLDecoder.decode(s, "UTF-8");  
       } catch(NoSuchMethodError e) {
          return URLDecoder.decode(s);
       } catch(java.io.UnsupportedEncodingException x) {
          return s;
       }
    }
    
    
    private static String encode(String s) {
       try {
          return URLEncoder.encode(s, "UTF-8");  
       } catch(NoSuchMethodError e) {
          return URLEncoder.encode(s);
       } catch(java.io.UnsupportedEncodingException x) {
          return s;
       }
    }
    
	 private String getURLFileName(URL url) {
		String file = url.getFile();
		String baseName = file.substring(file.lastIndexOf("/") + 1);
		int j;
		
      if(baseName.indexOf("retrieveResults.jsp") != -1 && (j=file.lastIndexOf("filename=")) != -1) { // for http://18.103.3.29:8080/gp/retrieveResults.jsp?job=1122&filename=all_aml_wv_xval.odf
         String temp = decode(file.substring(j+ "filename=".length(),  file.length()));
         return new java.util.StringTokenizer(temp, "&").nextToken();
      }
      if(baseName.indexOf("getFile.jsp") != -1 && (j=file.lastIndexOf("file=")) != -1) { // for http://cmec5-ea2.broad.mit.edu:8080/gp/getFile.jsp?task=try.SOMClusterViewer.pipeline&file=ten.res
         
         String temp = decode(file.substring(j+ "file=".length(),  file.length()));
         return new java.util.StringTokenizer(temp, "&").nextToken();
         
      }
		
		String path = url.getPath();
		path = path.substring(path.lastIndexOf("/") + 1);
		if(path==null || path.equals("")) {
			return "index";
		}
		return path;
	}
	
    // for input parameters which are URLs, and which are listed in the DOWNLOAD_FILES input parameter, download
    // the URL to a local file and create a mapping between the URL and the local filename for the command 
    // line substitution
    
    protected HashMap downloadInputURLs() throws IOException {
	// create a mapping between downloadable files and their actual (post-download) filenames
	HashMap hmDownloadables = new HashMap();
	StringTokenizer stDownloadables = new StringTokenizer((String)params.get(RunVisualizerConstants.DOWNLOAD_FILES), ",");
	String name = (String)params.get(RunVisualizerConstants.NAME);
	String prefix = (name.length() < 3 ? "dl" + name : name);
	File tempdir = File.createTempFile(prefix, ".tmp");
	tempdir.delete();
	tempdir.mkdir();
	tempdir.deleteOnExit();
	
	while (stDownloadables.hasMoreTokens()) {
		String paramName = stDownloadables.nextToken();
		String paramURL = (String)params.get(paramName);
		paramURL = variableSubstitution(paramURL, hmDownloadables);
		
		String filename = getURLFileName(new URL(paramURL));
		if (DEBUG) System.out.println("downloading " + paramURL + " to " + tempdir + " as " + filename);
		File file = downloadFile(new URL(paramURL), tempdir, filename);
		file.deleteOnExit();
		hmDownloadables.put(paramName, file);
	}
	return hmDownloadables;
    }

    /**
     * replace all instances of "find" in "original" string and substitute "replace" for them
     *
     * @param original String before replacements are made
     * @param find	   String to search for
     * @param replace  String to replace the sought string with
     * @return String  String with all replacements made
     * @author Jim Lerner
     */
	protected static final String replace(String original, String find, String replace) {
		StringBuffer res = new StringBuffer();
		int idx = 0;
		int i = 0;
		while (true) {
			i = idx;
			idx = original.indexOf(find,  idx);
			if ( idx == -1 ) {
				res.append(original.substring(i));
				break;
			}
			else {
				res.append(original.substring(i, idx));
				res.append(replace);
				idx += find.length();
			}
		}
		return res.toString();
	}

	protected Thread copyStream(final InputStream is, final PrintStream out) throws IOException {
	    // create thread to read from the a process' output or error stream
	    Thread copyThread = new Thread(new Runnable(){
	    	public void run() {
	            BufferedReader in = new BufferedReader(new InputStreamReader(is));
	            String line;
		    // copy inputstream to outputstream

		    try {
			boolean bNeedsBreak;
			while((line = in.readLine())!=null){
				out.println(line);
			}
		    } catch (IOException ioe) {
			System.err.println(ioe + " while reading from process stream");
	            }
	    	}
	    });
	    copyThread.setDaemon(true);
	    return copyThread;
    }

    protected File getTempDir() throws IOException {
	File tempdir = File.createTempFile("foo", ".libdir");
	tempdir.delete();
	return tempdir.getParentFile();
    }

    /**
     * 	args[0] = visualizer task name
     *	args[1] = command line
     *	args[2] = debug flag
     *  args[3] = OS required for running
     *	args[4] = CPU type required for running
     *  args[5] = libdir on server for this task
     * 	args[6] = CSV list of downloadable files for inputs
     *	args[7] = CSV list of input parameter names
     *	args[8] = CSV list of support file names
     *	args[9] = CSV list of support file modification dates
     *	args[10] = server URL
     *  args[11] = LSID of task
     *	args[12...n] = optional input parameter arguments
     */
    public static void main(String[] args) {
	String[] wellKnownNames = { RunVisualizerConstants.NAME, RunVisualizerConstants.COMMAND_LINE, 
				    RunVisualizerConstants.DEBUG, RunVisualizerConstants.OS, 
				    RunVisualizerConstants.CPU_TYPE, RunVisualizerConstants.LIBDIR,
				    RunVisualizerConstants.DOWNLOAD_FILES, RunVisualizerConstants.LSID };
	int PARAM_NAMES = 7;
	int SUPPORT_FILE_NAMES = PARAM_NAMES + 1;
	int SUPPORT_FILE_DATES = SUPPORT_FILE_NAMES + 1;
	int SERVER = SUPPORT_FILE_DATES + 1;
	int LSID = SERVER + 1;
	int TASK_ARGS = LSID + 1;

	try {
		HashMap params = new HashMap();
		for (int i = 0; i < wellKnownNames.length; i++) {
			params.put(wellKnownNames[i], args[i]);
		}

		String name = (String)params.get(RunVisualizerConstants.NAME);
		StringTokenizer stParameterNames = new StringTokenizer(args[PARAM_NAMES], ", ");
		int argNum = TASK_ARGS;
		// when pulling parameters from the command line, don't assume that all were provided.
		// some could be missing!
		while (stParameterNames.hasMoreTokens()) {
			String paramName = stParameterNames.nextToken();
			if (argNum < args.length) {
				String paramValue = args[argNum++];
				params.put(paramName, paramValue);
			} else {
				System.err.println("No value specified for " + paramName);
			}
		}
		URL source = new URL(args[SERVER]);

		StringTokenizer stFileNames = new StringTokenizer(args[SUPPORT_FILE_NAMES], ",");
		StringTokenizer stFileDates = new StringTokenizer(args[SUPPORT_FILE_DATES], ",");
		String[] supportFileNames = new String[stFileNames.countTokens()];
		long[] supportFileDates = new long[supportFileNames.length];
		String filename = null;
		String fileDate = null;
		int f = 0;
		while (stFileNames.hasMoreTokens()) {
			supportFileNames[f] = stFileNames.nextToken();
			if (stFileDates.hasMoreTokens()) {
				supportFileDates[f] = Long.parseLong(stFileDates.nextToken());
			} else {
				supportFileDates[f] = -1;
			}
			f++;
		}

		RunVisualizer visualizer = new RunVisualizer(params, source, supportFileNames, supportFileDates);
		visualizer.run();
	} catch (Throwable t) {
		t.printStackTrace();
	}
    }
}