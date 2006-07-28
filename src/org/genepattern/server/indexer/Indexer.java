/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.server.indexer;

// Lucene comes from http://jakarta.apache.org/lucene
// PDFBox comes from http://sourceforge.net/project/showfiles.php?group_id=78314
// or http://www.pdfbox.org

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.genepattern.GPLuceneAnalyzer;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.dao.AnalysisJobService;

public class Indexer {

	protected static Object concurrencyLock = new Object();

	// field names used in Lucene index files
	public static String URL = "url";

	public static String FILENAME = "filename";

	public static String TASKNAME = "taskName";

	public static String CONTENTS = "contents";

	public static String JOBID = "jobID";

	public static String JOB_HAS_OUTPUT = "jobHasOutput";

	public static String TYPE = "type";

	public static String TASKID = "taskid";

	public static String FILEID = "fileid";

	public static String TITLE = "title";

	public static String LSID = "lsid";

	// names for fields that the user will be selecting for searches
	public static String TASK = "tasks";

	public static String TASK_DOC = "taskDocumentation";

	public static String TASK_SCRIPTS = "taskScripts";

	public static String JOB_PARAMETERS = "jobParameters";

	public static String JOB_OUTPUT = "jobOutput";

	public static String MANUAL = "manual";

	protected static PrintWriter out = new PrintWriter(System.out, true);

	// BUG: This won't handle large job output files.
	// TODO: Need to write a custom Lucene Analyzer and Tokenizer to handle
	// these files.
	protected static int MAX_TERMS_PER_FIELD = 2000000; // 2 million

	protected static IndexWriter writer = null;

	protected static IndexReader reader = null;

	// TODO: enrich the analyzer with per-field stuff
	// (eg. StandardAnalyzer for most fields, WhitespaceAnalyzer for data files)
	public static Analyzer GPAnalyzer = new PerFieldAnalyzerWrapper(
			new GPLuceneAnalyzer() /* WhitespaceAnalyzer() */);

	public Indexer(Writer out) {
		Indexer.out = new PrintWriter(out, true);
	}

	public Indexer(OutputStream out) {
		Indexer.out = new PrintWriter(out, true);
	}

	public Indexer() {
		this(System.out);
	}

	public static IndexWriter getWriter() throws IOException {
		synchronized (getConcurrencyLock()) {
			if (writer == null) {
				// wait until writer is not in use
				while (reader != null) {
					try {
						Thread.currentThread().sleep(100);
					} catch (InterruptedException ie) {
						// ignore
					}
				}
				writer = new IndexWriter(getIndexDir(), Indexer.GPAnalyzer,
						false);
			}
		}
		return writer;
	}

	public static IndexWriter releaseWriter() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException ioe) {
				// ignore
			}
			writer = null;
		}
		return writer;
	}

	public static IndexReader getReader() throws IOException {
		synchronized (getConcurrencyLock()) {
			if (reader == null) {
				// wait until writer is not in use
				while (writer != null) {
					try {
						Thread.currentThread().sleep(100);
					} catch (InterruptedException ie) {
						// ignore
					}
				}
				reader = IndexReader.open(getIndexDir());
			}
		}
		return reader;
	}

	public static IndexReader releaseReader() {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException ioe) {
				// ignore
			}
			reader = null;
		}
		return reader;
	}

	public static void reset(File indexDir) throws IOException {
		indexDir.mkdirs();
		// if an old lock file was left around, delete it
		FSDirectory.getDirectory(indexDir, true).makeLock(
				IndexWriter.WRITE_LOCK_NAME).release();
		synchronized (getConcurrencyLock()) {
			writer = new IndexWriter(indexDir, GPAnalyzer, true);
			writer = releaseWriter();
		}
	}

	public static void optimize(File indexDir) throws IOException {
		synchronized (getConcurrencyLock()) {
			writer = getWriter();
			optimize(writer);
			writer = releaseWriter();
		}
	}

	public static void optimize(IndexWriter writer) throws IOException {
		writer.optimize(); // compacts storage after inserts
	}

	public static void createIfNecessary(File indexDir) throws IOException,
			OmnigeneException, Exception {
		if (indexDir.exists() && new File(indexDir, "segments").exists())
			return;
		//System.out.println("Indexer: resetting and reindexing everything");
		reset(indexDir);
		index(indexDir);
	}

	public static void index(File indexDir) throws IOException,
			OmnigeneException, Exception {
		// TODO: change Analyzer to one that is more appropriate.
		// Should probably ignore floating point numbers, use punctuation and
		// spaces as separators, ignore single-letter words
		indexDir.mkdirs();
		// NB: last parameter overwrites old index when set to TRUE
		synchronized (getConcurrencyLock()) {
			try {
				writer = getWriter();
				//indexManual(writer);
				indexTasks(writer);
				indexJobs(writer);
				optimize(writer);
			} finally {
				writer = releaseWriter();
			}
		}
	}

	public static void indexTasks(IndexWriter writer) throws IOException,
			OmnigeneException, Exception {
		Collection tmTasks = GenePatternAnalysisTask.getTasks(null);
		for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext();) {
			TaskInfo ti = (TaskInfo) itTasks.next();
			indexTask(writer, ti.getID());
		}
	}

	public static void indexJobs(IndexWriter writer) throws IOException,
			OmnigeneException {
		AnalysisJobService ds = AnalysisJobService.getInstance();
		JobInfo[] jobs = ds.getJobInfo(new Date());
		JobInfo jobInfo = null;
		for (int i = 0; i < jobs.length; i++) {
			jobInfo = jobs[i];
			try {
				indexJob(writer, jobInfo, ds);
			} catch (Exception e) {
			}
		}
	}

	public static void indexJob(IndexWriter writer, int jobID)
			throws IOException, OmnigeneException {
        AnalysisJobService ds = AnalysisJobService.getInstance();
		JobInfo jobInfo = ds.getJobInfo(jobID);
		indexJob(writer, jobInfo, ds);
	}

	public static void indexJob(IndexWriter writer, JobInfo jobInfo,
            AnalysisJobService ds) throws IOException, OmnigeneException {
		synchronized (getConcurrencyLock()) {
			writer.maxFieldLength = MAX_TERMS_PER_FIELD;
			String jobStatus = jobInfo.getStatus();
			ParameterInfo params[] = jobInfo.getParameterInfoArray();

			// very slow to get return to user when it asks for task name!
			TaskInfo taskInfo = null;
			int taskID = jobInfo.getTaskID();
			int jobID = jobInfo.getJobNumber();
			try {
				if (taskID != -1) {
					taskInfo = ds.getTask(taskID);
				}
			} catch (OmnigeneException oe) {
				// NoTaskFoundException
			}
			String taskName = "";
			LSID lsid = null;
			if (taskID != -1) {
				try {
					lsid = new LSID((String) taskInfo.getTaskInfoAttributes()
							.get(GPConstants.LSID));
				} catch (MalformedURLException mue) {
					// ignore
				}
			}
			if (taskInfo != null) {
				taskName = taskInfo.getName()
						+ (lsid != null ? (" - " + lsid.getVersion()) : "");
			} else {
				taskName = ds.getTemporaryPipelineName(jobID);
			}
			boolean hasOutputFiles = false;

			out.println("Indexing job and parameters for job " + jobID
					+ ", task " + taskName);
			Document doc = new Document();
			StringBuffer content = new StringBuffer();
			doc.add(Field.Text(TASKNAME, taskName));
			doc.add(Field.Text(JOBID, "" + jobID));
			if (lsid != null)
				doc.add(Field.Text(LSID, lsid.toString()));
			doc.add(Field.UnIndexed(URL, "getJobResults.jsp?jobID=" + jobID));
			doc.add(Field.Text(TYPE, "job"));
			content.append(taskName);
			content.append(" ");
			if (taskInfo != null)
				content.append(taskInfo.getDescription());
			content.append(" ");
			content.append(jobID);
			content.append(" ");
			content.append(jobInfo.getUserId());
			content.append(" ");
			boolean hasOutput = false;
			if (params != null) {
				for (int p = 0; p < params.length; p++) {
					content.append(" ");
					content.append(params[p].getName());
					content.append(" ");
					content.append(params[p].getValue());
					content.append(" ");
					content.append(params[p].getDescription());
					if (params[p].isOutputFile()) { // &&
													// waitFileExists(params[p].getValue()))
													// {
						hasOutput = true;
					}
				}
			}
			doc.add(Field.Text(JOB_PARAMETERS, content.toString()));
			if (hasOutput)
				doc.add(Field.Text(JOB_HAS_OUTPUT, "1"));
			writer.addDocument(doc);

			if (params != null) {
				for (int p = 0; p < params.length; p++) {
					if (params[p].isOutputFile()) {
						String filename = params[p].getValue();
						if (isBinaryFile(filename))
							continue;
						/*
						 * boolean bAlreadyDone = false; for (int p2 = 0; p2 <
						 * p; p2++) { if (params[p2].isOutputFile() &&
						 * filename.equals(params[p2].getValue())) {
						 * bAlreadyDone = true; break; } } if (bAlreadyDone)
						 * continue;
						 */
						doc = new Document();
						File f = new File(System.getProperty("jobs"), filename);
						if (!waitFileExists(f)) {
							out.println(f.getName() + " for job " + jobID
									+ " has been deleted.");
							continue;
						}
						out.println("Indexing output file " + f.getName()
								+ " for job " + jobID + ", task " + taskName);
						String childJobNumber = f.getParentFile().getName();
						FileReader fr = new FileReader(f);
						try {
							doc.add(Field.Text(JOB_OUTPUT, fr));
							doc.add(Field.Text(TASKNAME, taskName));
							doc.add(Field.Text(JOBID, "" + jobID));
							if (lsid != null)
								doc.add(Field.Text(LSID, lsid.toString()));
							doc.add(Field.Text(FILENAME, f.getName()));
							doc.add(Field.UnIndexed(URL,
									"retrieveResults.jsp?job="
											+ childJobNumber
											+ "&filename="
											+ URLEncoder.encode(f.getName(),
													"UTF-8")));
							doc.add(Field.Text(JOB_HAS_OUTPUT, "1"));
							doc.add(Field.Text(TYPE, "output"));
							doc.add(Field.Text(FILEID, "" + jobID + "/"
									+ filename));
							writer.addDocument(doc);
						} catch (Throwable t) {
							System.err.println(t.getMessage()
									+ " while indexing " + filename
									+ " for job " + jobID);
						} finally {
							try {
								fr.close();
							} catch (IOException ioe) {
								// ignore
							}
						}
					}
				}
			}
		}
	}

	public static void indexManual(IndexWriter writer) {
		out.println("Indexing user manual and tutorial");
		File[] docs = new File("webapps/gp/docs")
				.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".htm") || name.endsWith(".html");
					}
				});
		IndexManualHTML indexManualHTML = new IndexManualHTML();
		String name = null;
		synchronized (getConcurrencyLock()) {
			for (int f = 0; f < docs.length; f++) {
				name = docs[f].getName();
				try {
					out.println("Indexing " + name);
					Document doc = indexManualHTML.index(docs[f]);
					doc.add(Field.Text(TYPE, MANUAL));
					doc.add(Field.UnIndexed(URL, "docs/"
							+ URLEncoder.encode(name, "UTF-8")));
					doc.add(Field.Text(FILENAME, name));
					writer.addDocument(doc);
				} catch (IOException ioe) {
					out.println(ioe.getMessage() + " while indexing " + name);
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	public static int deleteJob(int jobID) throws IOException {
		return deleteJob(Integer.toString(jobID));
	}

	public static int deleteJob(String jobID) throws IOException {
		synchronized (getConcurrencyLock()) {
			IndexReader reader = getReader();
			int numDeleted = 0;
			try {
				numDeleted = reader.delete(new Term(JOBID, jobID));
			} finally {
				reader = releaseReader();
			}
			return numDeleted;
		}
	}

	public static int deleteJobFile(int jobID, String filename)
			throws IOException {
		return deleteJobFile(Integer.toString(jobID), filename);
	}

	public static int deleteJobFile(String jobID, String filename)
			throws IOException {
		synchronized (getConcurrencyLock()) {
			IndexReader reader = getReader();
			int numDeleted = 0;
			try {
				numDeleted = reader.delete(new Term(FILEID, jobID + "/"
						+ filename));
			} finally {
				reader = releaseReader();
			}
			return numDeleted;
		}
	}

	public static int deleteTask(int taskID) throws IOException {
		synchronized (getConcurrencyLock()) {
			int numDeleted = 0;
			IndexReader reader = getReader();
			try {
				numDeleted = reader.delete(new Term(TASKID, "" + taskID));
			} finally {
				reader = releaseReader();
			}
			return numDeleted;
		}
	}

	public static int deleteTask(String name) throws IOException,
			OmnigeneException {
		TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(name, null);
		return deleteTask(taskInfo.getID());
	}

	public static void indexTask(IndexWriter writer, int taskID)
			throws IOException, OmnigeneException, Exception {
		synchronized (getConcurrencyLock()) {
			writer.maxFieldLength = MAX_TERMS_PER_FIELD;
			AnalysisJobService ds = GenePatternAnalysisTask.getDS();
			TaskInfo ti = ds.getTask(taskID);

			// XXX: delete first in case it already exists?
			//deleteTask(ti.getID());

			TaskInfoAttributes tia = ti.giveTaskInfoAttributes();
			String name = ti.getName();
			String lsid = (String) tia.get(GPConstants.LSID);
			LSID l = null;
			try {
				l = new LSID(lsid);
			} catch (MalformedURLException mue) {
				// ignore
				lsid = name;
			}

			out.println("Indexing " + name + " (" + lsid + ")");

			Document doc = new Document();
			StringBuffer content = new StringBuffer();
			doc.add(Field.Text(TASKNAME, name + " - " + l.getVersion()));
			content.append(name);
			content.append(" ");
			content.append(ti.getDescription());
			content.append(" ");
			content.append(tia.toString());
			content.append(" ");
			content.append(ti.getUserId());
			content.append(" ");

			ParameterInfo[] parameterInfoArray = new ParameterFormatConverter()
					.getParameterInfoArray(ti.getParameterInfo());
			for (int i = 0; parameterInfoArray != null
					&& i < parameterInfoArray.length; i++) {
				ParameterInfo pi = parameterInfoArray[i];
				content.append(pi.getName());
				content.append(" ");
				content.append(pi.getValue());
				content.append(" ");
				content.append(pi.getDescription());
				content.append(" ");

				HashMap pia = pi.getAttributes();
				// TODO: add default value?
			}
			doc.add(Field.UnIndexed(URL, "addTask.jsp?" + GPConstants.NAME
					+ "=" + lsid + "&view=1"));

			// index documentation files for this task
			String taskLibDir = DirectoryManager.getTaskLibDir(lsid);
			String[] docs = new File(taskLibDir).list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.endsWith(".old");
				}
			});

			for (int i = 0; i < docs.length; i++) {
				content.append(docs[i]);
				content.append(" ");
			}
			doc.add(Field.Text(TASK, content.toString()));
			doc.add(Field.Text(TYPE, "task"));
			doc.add(Field.Text(TASKID, "" + ti.getID()));
			doc.add(Field.Text(LSID, lsid));
			writer.addDocument(doc);

			// index documentation files for this task
			for (int i = 0; i < docs.length; i++) {
				String filename = docs[i];
				File f = new File(taskLibDir, filename);
				doc = null;

				if (!f.isFile())
					continue;
				if (isBinaryFile(filename))
					continue;
				boolean codeFile = isCodeFile(filename);

				out.println("Indexing " + filename);
				try {
					String pkgName = Indexer.class.getPackage().getName();
					String indexerClassName = pkgName + "." + "Index"
							+ getExtension(filename).toUpperCase();
					Class cls = null;
					try {
						cls = Class.forName(indexerClassName);
					} catch (ClassNotFoundException cnfe) {
						//out.println("no indexer class found for files of type
						// " + getExtension(filename) + ": " + filename + ",
						// using text indexer.");
						indexerClassName = pkgName + "." + "IndexTXT";
						cls = Class.forName(indexerClassName);
					}
					Object instance = cls.newInstance();
					Method mExec = cls.getMethod("index",
							new Class[] { File.class });

					try {
						doc = (Document) mExec.invoke(instance,
								new Object[] { f });
					} catch (InvocationTargetException ite) {
						out.println(ite.getMessage() + " while indexing "
								+ filename + " (not indexed)");
						ite.printStackTrace();
						continue;
					}

					if (codeFile) {
						// change the indexing from TASK_DOC to TASK_SCRIPTS
						Reader code = doc.getField(TASK_DOC).readerValue();
						doc = new Document();
						doc.add(Field.Text(TASK_SCRIPTS, code));
					}
					/*
					 * for (Enumeration eFields = doc.fields();
					 * eFields.hasMoreElements(); ) { Field fld =
					 * (Field)eFields.nextElement(); out.println(fld.name() +
					 * "=" + fld.stringValue()); }
					 */
					doc.add(Field.UnIndexed(FILENAME, filename));
					doc.add(Field.Keyword(TASKNAME, name));
					doc.add(Field.Text(TYPE, "doc"));
					doc.add(Field.UnIndexed(URL, "getTaskDoc.jsp?"
							+ GPConstants.NAME + "=" + lsid + "&file="
							+ URLEncoder.encode(filename, "UTF-8")));
					doc.add(Field.Text(TASKID, "" + ti.getID()));
					doc.add(Field.Text(LSID, lsid));
					writer.addDocument(doc);
				} catch (Throwable t) {
					out.println(t.getMessage() + " while indexing " + filename
							+ " (not indexed)");
					t.printStackTrace();
				}
			}
		}
	}

	public static File getIndexDir() throws IOException {
		String GPResources = System.getProperty("index");
		File indexDir = new File(GPResources);
		return indexDir;
	}

	public static void indexDatabase() throws Exception {
		File indexDir = getIndexDir();
		try {
			index(indexDir);
		} catch (Throwable t) {
			out.println(t);
		}
	}

	public static String getExtension(String filename) {
		int e = filename.lastIndexOf(".");
		return (e != -1 ? filename.substring(e + 1) : "");
	}

	public static boolean isBinaryFile(String filename) {
		return GenePatternAnalysisTask.isBinaryFile(filename);
	}

	public static boolean isCodeFile(String filename) {
		return GenePatternAnalysisTask.isCodeFile(filename)
				|| filename.equals("version.txt");
	}

	public static boolean waitFileExists(String filename) {
		return waitFileExists(new File(filename));
	}

	public static boolean waitFileExists(File f) {
		long startTime = System.currentTimeMillis();
		for (int retries = 1; retries < 20; retries++) {
			if (f.exists()) {
				return true;
			}
			// sleep and retry in case Indexer is busy with this file right now
			try {
				Thread.sleep(100 * retries);
			} catch (InterruptedException ie) {
			}
		}
		startTime = (System.currentTimeMillis() - startTime) / 1000;
		String name = "";
		try {
			name = f.getCanonicalPath();
		} catch (IOException ioe) {
		}
		System.err.println("Indexer: timed out waiting " + startTime
				+ " seconds for " + name + " to reappear.");
		return false;

	}

	public static Object getConcurrencyLock() {
		return concurrencyLock;
	}

	public static void main(String[] args) throws Exception {
		String help = "java edu.mit.wi.omnigene.service.analysis.genepattern.Indexer [index all | index taskID | index jobID | delete taskID | delete jobID | delete jobID filename]";

		if (args.length >= 2) {
			String GPHome = System.getProperty("GPHome",
					"C:/Program Files/GenePattern");
			System.setProperty("genepattern.properties", GPHome + "/resources");
			System.setProperty("user.dir", GPHome + "/Tomcat");
			System.setProperty("log4j.configuration", GPHome
					+ "/Tomcat/webapps/gp/WEB-INF/classes/log4j.properties");
			File indexDir = new File(GPHome, "index");
			Indexer indexer = new Indexer(System.out);

			String cmd = args[0]; // index or delete
			String indexType = args[1]; // task, job, file, or all
			String ID = (args.length > 2 ? args[2] : null);
			String filename = (args.length > 3 ? args[3] : null);
			IndexWriter writer = null;

			// delete [task | job | file] [taskName | jobID | jobID]
			// [filename_for_job]
			synchronized (getConcurrencyLock()) {
				try {
					int numDeleted = 0;
					if (cmd.equals("delete")) {
						if (indexType.equals("task")) {
							numDeleted = Indexer.deleteTask(ID);
						} else if (indexType.equals("job")) {
							numDeleted = Indexer.deleteJob(ID);
						} else if (indexType.equals("file")) {
							numDeleted = Indexer.deleteJobFile(ID, filename);
						}
						System.out.println("deleted " + numDeleted
								+ " documents from index");
					} else if (cmd.equals("index")) {
						if (indexType.equals("task")) {
							writer = new IndexWriter(indexDir, GPAnalyzer,
									false);
							Indexer.indexTask(writer, Integer.parseInt(ID));
						} else if (indexType.equals("job")) {
							writer = new IndexWriter(indexDir, GPAnalyzer,
									false);
							Indexer.indexJob(writer, Integer.parseInt(ID));
						} else if (indexType.equals("all")) {
							Indexer.reset(indexDir);
							Indexer.index(indexDir);
						}
					} else if (cmd.equals("help") || cmd.equals("?")) {
						System.err.println(help);
					} else {
						System.err.println("Don't understand command " + cmd);
						System.err.println(help);
					}
				} catch (Throwable t) {
					System.err.println(t);
				} finally {
					writer = releaseWriter();
				}
			}
		} else {
			System.err.println(help);
		}
	}
}