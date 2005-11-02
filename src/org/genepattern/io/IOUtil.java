package org.genepattern.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.data.matrix.ClassVector;
import org.genepattern.io.expr.ExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.io.expr.IExpressionDataWriter;
import org.genepattern.io.expr.cls.ClsReader;
import org.genepattern.io.expr.cls.ClsWriter;
import org.genepattern.io.expr.gct.GctReader;
import org.genepattern.io.expr.gct.GctWriter;
import org.genepattern.io.expr.odf.OdfDatasetReader;
import org.genepattern.io.expr.odf.OdfDatasetWriter;
import org.genepattern.io.expr.res.ResReader;
import org.genepattern.io.expr.res.ResWriter;

/**
 * A class containing static convenience methods for reading and writing data
 * 
 * @author Joshua Gould
 */
public class IOUtil {
	static Map suffix2ExpressionReaders;

	static ClsReader clsReader = new ClsReader();

	static Map formatNameToExpressionWriterMap;

	private static boolean debug = false;

	private static FeatureListReader featureListReader = new FeatureListReader();

	private IOUtil() {
	}

	/**
	 * Reads the cls document at the given pathname
	 * 
	 * @param pathname
	 *            The pathname string
	 * @return The class vector
	 * @exception IOException
	 *                If an error occurs while reading from the file
	 * @exception ParseException
	 *                If there is a problem with the data
	 */
	public static ClassVector readCls(String pathname) throws IOException,
			ParseException {
		return clsReader.read(pathname);
	}

	/**
	 * Gets a list of features at the given file pathname string
	 * 
	 * @param pathname
	 *            The pathname string
	 * @return the feature list
	 * @exception IOException
	 *                If an error occurs while reading from the file
	 */
	public static List readFeatureList(String pathname) throws IOException {
		return featureListReader.read(pathname);
	}
   
  /**
	 * Writes the given feature list to a file
	 * 
	 * @param features
	 *            The features
	 * @param pathname
	 *            A pathname string
	 * @exception IOException
	 *                If an error occurs while saving the data
	 */
	public static void writeFeatureList(String[] features, String pathname) throws IOException {
		PrintWriter pw = null;
      try {
         pw = new PrintWriter(new FileWriter(pathname));
         for(int i = 0; i < features.length; i++) {
            pw.println(features[i]);  
         }
         
      } finally {
         if(pw!=null) {
            pw.close();
         }
      }
     
	}

	/**
	 * Reads the expression data at the given pathname. The type of the returned
	 * object is determined by the expressionDataCreator argument.
	 * 
	 * @param pathname
	 *            The file pathname
	 * @param expressionDataCreator
	 *            The expression data creator
	 * @return An object containing the expression data
	 * @exception IOException
	 *                If an error occurs while reading from the file
	 * @exception ParseException
	 *                If there is a problem with the data
	 */
	public static Object readExpressionData(String pathname,
			IExpressionDataCreator expressionDataCreator) throws IOException,
			ParseException {
		IExpressionDataReader reader = getExpressionReader(pathname);
		return reader.read(pathname, expressionDataCreator);
	}
   
   /**
	 * Reads the expression data at the given pathname and returns a new 
    * <tt>ExpressionData</tt> instance.
	 * 
	 * @param pathname
	 *            The file pathname
	 * @return An <tt>ExpressionData</tt> instance
	 * @exception IOException
	 *                If an error occurs while reading from the file
	 * @exception ParseException
	 *                If there is a problem with the data
	 */
	public static ExpressionData readExpressionData(String pathname) throws IOException,
			ParseException {
      return (ExpressionData) readExpressionData(pathname, new ExpressionDataCreator());
	}

	/**
	 * Writes the given class vector to a file
	 * 
	 * @param cv
	 *            The class vector
	 * @param pathname
	 *            A pathname string
	 * @param checkFileExtension
	 *            Whether the correct file extension will be added to the
	 *            pathname if it is not present.
	 * @return The pathname that the data was saved to
	 * @exception IOException
	 *                If an error occurs while saving the data
	 */
	public static String writeCls(ClassVector cv, String pathname,
			boolean checkFileExtension) throws IOException {
		FileOutputStream fos = null;
		try {
			ClsWriter writer = new ClsWriter();
			if (checkFileExtension) {
				pathname = writer.checkFileExtension(pathname);
			}
			fos = new FileOutputStream(pathname);
			writer.write(cv, fos);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {
			}
		}
		return pathname;
	}

	/**
	 * Writes expression data to a file in the given format. The correct file
	 * extension will be added to the pathname if it is not present. If there is
	 * already a file present at the given pathname, its contents are discarded.
	 * 
	 * @param data
	 *            the expression data.
	 * @param formatName
	 *            a String containing the informal name of a format (e.g., "res"
	 *            or "gct".)
	 * @param pathname
	 *            a pathname string
	 * @param checkFileExtension
	 *            Whether the correct file extension will be added to the
	 *            pathname if it is not present.
	 * @return The pathname that the data was saved to
	 * @exception IOException
	 *                If an error occurs while saving the data
	 */
	public static String write(IExpressionData data, String formatName,
			String pathname, boolean checkFileExtension) throws IOException {
		IExpressionDataWriter writer = (IExpressionDataWriter) formatNameToExpressionWriterMap
				.get(formatName);
		if (writer == null) {
			throw new IOException("No writer to save the data in " + formatName
					+ " format.");
		}
		if (checkFileExtension) {
			pathname = writer.checkFileExtension(pathname);
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(pathname);
			writer.write(data, fos);
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {
			}
		}
		return pathname;
	}

	/**
	 * Parses the odf document at the given pathname using the specified handler
	 * 
	 * @param pathname
	 *            A pathname string
	 * @param handler
	 *            The odf handler
	 * @exception IOException
	 *                If an error occurs while reading from the file
	 * @exception ParseException
	 *                If there is a problem with the data
	 * @return <code>true</code> if the odf document was read successfully
	 */

	public static void readOdf(String pathname, IOdfHandler handler)
			throws ParseException, IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(pathname);
			OdfParser parser = new OdfParser();
			parser.setHandler(handler);
			parser.parse(fis);
			fis.close();
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException x) {
				}
			}
		}
	}

	private static IExpressionDataReader tryAllReaders(Iterator it,
			String pathname) throws IOException {
		while (it.hasNext()) {
			IExpressionDataReader r = (IExpressionDataReader) it.next();
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(pathname);
				try {
					if (r.canRead(fis)) {
						return r;
					}
				} catch (Exception e) {
					if (debug) {
						e.printStackTrace();
					}
				}
			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		}
		return null;
	}

	private static Iterator lookupProviders(Class c, Iterator defaultIt) {
		try {
			Class klass = Class.forName("javax.imageio.spi.ServiceRegistry");
			java.lang.reflect.Method m = klass.getMethod("lookupProviders",
					new Class[] { Class.class });
			return (Iterator) m.invoke(null, new Object[] { c });
			//return javax.imageio.spi.ServiceRegistry.lookupProviders(c);
		} catch (Exception e) {
			return defaultIt;
		}
	}

	/*
	 * public static boolean isFeatureList(String pathname) { String suffix =
	 * pathname.substring(pathname.lastIndexOf(".") + 1,
	 * pathname.length()).toLowerCase();
	 * featureListReader.getFileSuffixes().contains(suffix); return
	 * pathname.endsWith(".grp"); }
	 */
	/**
	 * Gets an expression data reader that can read the document at the given
	 * pathname or <code>null</code> if no reader is found.
	 * 
	 * @param pathname
	 *            A pathname string
	 * @return The expression reader
	 */
	public static IExpressionDataReader getExpressionReader(String pathname) {
		IExpressionDataReader reader = null;
		int dotIndex = pathname.lastIndexOf(".");
		if (dotIndex != -1) {// see if file has an extension
			String suffix = pathname.substring(dotIndex + 1, pathname.length());
			reader = (IExpressionDataReader) suffix2ExpressionReaders
					.get(suffix);
		}
		if (reader == null) {
			try {
				reader = tryAllReaders(suffix2ExpressionReaders.values()
						.iterator(), pathname);
			} catch (IOException ioe) {
				return null;
			}
		}
		return reader;
	}

	/**
	 * Returns a IExpressionDataWriter that can encode the named format
	 * 
	 * @param formatName
	 *            a String containing the informal name of a format (e.g., "res"
	 *            or "gct".
	 * @return a IExpressionDataWriter that can encode in the specified format
	 *         or <codee>null</code> if no such IExpressionDataWriter exists.
	 */
	public static IExpressionDataWriter getExpressionWriterByFormatName(String formatName) {
		return (IExpressionDataWriter) formatNameToExpressionWriterMap
				.get(formatName);
	}

	static {
		List defaultWriters = Arrays.asList(new IExpressionDataWriter[] {
				new ResWriter(), new GctWriter(), new OdfDatasetWriter() });
		try {
			defaultWriters.add(new org.genepattern.io.expr.mage.MAGEMLWriter());// don't
																				// require
																				// magestk
																				// to
																				// be
																				// in
																				// classpath
		} catch (Throwable t) {
		}
		Iterator it = defaultWriters.iterator();//lookupProviders(edu.mit.broad.io.expr.IExpressionDataWriter.class,
												// defaultWriters);
		formatNameToExpressionWriterMap = new HashMap();
		while (it.hasNext()) {
			IExpressionDataWriter writer = (IExpressionDataWriter) it.next();
			formatNameToExpressionWriterMap.put(writer.getFormatName(), writer);
		}
	}

	static {
		Iterator defaultReaders = Arrays.asList(
				new IExpressionDataReader[] { new GctReader(), new ResReader(),
						new OdfDatasetReader() }).iterator();// defaults if
															 // using java 1.3
		Iterator it = defaultReaders;//lookupProviders(edu.mit.broad.io.expr.IExpressionDataReader.class,
									 // defaultReaders);
		suffix2ExpressionReaders = new HashMap();
		while (it.hasNext()) {
			IExpressionDataReader reader = (IExpressionDataReader) it.next();
			List suffixes = reader.getFileSuffixes();
			for (int j = 0, cnt = suffixes.size(); j < cnt; j++) {
				String suffix = (String) suffixes.get(j);
				// XXX if > 1 reader can read a file with the same suffix, one
				// of the readers will be 'lost'
				suffix2ExpressionReaders.put(suffix, reader);
			}
		}
		String temp = System.getProperty("edu.mit.broad.gp.debug");
		if ("true".equals(temp)) {
			debug = true;
		}
	}
}