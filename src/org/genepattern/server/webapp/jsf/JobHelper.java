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

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class JobHelper {
    private static NumberFormat numberFormat;

    static {
        numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
    }

    public static List<File> getOutputFiles(JobInfo jobInfo) {

        List<File> outputFiles = new ArrayList<File>();
        ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
        if (parameterInfoArray != null) {
            String dir = System.getProperty("jobs", "./temp") + "/" + jobInfo.getJobNumber();
            for (int i = 0; i < parameterInfoArray.length; i++) {
                if (parameterInfoArray[i].isOutputFile()) {
                    String fn = parameterInfoArray[i].getName();
                    outputFiles.add(new File(dir, fn));
                    // get modules for output file
                }
            }
        }

        return outputFiles;
    }

    public static String getFormattedSize(long size) {
        if (size >= 1073741824) {
            double gigabytes = size / 1073741824.0;
            return numberFormat.format(gigabytes) + " GB";
        } else if (size >= 1048576) {
            double megabytes = size / 1048576.0;
            return numberFormat.format(megabytes) + " MB";
        } else {
            return Math.max(0, Math.ceil(size / 1024.0)) + " KB";
        }
    }

}
