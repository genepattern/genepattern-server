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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;


public class ZipCatalogUpload {
    public static final String DEV = "dev";
    public static final String PROD = "prod";
    public static final String MODULE = "module";
    public static final String PATCH = "patch";
    public static final String BROAD_URL =
            "http://www.broad.mit.edu/webservices/genepatternmodulerepository/ModuleRepositoryServlet";

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java ZipCatalogUpload url [module|patch] [prod|dev] fileOrDir\n\n");
            System.out.println("To load all zips in a directory:");
            System.out.println(
                    "e.g. java ZipCatalogUpload " + BROAD_URL + " " + MODULE + " " + DEV + " ./gp2/modules/build  ");
            System.out.println("To load just one zip:");
            System.out.println("e.g. java ZipCatalogUpload " + BROAD_URL + " " + MODULE + " " + DEV +
                    " ./gp2/modules/build/TransposeDataset.zip");
            System.exit(1);
        }
        String targetURL = args[0];
        String modulePatchOrSuite = args[1];
        String devOrProd = args[2];
        String fileOrDirName = args[3];
        upload(targetURL, modulePatchOrSuite, devOrProd, fileOrDirName);
    }


    /**
     * @param targetURL          The url  http://iwww.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_publish_module.cgi
     * @param modulePatchOrSuite ZipCatalogUpload.MODULE or ZipCatalogUpload.PATCH
     * @param devOrProd          ZipCatalogUpload.DEV or ZipCatalogUpload.PROD
     * @param fileOrDirName      The file name or directory to upload
     * @throws IOException If an error occurs
     */
    public static void upload(String targetURL, String modulePatchOrSuite, String devOrProd, String fileOrDirName)
            throws IOException {
        File aFile = new File(fileOrDirName);
        if (!aFile.exists()) {
            System.out.println("File does not exist:" + aFile.getAbsolutePath());
            System.exit(1);
        }
        if (aFile.isDirectory()) {
            File[] children = aFile.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".zip");
                }
            });
            for (int i = 0; i < children.length; i++) {
                MultipartPostMethod filePost = new MultipartPostMethod(targetURL);
                File targetFile = children[i];
                uploadFile(filePost, modulePatchOrSuite, devOrProd, targetFile);
            }
        } else {
            MultipartPostMethod filePost = new MultipartPostMethod(targetURL);
            uploadFile(filePost, modulePatchOrSuite, devOrProd, aFile);
        }
    }

    /**
     * @param targetURL The url  http://iwww.broad.mit.edu/cgi-bin/cancer/software/genepattern/gp_publish_module.cgi
     * @throws IOException If an error occurs
     */

    public static void regenerateCatalog(String targetURL) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(targetURL + "?cmd=REGENERATE_CATALOG");
        try {
            int status = client.executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                System.out.println("Successfully regenerated catalog");
            } else {
                System.out.println("Error regenerating catalog");
            }
        } finally {
            get.releaseConnection();
        }
    }

    private static void uploadFile(MultipartPostMethod filePost, String modulePatchOrSuite, String devOrProd,
                                   File targetFile) throws IOException {
        filePost.addParameter("repos", modulePatchOrSuite);
        filePost.addParameter("env", devOrProd);
        filePost.addParameter("zipfilename", targetFile);
        filePost.addParameter("cmd", "upload");
        HttpClient client = new HttpClient();
        client.setConnectionTimeout(Integer.MAX_VALUE);
        int status = client.executeMethod(filePost);
        if (status == HttpStatus.SC_OK) {
            System.out.println("Successfully uploaded " + targetFile.getName());
        } else {
            System.out.println("An error occurred while uploading " + targetFile.getName());
            System.out.println("Response: " + HttpStatus.getStatusText(status));
        }
        filePost.releaseConnection();
    }
}
