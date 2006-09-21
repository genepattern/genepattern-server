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
 * Writes cn/loh files.
 * 
 * @author Joshua Gould
 */
public class CnWriter implements IExpressionDataWriter {
    String format;

    public CnWriter(String format) {
        this.format = format;
    }

    public String checkFileExtension(String filename) {
        if (!filename.toLowerCase().endsWith("." + format)) {
            filename += "." + format;
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
        }
        out.println();

        for (int i = 0; i < rows; i++) {
            out.print(data.getRowName(i));
            out.print("\t");

            String chromosome = data.getRowMetadata(i, ExpressionConstants.CHROMOSOME);
            if (chromosome == null) {
                chromosome = "";
            }
            out.print(chromosome);
            out.print("\t");

            String physicalPosition = data.getRowMetadata(i, ExpressionConstants.PHYSICAL_POSITION);
            if (physicalPosition == null) {
                physicalPosition = "";
            }
            out.print(physicalPosition);

            for (int j = 0; j < columns; j++) {
                if (format.equals("cn")) {
                    out.print("\t");
                    out.print(data.getValueAsString(i, j));
                }
                out.print("\t");
                Object lohData = data.getData(i, j, "Data");
                out.print(lohData);
            }
            out.println();
        }
        out.flush();
    }

    public String getFormatName() {
        return format;
    }

}
