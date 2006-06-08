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

package org.genepattern.io.expr.snp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.genepattern.data.expr.ExpressionConstants;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.io.expr.IExpressionDataWriter;

/**
 * Writes snp files.
 * 
 * @author Joshua Gould
 */
public class SnpWriter implements IExpressionDataWriter {
    final static String FORMAT_NAME = "snp";

    public String checkFileExtension(String filename) {
        if (!filename.toLowerCase().endsWith(".snp")) {
            filename += ".snp";
        }
        return filename;
    }

    public void write(IExpressionData data, OutputStream os) throws IOException {
        PrintWriter out = new PrintWriter(os);
        int rows = data.getRowCount();
        int columns = data.getColumnCount();

        out.print("SNP\tChromosome\tPhysicalPosition");
        for (int j = 0; j < columns; j++) {
            out.print("\t");
            out.print(data.getColumnName(j));
            out.print("\t");
            out.print(data.getColumnName(j) + " Call");
        }
        out.println();

        for (int i = 0; i < rows; i++) {
            out.print(data.getRowName(i));
            out.print("\t");

            String chromosome = data.getRowMetadata(i,
                    ExpressionConstants.CHROMOSOME);
            if (chromosome == null) {
                chromosome = "";
            }
            out.print(chromosome);
            out.print("\t");

            String physicalPosition = data.getRowMetadata(i,
                    ExpressionConstants.PHYSICAL_POSITION);
            if (physicalPosition == null) {
                physicalPosition = "";
            }
            out.print(physicalPosition);

            for (int j = 0; j < columns; j++) {
                out.print("\t");
                out.print(data.getValueAsString(i, j));
                out.print("\t");
                Object call = data.getData(i, j, ExpressionConstants.CALLS);
                out.print(call != null ? call : "");
            }
            out.println();
        }
        out.flush();
    }

    public String getFormatName() {
        return FORMAT_NAME;
    }

}
