/*
 * This software and its documentation are copyright 1999 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */

package org.genepattern.io.encoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.genepattern.data.DataObjector;
import org.genepattern.data.Dataset;

/**
 * Class to support writing Whitehead's .gct files
 */

public class GctDatasetEncoder implements Encoder {

	// NOTE: see CSomAlg::SaveAll to optionally write class assignments and
	// distances to file
	// To read this into splus (note row.names is still 1 even though the data
	// begins on line 3.
	//    The reason is the skip=2.
	//
	//    gcm <- read.table("d:\\mprsets\\gcm.gct", header=T, row.names=1, skip=2,
	// as.is=T, sep="\t")
	//    may also have to do options(object.size=10000000)
	public void write(final DataObjector data, final OutputStream out)
			throws IOException {
		write(data, new OutputStreamWriter(out));
	}

	public void write(final DataObjector data, final Writer writer)
			throws IOException {
		//        long fBytesWritten = 0;
		//        long fTotalBytes;
		//        int fNumRows;
		//        int fCurRow = 0;
		//        File fFile;
		final Dataset dataset = (Dataset) data;
		;
		final StringBuffer buffer = new StringBuffer(4096);
		try {
			BufferedWriter outFile = new BufferedWriter(writer);
			final int numRows = dataset.getRowCount();
			final int numCols = dataset.getColumnCount();
			//	    fBytesWritten = 0;

			String outStr = org.genepattern.io.parsers.GctParser.VERSION_2_TOKEN;
			outFile.write(outStr);
			outFile.newLine();
			//	    fBytesWritten += outStr.length() + 1;

			outStr = getDimensionLine(buffer, numRows, numCols);
			outFile.write(outStr);
			outFile.newLine();
			//	    fBytesWritten += outStr.length() + 1;

			outStr = getColLine(dataset, buffer);
			outFile.write(outStr);
			outFile.newLine();
			//	    fBytesWritten += outStr.length() + 1;

			//	    fCurRow = 3;
			//	    notifyListeners(IAlgListener.kAlgIterated);

			for (int i = 0; i < numRows; ++i) {
				outStr = getDataLine(i, buffer, dataset);
				outFile.write(outStr);
				outFile.newLine();
				//		fBytesWritten += outStr.length() + 1;
				//		fCurRow++;
			}
			outFile.close();
		} catch (IOException e) {
			e = new IOException("Error: while writing dataset "
					+ dataset.getName() + ":\n" + e.getMessage());
			e.fillInStackTrace();
			throw e;
		}

	}

	public String getFileExtension(DataObjector data) {
		return FILE_EXTENSION;
	}

	public boolean canEncode(DataObjector data) {
		return (data instanceof Dataset);
	}

	/**
	 * all Encoder implementations should be singleton classes without state
	 * 
	 * @return Encoder, the singleton
	 */
	public static final Encoder instance() {
		return INSTANCE;
	}

	// helpers
	/**
	 * creates the dimension line
	 *  
	 */
	private String getDimensionLine(final StringBuffer buffer, final int aRows,
			final int aCols) {
		buffer.setLength(0);
		buffer.append(aRows);
		buffer.append(DELIMITER);
		buffer.append(aCols);
		return buffer.toString();
	}

	/** creates the column definition line */
	private String getColLine(final Dataset dataset, final StringBuffer buffer) {
		final char delim = DELIMITER;
		buffer.setLength(0);

		// Put in columns for the name and description to allow
		//  better importing into other programs (like excel)
		//buffer.append("# ");
		buffer.append("Name");
		buffer.append(delim);
		buffer.append("Description");
		final int col_cnt = dataset.getColumnCount();
		for (int i = 0; i < col_cnt; ++i) {
			buffer.append(delim);
			buffer.append(dataset.getColumnName(i));
		}
		return buffer.toString();
	}

	/** creates a data line */
	private String getDataLine(final int r, final StringBuffer buffer,
			final Dataset dataset) {
		final char delim = DELIMITER;
		buffer.setLength(0);
		buffer.append(dataset.getRowName(r));
		buffer.append(delim);
		buffer.append(dataset.getRowPanel().getAnnotation(r));

		final int numCols = dataset.getColumnCount();
		//        float[] theRow = dataset.getMatrix().getRow(r);
		for (int c = 0; c < numCols; c++) {
			buffer.append(delim);
			buffer.append(dataset.getElement(r, c));
		}
		return buffer.toString();
	}

	// fields
	/** the default file extension when writing this to file */
	public static final String FILE_EXTENSION = ".gct";

	/** the singleton instance of this */
	public static final GctDatasetEncoder INSTANCE = new GctDatasetEncoder();

	/**
	 * the delimiter for tsv FIXME this should be common for both the encoder
	 * and decoder
	 */
	public static final char DELIMITER = '\t';

}