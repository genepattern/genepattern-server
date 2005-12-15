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


package org.genepattern.io.expr.cls;

import java.io.PrintWriter;

import org.genepattern.data.matrix.ClassVector;

/**
 * <P>
 * 
 * Writer for cls files. CLS files are simple files created to load class
 * information into GeneCluster. These files use spaces to separate the fields.
 * </P>
 * <UL>
 * <LI>The first line of a CLS file contains numbers indicating the number of
 * samples and number of classes. The number of samples should correspond to the
 * number of samples in the associated RES or GCT data file.</LI>
 * 
 * <UL>
 * <LI>Line format: (number of samples) (space) (number of classes) (space) 1
 * </LI>
 * <LI>For example: 58 2 1</LI>
 * </UL>
 * 
 * <LI>The second line in a CLS file contains names for the class numbers. The
 * line should begin with a pound sign (#) followed by a space.</LI>
 * 
 * <UL>
 * <LI>Line format: # (space) (class 0 name) (space) (class 1 name)</LI>
 * 
 * <LI>For example: # cured fatal/ref.</LI>
 * </UL>
 * 
 * <LI>The third line contains numeric class labels for each of the samples.
 * The number of class labels should be the same as the number of samples
 * specified in the first line.</LI>
 * <UL>
 * <LI>Line format: (sample 1 class) (space) (sample 2 class) (space) ...
 * (sample N class)</LI>
 * <LI>For example: 0 0 0 ... 1
 * </UL>
 * 
 * </UL>
 * 
 * 
 * @author Joshua Gould
 */
public class ClsWriter {

	public String checkFileExtension(String pathname) {
		if (!pathname.toLowerCase().endsWith(".cls")) {
			pathname += ".cls";
		}
		return pathname;
	}

	/**
	 * Writes an <CODE>ClassVector</CODE> instance to a file. The correct file
	 * extension will be added to the file name if it does not already exist.
	 * 
	 * @param cv
	 *            the class vector
	 * @param os
	 *            the output stream
	 * @exception java.io.IOException
	 *                if an I/O error occurs during writing
	 */
	public void write(ClassVector cv, java.io.OutputStream os)
			throws java.io.IOException {
		PrintWriter pw = new PrintWriter(os);
		pw.print(cv.size());
		pw.print(" ");
		pw.print(cv.getClassCount());
		pw.print(" ");
		pw.println("1");

		pw.print("#");
		for (int i = 0, levels = cv.getClassCount(); i < levels; i++) {
			pw.print(" ");
			pw.print(cv.getClassName(i));
		}
		pw.println();

		for (int i = 0, size = cv.size() - 1; i < size; i++) {
			pw.print(cv.getAssignment(i));
			pw.print(" ");
		}
		pw.println(cv.getAssignment(cv.size() - 1));
		pw.flush();
		pw.close();
	}

}