package org.genepattern.server.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;

public class JSPPrecompiler implements Runnable {

	protected String jspDir = null;

	protected String urlBase = null;

	protected String webmaster = null;

	protected String[] jspFilenames = null;

	protected StringBuffer message = new StringBuffer();

	protected int numErrors = 0;

	final static String JSP = ".jsp";

	final static boolean DEBUG = false;

	public JSPPrecompiler(String jspDir, String urlBase, String webmaster) {
		if (DEBUG)
			System.out.println("JSPPrecompiler using " + jspDir + " and "
					+ urlBase);
		this.jspDir = jspDir;
		this.urlBase = urlBase;
		this.jspFilenames = null;
		this.webmaster = webmaster;
	}

	public void run() {
		if (jspDir == null || urlBase == null)
			return;
		System.out.println(new Date() + ": compiling JSPs.");

		if (jspFilenames == null || jspFilenames.length == 0) {
			// generate a list of jsp filenames to precompile
			jspFilenames = new File(jspDir).list(new FilenameFilter() {
				// INNER CLASS !!!
				public boolean accept(File dir, String name) {
					File possibleDirectory = new File(dir, name);
					if (possibleDirectory.isDirectory()) {
						oneDirectory(possibleDirectory
								.list(new FilenameFilter() {
									// INNER CLASS !!!
									public boolean accept(File dir, String name) {
										return name.endsWith(JSP);
									}
								}), urlBase + name + "/");
						return false;
					}
					return name.endsWith(JSP);
				}
			});
		}
		if (DEBUG)
			System.out.println("jspFilenames=" + jspFilenames
					+ " for directory " + jspDir);
		oneDirectory(jspFilenames, urlBase);

		// send just one error summary to webmaster for all problems
		if (message.length() > 0) {
			// notify the webmaster pronto
			if (webmaster == null) {
				System.err
						.println("No webmaster to notify regarding problem in JSP compilation: "
								+ message.toString());
			} else {
				String hostName = "";
				try {
					hostName = InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException uhe) {
				}
				;
				try {
					String to = webmaster;
					String from = webmaster
							+ "(GenePattern JSP precompiler on " + hostName
							+ ")";
					String subject = numErrors + " JSP precompilation problem"
							+ (numErrors > 1 ? "s" : "") + " on " + hostName;
					if (DEBUG)
						System.out.println("notifying webmaster");
					String utf8 = "UTF-8";
					URL sendMail = new URL(urlBase + "sendMail.jsp?to="
							+ URLEncoder.encode(to, utf8) + "&from="
							+ URLEncoder.encode(from, utf8) + "&subject="
							+ URLEncoder.encode(subject, utf8) + "&mimeType="
							+ URLEncoder.encode("text/plain", utf8)
							+ "&message="
							+ URLEncoder.encode(message.toString(), utf8));

					BufferedReader in = new BufferedReader(
							new InputStreamReader(sendMail.openStream()));
					String inputLine = null;
					while (in.ready() && (inputLine = in.readLine()) != null) {
						if (DEBUG)
							System.out.println(inputLine);
					}
					in.close();
				} catch (Exception e) {
					System.err.println(e + " - unable to email webmaster");
				}
			}
		}
		System.out.println(new Date() + ": JSPPrecompiler done.");
	}

	protected void oneDirectory(String[] jspFilenames, String urlBase) {
		URL url = null;
		HttpURLConnection connection = null;
		BufferedReader in = null;
		String inputLine = null;
		int numConnectionExceptions = 0;

		for (int i = 0; i < jspFilenames.length; i++) {

			String urlString = urlBase + jspFilenames[i] + "?jsp_precompile";
			if (DEBUG)
				System.out.println("Precompiling " + urlString);

			try {
				url = new URL(urlString);
				do {
					connection = (HttpURLConnection) url.openConnection();
					numConnectionExceptions = 0;
					// No caching, we want the real thing.
					connection.setUseCaches(false);
					in = new BufferedReader(new InputStreamReader(connection
							.getInputStream()));
					while (in.ready() && (inputLine = in.readLine()) != null) {
						System.out.println(inputLine);
					}
					in.close();

					//					BUG: response code and message don't seem to work with
					// the
					//					?pre_compile request
					if (DEBUG) {
						System.out.println("response code: "
								+ connection.getResponseCode() + " "
								+ connection.getResponseMessage());
						System.out.println(jspFilenames[i] + " compiled");
					}
					break;
				} while (true);
			} catch (Exception e) {
				String errorMessage = "\n" + new Date() + ": ";
				if (!(e instanceof java.io.FileNotFoundException))
					errorMessage = errorMessage + e.getMessage()
							+ " while precompiling.  ";
				if (!(e instanceof java.net.ConnectException))
					errorMessage = errorMessage + "Probable syntax error.";
				System.err.println(errorMessage);
				message.append(errorMessage + "\n");
				numErrors++;

				/*
				 * TODO: knowing that there are errors, attempt to figure out
				 * which is the latest .java file for this .jsp file, compile it
				 * with javac and read the Process.inputStream to pipe the
				 * errors from the compiler into the error message text.
				 */
			}
		} // end loop for each JSP file
	}

	public static void main(String[] args) {
		System.out.println(new Date() + ": Starting JSP precompiler");
		try {
			JSPPrecompiler jspc = new JSPPrecompiler(
					"/local/omnigene_analysis_engine/Jetty-4.2.2/webapps/gp",
					"http://localhost:8080/gp/", "jlerner@broad.mit.edu");

			jspc.run();
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}