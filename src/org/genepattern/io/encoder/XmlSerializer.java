/*
 * XmlSerializer.java
 *
 * Created on June 30, 2003, 3:04 PM
 */

package org.genepattern.io.encoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.beans.BeanInfo;
import javax.beans.ExceptionListener;
import javax.beans.IntrospectionException;
import javax.beans.Introspector;
import javax.beans.PropertyDescriptor;
import javax.beans.XMLEncoder;

import org.genepattern.data.DataObjector;

/**
 * Class for saving a Java Object to a file in XML format
 * 
 * @author kohm
 */
public class XmlSerializer implements ExceptionListener,
		org.genepattern.io.encoder.Encoder {

	/** Creates a new instance of XmlSerializer */
	private XmlSerializer() {
		out = System.err;
	}

	// method from ExceptionListener
	public final void exceptionThrown(final Exception exception) {
		exception.printStackTrace(out);
		out.println("***    ***\n\n");
		out.flush();
	}

	/**
	 * Saves the DataModel into a XML file
	 * 
	 * @param object
	 *            DataModel for the analysis UI
	 * @param file
	 *            the xml file name
	 * @throws IOException
	 */
	public static final void saveData(final Object object, final File file)
			throws IOException {
		final BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		LISTENER.write(object, out);
	}

	//    /** test */
	//    public static final void main(final String[] args) throws Exception {
	//        //Test xml encoding
	//	final String test_file_name = "/tmp/old_ana_jobs.xml";
	//        System.out.println("loading in \""+test_file_name+"\"");
	//        final File file = new File(test_file_name);
	//        final Object dat_model =
	//        org.genepattern.io.DeSerializer.loadData(new File(test_file_name));
	//        
	//        //System.out.println("Saving DataModel as XML..");
	//        System.out.println("saving results as XML..");
	//        
	//        XmlSerializer.saveData(dat_model, new File("xml_serializer_out.xml"));
	//        System.out.println("Done!");
	//        System.exit(0);
	//    }

	/** encodes the data to the output stream */
	public void write(Object object, OutputStream out) throws IOException {
		final XMLEncoder encoder = new XMLEncoder(out);
		encoder.setExceptionListener(this);
		encoder.writeObject(object);
		encoder.close();
	}

	// Encoder interface methods
	/** encodes the data to the output stream */
	public void write(final DataObjector data, final OutputStream out)
			throws IOException {
		write((Object) data, out);
	}

	/** gets the file extension for the specified object or null if wrong type */
	public String getFileExtension(final DataObjector data) {
		return FILE_EXTENSION;
	}

	/**
	 * returns true if this can handle encoding the specified DataObjector FIXME
	 * not smart enough to determine if it can really serialize something
	 */
	public boolean canEncode(final DataObjector data) {
		return data.isMutable();//just a guess that if it is mutable then has
								// setters as well as getters
	}

	/** all Encoder implementations should be singleton classes without state */
	public static final org.genepattern.io.encoder.Encoder instance() {
		return LISTENER;
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
					//System.out.println(var+" is now transient");
					break;
				}
			}
		}
	}

	// fields
	/** the default file extension when writing this to file */
	public static final String FILE_EXTENSION = ".obj";

	/** where to print the errors to */
	private PrintStream out;

	/** the singleton of this */
	private static final XmlSerializer LISTENER;

	/** static initializer */
	static {
		// FIXME must get the right version of the serializer
		LISTENER = new XmlSerializer();
	}
	// I N N E R C L A S S E S

}