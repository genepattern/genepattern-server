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

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.MultipartPostMethod;

public class ZipUpload {

    public static void main(String[] args) throws Exception {

        // get url (base only) and file or dir to upload
        //
        if (args.length != 2) {
            System.out.println("Usage: java ZipUpload url fileOrDir");
            System.out.println("To load all zips in a directory");
            System.out
                    .println("e.g. java ZipUpload http://elm:8080/gp ./gp2/modules/build  ");
            System.out.println("To load just one zip");
            System.out
                    .println("e.g. java ZipUpload http://elm:8080/gp ./gp2/modules/build/TransposeDataset.zip");
            System.exit(999);
        }

        String targetURL = args[0] + "/installZip.jsp";

        String fileOrDirName = args[1];
        File aFile = new File(fileOrDirName);
        if (!aFile.exists()) {
            System.out
                    .println("File does not exist:" + aFile.getAbsolutePath());
            System.exit(998);
        }

        if (aFile.isDirectory()) {
            System.out.println("Files coming from:" + aFile.getAbsolutePath());

            File[] children = aFile.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".zip");
                }
            });
            for (int i = 0; i < children.length; i++) {
                MultipartPostMethod filePost = new MultipartPostMethod(
                        targetURL);
                File targetFile = children[i];
                uploadFile(filePost, targetFile);
            }

        } else {
            MultipartPostMethod filePost = new MultipartPostMethod(targetURL);
            File targetFile = aFile;
            uploadFile(filePost, targetFile);
        }
    }

    public static void uploadFile(MultipartPostMethod filePost, File targetFile)
            throws Exception {
        filePost.addParameter("file1", targetFile);
        HttpClient client = new HttpClient();
        HttpState state = client.getState();

        Cookie cook = new Cookie(filePost.getHostConfiguration().getHost(),
                "userid", "genepattern", "/gp", Integer.MAX_VALUE, false);
        state.addCookie(cook);
        client.setState(state);

        client.setConnectionTimeout(5000);

        System.out.println("Uploading " + targetFile.getName() + " to "
                + filePost.getHostConfiguration().getHostURL());
        int status = client.executeMethod(filePost);

        if (status != HttpStatus.SC_OK) {
            System.out.println("\tUpload failed, response="
                    + HttpStatus.getStatusText(status));
        }
        filePost.releaseConnection();
    }
}
