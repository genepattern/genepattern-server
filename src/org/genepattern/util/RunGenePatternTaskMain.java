/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;

/**
 * Create a network class loader to load Java bytecode from the remote
 * Omnigene/GenePattern server's tasklib directory (for a named task) and then
 * instantiate the public static void main(String args[]) method of that class
 */
public class RunGenePatternTaskMain {

	protected ClassLoader networkLoader = null;

	protected boolean debug = false;

	public RunGenePatternTaskMain(String host, String port, String taskName,
			String classPath, boolean debug) throws Throwable {
		this.debug = debug;
		// get a network classloader to extend the classpath to the GenePattern
		// installed tasks
		//networkLoader = new GenePatternNetworkClassLoader(host, port,
		// taskName, classPath, debug);
	}

	public RunGenePatternTaskMain(String host, String port, String taskName,
			String classPath) throws Throwable {
		this(host, port, taskName, classPath, false);
	}

	public Method getMain(String className) throws Exception {
		// load the class and invoke it's main(String args[]) method using the
		// network class loader
		//Class theClass = Class.forName(className, true, networkLoader);
		//Method main = theClass.getMethod("main", new Class[] { String[].class
		// });
		//return main;
		return null;
	}

	/**
	 * convenience method to create a network classloader, load the requested
	 * class from the specified classpath for a specific GenePattern task on a
	 * particular Omnigene server, and then invoke that class' main method,
	 * passing it the specified arguments
	 */
	public static void run(String className, String args[], String taskName,
			String host, String port, String classPath, boolean debug)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException,
			IllegalArgumentException, Throwable {
		RunGenePatternTaskMain rgptm = new RunGenePatternTaskMain(host, port,
				taskName, classPath, debug);
		if (debug) {
			System.out.println("RunGenePatternTaskMain.run: will use "
					+ taskName + " from " + host + ":" + port
					+ " with classPath " + classPath);
			System.out.print("Will invoke " + className + "'s main with args ");
			final int limit = args.length;
			for (int i = 0; i < limit; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(args[i]);
			}
			System.out.println("");
			//new Exception().printStackTrace();
		}
		try {
			rgptm.getMain(className).invoke(null, new Object[] { args });
		} catch (ClassNotFoundException cnfe) {
			//cnfe.printStackTrace();
			System.err
					.println("An error occurred while downloading the task files. Please make sure the task exists on the server and try again.");
			if (debug) {
				cnfe.printStackTrace();
			}
		} catch (NoSuchMethodException nsme) {
			throw new NoSuchMethodException(
					className
							+ " has no public static void main(String[] args) method on "
							+ host + ":" + port + " in task " + taskName
							+ " using classpath " + classPath);
		} catch (InvocationTargetException ite) {
			// if the invoked program throws an exception, rethrow it (unwrap it
			// from InvocationTargetException)
			Throwable e = ite.getTargetException();
			System.err.println(e.getMessage());
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			System.err.println("here");
		}
	}

	public static void run(String className, String args[], String taskName,
			String host, String port, String classPath)
			throws ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException,
			IllegalArgumentException, Throwable {
		run(className, args, taskName, host, port, classPath, false);
	}

	public static void main(String args[]) throws Exception {
		BufferedWriter out = new BufferedWriter(new FileWriter("runit.log"));

		try {
			if (args.length < 5) {
				throw new Exception(
						"Insufficient arguments to RunGenePatternTaskMain.\n"
								+ "Usage: RunGenePatternTaskMain className taskName host port classPath [taskArgs...]");
			}
			String className = args[0];
			String taskName = args[1];
			String host = args[2];
			String port = args[3];
			String classPath = args[4];

			out.write("\n0: " + args[0]);
			out.write("\n1: " + args[1]);
			out.write("\n2: " + args[2]);
			out.write("\n3: " + args[3]);
			out.write("\n4: " + args[4]);

			String taskArgs[] = new String[args.length - 5];
			System.arraycopy(args, 5, taskArgs, 0, taskArgs.length);
			boolean debug = (System.getProperty("DEBUG") != null);
			RunGenePatternTaskMain.run(className, taskArgs, taskName, host,
					port, classPath, debug);
		} catch (Throwable e) {
			e.printStackTrace();
			out.write("\nERROR: " + e);

			System.err.println(e);
		} finally {
			out.flush();
			out.close();
		}
	}

	public static void bpog(String args[]) {
		String className = "edu.mit.wi.gp.ui.pinkogram.BpogPanel";
		if (args.length == 0) {
			args = new String[] { "c:/local", "short.res" };
		}
		String taskName = "BluePinkOGram";
		String host = "pc11057.wi.mit.edu";
		String port = "8080";
		String classPath = "";
		try {
			RunGenePatternTaskMain.run(className, args, taskName, host, port,
					classPath, false);
		} catch (Throwable e) {
			System.err.println(e);
		}
	}

	public static void helloWorld(String args[]) {
		String className = "HelloWorld";
		if (args.length == 0) {
			args = new String[] { "hello", "world", "from", "Jim" };
		}
		String taskName = "foo";
		String host = "pc11057.wi.mit.edu";
		String port = "8080";
		String classPath = "";
		// http://localhost:8080/gp/getClassBytes.jsp?taskName=foo&className=HelloWorld&classPath=%3Clibdir%3EThresholdFilter.jar%3B%3Clibdir%3E
		try {
			RunGenePatternTaskMain.run(className, args, taskName, host, port,
					classPath, false);
		} catch (Throwable e) {
			System.err.println(e);
		}
	}
}

/*
 * ClassLoader that loads GenePattern task-affiliated classes and resources over
 * the network. It works in cooperation with the getClassBytes.jsp page to
 * return the bytes from a file or jar file on the GenePattern server.
 * 
 * Since each GenePattern task has it's own set of jar files and classpath
 * definitions, and since they are specifically isolated in a per-task directory
 * to avoid conflict, the caller who wants to load a GenePattern class or
 * resource must specify the host and port name on which GenePattern is running,
 * the task name whose jars and files are to be searched, and the classpath
 * specifying within that task which jars, etc. are to be searched.
 * 
 * @author Jim Lerner
 * 
 * @see java.lang.ClassLoader
 * 
 * 
 * TODO: should override these as well??: Enumeration getResources(String name),
 * String findLibrary(String libraryName)
 */

class GenePatternNetworkClassLoader extends ClassLoader {
	String host = null;

	String port = null;

	String taskName = null;

	String classPath = null;

	String GP_URI = "/gp/getClassBytes.jsp"; // TODO: BUG: specify context at
											 // invocation time?

	boolean DEBUG = false;

	public GenePatternNetworkClassLoader(String host, String port,
			String taskName, String classPath, boolean debug) {
		this.host = host;
		this.port = port;
		this.taskName = taskName;
		this.classPath = classPath;
		this.DEBUG = debug;
	}

	public GenePatternNetworkClassLoader(String host, String port,
			String taskName, String classPath) {
		this(host, port, taskName, classPath, false);
	}

	public Class findClass(String className) throws ClassNotFoundException {
		try {
			byte[] classBytes = loadClassData(className);
			return defineClass(className, classBytes, 0, classBytes.length);
		} catch (Exception e) {
			throw new ClassNotFoundException(e.getMessage());
		}
	}

	protected URL findResource(String name) {
		URL resourceURL = null;
		try {
			byte[] b = fetchBytes(name);
			resourceURL = makeURL(name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resourceURL;
	}

	protected Enumeration findResources(String name) throws IOException {
		Enumeration ret = super.findResources(name);
		return ret;
	}

	public InputStream getResourceAsStream(String name) {
		InputStream is = null;
		try {
			is = new ByteArrayInputStream(fetchBytes(name));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return is;
	}

	private byte[] loadClassData(String className)
			throws MalformedURLException, IOException {
		// load the class data from the connection
		className = className.replace('.', '/') + ".class";
		return fetchBytes(className);
	}

	private byte[] fetchBytes(String className) throws MalformedURLException,
			IOException {
		URL remoteClassFinder = makeURL(className);
		InputStream is = remoteClassFinder.openStream();
		byte[] buf = getBytesFromStream(is, className);
		return buf;
	}

	private URL makeURL(String className) throws MalformedURLException,
			UnsupportedEncodingException {
		String GPAddress = "http://" + host + ":" + port + GP_URI
				+ "?taskName=" + URLEncoder.encode(taskName, "UTF-8")
				+ "&className=" + URLEncoder.encode(className, "UTF-8")
				+ "&classPath=" + URLEncoder.encode(classPath, "UTF-8")
				+ (DEBUG ? "&debug=1" : "");
		return new URL(GPAddress);
	}

	private byte[] getBytesFromStream(InputStream is, String className)
			throws IOException, NumberFormatException {
		int i;
		// first getClassBytes.jsp sends a 6 digit (trailing space-padded) class
		// length to help us allocate sufficient space
		byte[] classLengthBytes = new byte[6];
		is.read(classLengthBytes, 0, 6);
		int len = Integer.parseInt(new String(classLengthBytes).trim());
		// allocate the required space
		byte[] buf = new byte[len];
		len = 0;
		// read in the class bytes
		for (len = 0; (i = is.read(buf, len, buf.length - len)) > 0; len += i) {
		}
		is.close();
		if (len != buf.length) {
			throw new IOException(className + " on " + host + ":" + port
					+ " for GenePattern task " + taskName
					+ " is shorter than buffer size of " + buf.length);
		}
		return buf;
	}
}
