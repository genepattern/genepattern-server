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

package org.genepattern.util;

import java.io.File;
import java.io.FilenameFilter;

import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

public class ZipUpload {
    private TaskIntegratorProxy taskIntegratorProxy;

    public ZipUpload(String server) throws WebServiceException {
	taskIntegratorProxy = new TaskIntegratorProxy(server, "GenePattern", "");
    }

    public static void main(String[] args) throws Exception {
	args = new String[] { "http://192.168.2.2:8080",
		"/Users/jgould/Documents/workspace/modules/build/PredictionResultsViewer.zip" };
	// get url and file or directory to upload
	if (args.length != 2) {
	    System.out.println("Usage: java ZipUpload url fileOrDir");
	    System.out.println("To load all zip files in a directory");
	    System.out.println("e.g. java ZipUpload http://hassium:8080 ./gp2/modules/build  ");
	    System.out.println("To load just one zip file");
	    System.out.println("e.g. java ZipUpload http://hassium:8080 ./gp2/modules/build/TransposeDataset.zip");
	    System.exit(1);
	}

	ZipUpload zipUpload = new ZipUpload(args[0]);
	String fileOrDirName = args[1];

	File aFile = new File(fileOrDirName);
	if (!aFile.exists()) {
	    System.out.println("File not found: " + aFile.getPath());
	    System.exit(1);
	}

	if (aFile.isDirectory()) {
	    File[] children = aFile.listFiles(new FilenameFilter() {
		public boolean accept(File dir, String name) {
		    return name.toLowerCase().endsWith(".zip");
		}
	    });
	    for (int i = 0; i < children.length; i++) {
		zipUpload.uploadFile(children[i]);
	    }

	} else {
	    zipUpload.uploadFile(aFile);
	}
    }

    public void uploadFile(File zipFile) throws Exception {
	taskIntegratorProxy.importZip(zipFile, GPConstants.ACCESS_PUBLIC);
    }
}
