/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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
