/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.text.NumberFormat;

public class JobHelper {
    private static NumberFormat numberFormat;
    static {
        numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
    }

    public static String getFormattedSize(long size) {
        if (size >= 1073741824) {
            double gigabytes = size / 1073741824.0;
            return numberFormat.format(gigabytes) + " GB";
        } 
        else if (size >= 1048576) {
            double megabytes = size / 1048576.0;
            return numberFormat.format(megabytes) + " MB";
        } 
        else {
            return Math.max(0, Math.ceil(size / 1024.0)) + " KB";
        }
    }
}
