/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.util;

import org.genepattern.matrix.DefaultDataset;

public class MatlabUtil {

    public MatlabUtil() {
    }

    public DefaultDataset asDataset(int rows, int columns, String[] rowNames, String[] rowDescriptions,
            String[] columnNames, String[] colDescriptions, double[][] data) {
        return new DefaultDataset(data, rowNames, columnNames, rowDescriptions, colDescriptions);
    }

}
