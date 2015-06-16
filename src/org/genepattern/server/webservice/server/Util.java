/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.genepattern.webservice.WebServiceException;

/**
 * Utilities methods
 * 
 * @author Joshua Gould
 * 
 */
public class Util {
    private Util() {
    }

    /**
     * Download the contents at the url and save them to a local temp file. Return a handle to the local file.
     * 
     * @param url
     * @return handle to local temp file
     * @throws WebServiceException
     */
    public static File downloadUrl(String url) throws WebServiceException {
        if (new File(url).exists()) {
            return new File(url);
        }
        try {
            URL u = new URL(url);

            if ("file".equalsIgnoreCase(u.getProtocol())) {
                return new File(new URI(u.toExternalForm()));
            }
            OutputStream os = null;
            InputStream is = null;
            try {
                File file = File.createTempFile("tmp", null);
                os = new FileOutputStream(file);
                is = u.openStream();
                byte[] buf = new byte[100000];
                int i;
                while ((i = is.read(buf, 0, buf.length)) > 0) {
                    os.write(buf, 0, i);

                }
                return file;
            } catch (IOException ioe) {
                throw new WebServiceException(ioe);
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException x) {
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException x) {
                    }
                }

            }

        } catch (MalformedURLException e) {
            throw new WebServiceException(e);
        } catch (URISyntaxException e) {
            throw new WebServiceException(e);
        }

    }

    public static void copyFile(File source, File dest) {
        byte[] buf = new byte[100000];
        int j;
        FileOutputStream os = null;
        FileInputStream is = null;
        try {
            os = new FileOutputStream(dest);
            is = new FileInputStream(source);
            while ((j = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, j);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getAxisFile(DataHandler dh) {
        javax.activation.DataSource ds = dh.getDataSource();
        if (ds instanceof FileDataSource) { // if local
            return ((FileDataSource) ds).getFile();
        }
        // if through SOAP org.apache.axis.attachments.ManagedMemoryDataSource
        return new File(dh.getName());
    }
}
