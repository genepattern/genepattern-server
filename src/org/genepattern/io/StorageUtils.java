/*
 * StorageUtils.java
 *
 * Created on February 13, 2003, 3:00 PM
 */

package org.genepattern.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import org.genepattern.io.parsers.AbstractDataParser;

//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.Reader;
//import java.io.Writer;

/**
 * Class has static methods for assisting in creating temp data storage
 * 
 * @author kohm
 */
public class StorageUtils {

	/** Creates a new instance of StorageUtils */
	private StorageUtils() {
	}

	/**
	 * creates a new temp file with the given suffix this file is deleted upon
	 * normal JVM exit
	 * 
	 * @param suffix
	 *            The name of the file
	 * @throws IOException
	 *             if a problem arises durring an I/O operation
	 * @return File, the new temp file
	 */
	public static final File createTempFile(final String suffix)
			throws IOException {
		File tmp_file = null;
		//final String prefix = "gp";
		while (tmp_file == null) {
			try {
				final int integer = (int) System.currentTimeMillis();
				//                tmp_file = File.createTempFile(prefix + integer, suffix);
				//tmp_file = File.createTempFile(integer+TEMP_FILE_MARKER+"00",
				// suffix);
				tmp_file = File.createTempFile(integer + TEMP_FILE_MARKER,
						suffix);
			} catch (IOException ex) {
				System.err.println("While trying to create a temp file " + ex);
				throw ex;
			}
		}
		tmp_file.deleteOnExit();
		return tmp_file;
	}

	/**
	 * Creates a new file with the given name in a temp folder. If this file
	 * already exists it's name is appended _1 or _2 etc. This file is deleted
	 * upon normal JVM exit
	 */
	public static final File createTempFileNoMung(final String name)
			throws IOException {
		File tmp_file = new File(TEMP_DIR, name);
		if (tmp_file.exists()) {
			final String ext = getFileExtension(name);
			final String just_name = getFileNameNoExt(name);
			for (int i = 0; tmp_file.exists(); i++) {
				tmp_file = new File(TEMP_DIR, just_name + '_' + i + ext);
			}
		}
		tmp_file.deleteOnExit();
		return tmp_file;
	}

	/** creates a String from the contents of the file */
	public static final String createStringFromContents(final File file)
			throws IOException {
		final StringBuffer buf = new StringBuffer((int) file.length());
		final BufferedReader reader = new BufferedReader(new FileReader(file));
		for (String line = reader.readLine(); line != null; line = reader
				.readLine()) {
			buf.append(line);
			buf.append('\n');
		}
		reader.close();
		return buf.toString();
	}

	/** creates a String from a Reader */
	public static final String createStringFromReader(final Reader rdr)
			throws IOException {
		final StringBuffer buf = new StringBuffer();
		final BufferedReader reader = new BufferedReader(rdr);
		for (String line = reader.readLine(); line != null; line = reader
				.readLine()) {
			buf.append(line);
			buf.append('\n');
		}
		reader.close();
		return buf.toString();
	}

	/**
	 * gets the file extension of the file name
	 * 
	 * @return String the file extension including the file extension separator
	 *         character
	 */
	public static final String getFileExtension(final String name) {
		final int index = name
				.lastIndexOf(AbstractDataParser.FILE_EXT_SEPARATOR);
		if (index < 0)
			return "";
		return name.substring(index);
	}

	/** utility method that get the name of the file with out its' file extension */
	public static final String getFileNameNoExt(final java.io.File file) {
		final String base = file.getName();
		return getFileNameNoExt(base);
	}

	/** utility method that get the name of the file with out its' file extension */
	public static final String getFileNameNoExt(final String base) {
		final int index = base
				.lastIndexOf(AbstractDataParser.FILE_EXT_SEPARATOR);
		if (index < 0)
			return base;
		return base.substring(0, index);
	}

	/**
	 * writes the InputStream to the file
	 * 
	 * @param file
	 *            the file to write to
	 * @param in
	 *            the input stream to read from
	 * @throws IOException
	 *             if a problem arises durring an I/O operation
	 */
	public static final void writeToFile(final File file, final InputStream in)
			throws IOException {
		java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
		byte[] b = new byte[1024];
		int bytesRead;
		while ((bytesRead = in.read(b)) != -1) {
			fos.write(b, 0, bytesRead);
		}
		fos.close();
		in.close();
	}

	/**
	 * writes the InputStream to the file
	 * 
	 * @param file
	 *            he file to write to
	 * @param reader
	 *            the reader to read from
	 * @throws IOException
	 *             if a problem arises durring an I/O operation
	 */
	/*
	 * public static final void writeToFile(final File file, final Reader
	 * reader) throws IOException{ final PrintWriter out = new PrintWriter(new
	 * BufferedWriter(new FileWriter(file))); final BufferedReader in = (reader
	 * instanceof BufferedReader) ? (BufferedReader)reader : new
	 * BufferedReader(reader); for(String line = in.readLine(); line != null;
	 * line = in.readLine()) { out.println(line); } out.close(); in.close(); }
	 */
	/**
	 * writes the contents of a reader to the writer using buffered data streams
	 * and closes the reader and writer when done Note that this method uses
	 * Read and Writer which are Character centric IO classes. Use
	 * copyInputToOutputStream() for binary transactions
	 * 
	 * @param reader
	 *            he reader to read from
	 * @param writer
	 *            the write to write to
	 * @throws IOException
	 *             if a problem arises durring an I/O operation
	 */
	public static final void copyReaderToWriter(final Reader reader,
			final Writer writer) throws IOException {
		final int size = 8096;
		final char[] buff = new char[size];
		int i = 0, count = 0;
		final BufferedReader input = (reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader, size));
		final BufferedWriter output = (writer instanceof BufferedWriter ? (BufferedWriter) writer
				: new BufferedWriter(writer, size));
		while ((i = input.read(buff)) >= 0) { // if i == -1 (EOF) then done
			output.write(buff, 0, i);
			//System.out.print(++count+". ");
		}
		//System.out.println();
		// does order matter here? should the input be close first?
		output.flush();
		output.close();
		input.close();
	}

	/**
	 * writes the contents of an InputStream to the OutputStream using buffered
	 * data streams and closes the input and output when done
	 * 
	 * @param input
	 *            he input to read from
	 * @param output
	 *            the output stream to read from
	 * @throws IOException
	 *             if a problem arises durring an I/O operation
	 */
	public static final void copyInputToOutputStream(final InputStream input,
			final OutputStream output) throws IOException {
		final int size = 8096;
		final byte[] buff = new byte[size];
		int i = 0, count = 0;
		final BufferedInputStream in = (input instanceof BufferedInputStream ? (BufferedInputStream) input
				: new BufferedInputStream(input, size));
		final BufferedOutputStream out = (output instanceof BufferedOutputStream ? (BufferedOutputStream) output
				: new BufferedOutputStream(output, size));
		while ((i = in.read(buff, 0, size)) >= 0) { // if i == -1 (EOF) then
													// done
			out.write(buff, 0, i);
			//System.out.print(++count+". ");
		}
		//System.out.println();
		// does order matter here? should the input be close first?
		out.flush();
		out.close();
		in.close();
	}

	// Soome work with sophisticated FileChooser behaviour
	//    /** The file chooser */
	//    private JFileChooser chooser;
	//    /** the last dir or null if none opened yet */
	//    private File last_dir;
	//    /** the name of the current survice */
	//    private String service_name;
	//    /** maps the service name to a map of param_name to dir values */
	//    private Map service_map = new HashMap();
	//    protected File getFileChooserFile(final String param_name) {
	//        final JFileChooser chooser = getFileChooser(param_name);
	//        final int state = chooser.showOpenDialog(null);
	//        final File selected_file = chooser.getSelectedFile();
	//        
	//        if( state == JFileChooser.APPROVE_OPTION && selected_file != null ) {
	//            last_dir = selected_file;
	//            return selected_file;
	//        }
	//        return null;
	//    }
	//    protected JFileChooser getFileChooser(final String param_name) {
	//        if( chooser == null ) {
	//            chooser = new JFileChooser();
	//            // don't set font should be from the window it was spauned from
	//        }
	//        setChooserDirFor(param_name);
	//        return chooser;
	//    }
	//    /** sets the file chooser's current directory */
	//    private void setChooserDirFor(final String param_name) {
	//        Map pname_dir = (Map)service_map.get(service_name);
	//        if( pname_dir == null )
	//            pname_dir = new HashMap();
	//        
	//        File dir = (File)pname_dir.get(param_name);
	//        if( dir == null )
	//            dir = last_dir;
	//        if( dir == null )
	//            dir = new File(System.getProperty("user.home"));
	//        chooser.setCurrentDirectory(dir);
	//        pname_dir.put(param_name, dir);
	//        
	//    }
	// fields
	/** the indicator of where the tmp file name ends */
	public static final String TEMP_FILE_MARKER = "__gpgp";

	/** The standard temp directory. Note perhaps this should be "user.home" */
	public static final File TEMP_DIR = new File(System
			.getProperty("java.io.tmpdir"));
}