/*
 * DataHandlerOmniView.java
 *
 * Created on June 30, 2003, 3:04 PM
 */

package org.genepattern.gpge.ui.maindisplay;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.beans.BeanInfo;
import javax.beans.DefaultPersistenceDelegate;
import javax.beans.Encoder;
import javax.beans.ExceptionListener;
import javax.beans.IntrospectionException;
import javax.beans.Introspector;
import javax.beans.PropertyDescriptor;
import javax.beans.Statement;

import org.genepattern.gpge.ui.tasks.DataModel;
import org.genepattern.io.DeSerializer;
import org.genepattern.io.encoder.XmlSerializer;
import org.genepattern.util.StringUtils;

/**
 * Class for loading and saving OmniView's DataModel
 * 
 * @author kohm
 */
public class DataHandlerOmniView implements ExceptionListener {

	/** Creates a new instance of DataHandlerOmniView */
	private DataHandlerOmniView() {
		out = System.err;
	}

	// method from ExceptionListener
	public final void exceptionThrown(final Exception exception) {
		exception.printStackTrace(out);
		out.println("***    ***\n\n");
		out.flush();
	}

	/**
	 * This method is a bit of a hack to convert the bean formated
	 * analysis_jobs.xml class names into the new packaged names. If we can get
	 * to it we ought to change the format to remnove the class name from the
	 * file and use a proper serializer than can remap to different classes as
	 * needed.
	 */

	public static InputStream translateFromOmniview(File file)
			throws IOException {
		String contents = null;
		final InputStream in = new FileInputStream(file);
		contents = org.genepattern.io.StorageUtils
				.createStringFromReader(new InputStreamReader(in));
		in.close();
		// XXX hack
		contents = contents.replaceAll("edu.mit.wi.omniview",
				"edu.mit.genome.gp.ui");

		contents = contents.replaceAll(
				"edu.mit.genome.gp.ui.analysis.DataModel",
				"org.genepattern.gpge.ui.tasks.DataModel");

		contents = contents.replaceAll(
				"edu.mit.wi.omnigene.framework.analysis",
				"org.genepattern.webservice"); // rename ParameterInfo, JobInfo

		contents = contents.replaceAll("edu.mit.genome.gp.ui.analysis",
				"org.genepattern.webservice"); // rename AnalysisJob

		return new ByteArrayInputStream(contents.getBytes());
	}

	/**
	 * Load data from a xml file and restore it to <code>DataModel<code>
	 * @param file XML file containing result and history
	 * @return DataModel for analysis UI
	 * @throws FileNotFoundException if the file does not exist, 
	 *  is a directory rather than a regular file, or for some other reason cannot
	 *  be opened for reading
	 */
	public static final DataModel loadData(final File file) throws IOException {
		final InputStream translatedFileIS = translateFromOmniview(file);
		final Object result = DeSerializer.decode(translatedFileIS, LISTENER,
				LISTENER);
		return (DataModel) result;
	}

	/**
	 * Saves the DataModel into a XML file
	 * 
	 * @param model
	 *            DataModel for the analysis UI
	 * @param fileName
	 *            the xml file name
	 * @throws IOException
	 */
	public static final void saveData(final DataModel model, final File file)
			throws IOException {
		XmlSerializer.saveData(model, file);
	}

	/** test */
	public static final void main(final String[] args) throws Exception {
		//Test xml encoding
		final String test_file_name = "c:/temp/old_ana_jobs.xml";
		System.out.println("loading in \"" + test_file_name + "\"");
		final File file = new File(test_file_name);
		final org.genepattern.gpge.ui.tasks.DataModel dat_model = DataHandlerOmniView
				.loadData(file);

		System.out.println("saving results as XML..");

		DataHandlerOmniView.saveData(dat_model, new File(test_file_name
				+ "_out.xml"));
		System.out.println("Done!");
		System.exit(0);
	}

	// helpers
	/** makes the bean patterns (properties) transient */
	protected static final void makeTransient(final Class klass,
			final String[] vars) throws IntrospectionException {
		final BeanInfo info = Introspector.getBeanInfo(klass);
		final PropertyDescriptor[] propertyDescriptors = info
				.getPropertyDescriptors();
		final int desc_cnt = propertyDescriptors.length;
		final int vars_cnt = vars.length;
		for (int i = 0; i < desc_cnt; ++i) {
			final PropertyDescriptor pd = propertyDescriptors[i];
			final String pd_name = pd.getName();
			for (int j = 0; j < vars_cnt; j++) {
				final String var = vars[j];
				if (pd_name.equals(var)) {
					pd.setValue("transient", Boolean.TRUE);
					System.out.println(var + " is now transient");
					break;
				}
			}
		}
	}

	// fields
	/** where to print the errors to */
	private PrintStream out;

	/** the singleton of this */
	private static final DataHandlerOmniView LISTENER = new DataHandlerOmniView();

	/** static initializer */
	static {
		try {
			// DataModel
			makeTransient(DataModel.class, new String[] { "analysisServices",
					"dataSourceURL" });
		} catch (Throwable t) {
			//FIXME need to report error
			System.err.println(t);
		}
		try {
			// JobInfo
			makeTransient(org.genepattern.webservice.JobInfo.class,
					new String[] { "parameterInfo" });
		} catch (Throwable t) {
			//FIXME need to report error
			System.err.println(t);
		}
		// ParameterInfo
		try {
			makeTransient(org.genepattern.webservice.ParameterInfo.class,
					new String[] { "inputFile", "label", "outputFile", });
		} catch (Throwable t) {
			//FIXME need to report error
			System.err.println(t);
		}

		//AnalysisJob
		final Encoder encoder = new Encoder();
		encoder.setPersistenceDelegate(DataModel.class,
				new DataModelPersistenceDelegate());
		encoder.setPersistenceDelegate(
				org.genepattern.webservice.AnalysisJob.class,
				new DefaultPersistenceDelegate(new String[] { "siteName",
						"taskName", "jobInfo" }));// name of a getter:
												  // AnalysisJob.getJobInfo()
	}

	// I N N E R C L A S S E S

	/** constructs a DataModel */
	static final class DataModelPersistenceDelegate extends
			DefaultPersistenceDelegate {
		protected void initialize(Class type, Object oldInstance,
				Object newInstance, Encoder out) {
			//super.initialize(type, oldInstance, newInstance, out);

			final DataModel old_data = (DataModel) oldInstance;
			System.out.println("old_data");
			final Vector old_jobs = old_data.getJobs();
			System.out.println("old_jobs");
			final Hashtable old_results = old_data.getResults();
			System.out.println("old_results");

			final Vector jobs = (Vector) old_jobs.clone();
			final Hashtable results = (Hashtable) old_results.clone();
			System.out.println("creating the writeStatement..");
			out.writeStatement(new Statement(oldInstance, "resetData",
					new Object[] { jobs, results }));
			System.out.println("initialize done");
		}
	}
}