/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

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
