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



package edu.mit.genome.gp.util;

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.MultipartPostMethod;



public class ZipCatalogUpload {



    public static void main(String[] args) throws Exception {

	// get url (base only) and file or dir to upload
	//
	if (args.length != 4) {
	    System.out.println ("Usage: java ZipCatalogUpload url [module|patch] [prod|dev] fileOrDir\n\n");
	    System.out.println ("To load all zips in a directory");
	    System.out.println ("e.g. java ZipCatalogUpload http://iwww.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_publish_module.cgi module dev ./gp2/modules/build  ");
	    System.out.println ("To load just one zip");
	    System.out.println ("e.g. java ZipCatalogUpload http://iwww.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_publish_module.cgi dev ./gp2/modules/build/TransposeDataset.zip");
	    System.exit(999);
	}


	String targetURL = args[0];
	//MultipartPostMethod filePost =  new MultipartPostMethod(targetURL);

	String type = args[1];
	String environment = args[2];

	String fileOrDirName = args[3];
	File aFile = new File(fileOrDirName);
	if (!aFile.exists()) {
	    System.out.println("File does not exist:" + aFile.getAbsolutePath());
	    System.exit(998);
	}
	System.out.println("Uploading zip files to: " + targetURL);
	
	if (aFile.isDirectory()){
	    System.out.println("Files coming from dir:" + aFile.getAbsolutePath());
	    // FilenameFilter filt =  

	    File[] children = aFile.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name){
			return name.toLowerCase().endsWith(".zip");
		    }
		}
            );
	    for (int i=0; i < children.length; i++){
		MultipartPostMethod filePost =  new MultipartPostMethod(targetURL);
		File targetFile = children[i];
		uploadFile(filePost, type, environment, targetFile);

	    }

	} else {
	    MultipartPostMethod filePost =  new MultipartPostMethod(targetURL);
	    File targetFile = aFile;
	    uploadFile(filePost, type, environment, targetFile);
	}
    } 

    public static void uploadFile(MultipartPostMethod filePost, String type, String environment, File targetFile) throws Exception {
	boolean DEBUG = Boolean.getBoolean("DEBUG");
	filePost.addParameter("repos", type);
	filePost.addParameter("location", environment);
	filePost.addParameter("zipfilename", targetFile);
	HttpClient client = new HttpClient();
	
	client.setConnectionTimeout(5000);
	    
	//System.out.print("Starting upload of: " + targetFile.getAbsolutePath() + " to " + environment + " " + filePost.getHostConfiguration().getHostURL() + "" + filePost.getPath());
	System.out.print("uploading: " + targetFile.getName() + " to " + filePost.getHostConfiguration().getHostURL());
	int status = client.executeMethod(filePost);
	
	if (status == HttpStatus.SC_OK) {
	    System.out.println("\t Upload complete " + status);
	    // to see the whole response uncomment the following line
	   if (DEBUG) System.out.println("Upload complete, response=" + filePost.getResponseBodyAsString() );
	} else {
	    System.out.println("\t Upload failed, response=" + HttpStatus.getStatusText(status) );
	}
	filePost.releaseConnection();
    }

}
