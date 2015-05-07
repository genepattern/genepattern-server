/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.server.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.SuiteInfo;

public class ZipSuite extends CommandLineAction {
    final static private Logger log = Logger.getLogger(ZipSuite.class);
    public static final String suiteManifestFileName = "suiteManifest.xml";

    /**
     * @param zos
     * @param dir
     * @throws Exception
     */
    protected void zipFiles(final ZipOutputStream zos, final File dir) throws Exception {
        if (dir==null) {
            log.debug("dir==null");
            return;
        }
        if (!dir.canRead()) {
            log.debug("Can't read directory: "+dir);
            return;
        }
        File[] fileList = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.endsWith(".old") && !name.endsWith(".bak");
            }
        });
        for (int i = 0; i < fileList.length; i++) {
            zipFile(zos, fileList[i]);
        }
    }

    /**
     * @param zos
     * @param f
     * @throws Exception
     */
    protected void zipFile(ZipOutputStream zos, File f) throws Exception {
        zipFile(zos, f, null);
    }

    /**
     * @param zos
     * @param f
     * @param comment
     * @throws Exception
     */
    private void zipFile(ZipOutputStream zos, File f, String comment) throws Exception {
        try {
            if (f.isDirectory()) {
                return;
            }
            ZipEntry zipEntry = null;
            FileInputStream is = null;

            byte[] buf = new byte[100000];
            zipEntry = new ZipEntry(f.getName());
            zipEntry.setTime(f.lastModified());
            zipEntry.setSize(f.length());
            if (comment != null) {
                zipEntry.setComment(comment);
            }
            zos.putNextEntry(zipEntry);
            long fileLength = f.length();
            long numRead = 0;
            is = new FileInputStream(f);
            int n;
            while ((n = is.read(buf, 0, buf.length)) > 0) {
                zos.write(buf, 0, n);
                numRead += n;
            }
            is.close();
            if (numRead != fileLength) {
                throw new Exception("only read " + numRead + " of "
                        + fileLength + " bytes in " + f.getPath());
            }
            zos.closeEntry();
        }
        catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    private ZipEntry zipSuiteManifest(ZipOutputStream zos, SuiteInfo suite) throws Exception {
        // insert manifest
        ZipEntry zipEntry = new ZipEntry(GPConstants.SUITE_MANIFEST_FILENAME);
        zos.putNextEntry(zipEntry);

        OutputStreamWriter osw = new OutputStreamWriter(zos);
        BufferedWriter bout = new BufferedWriter(osw);

        SuiteInfoManifestXMLGenerator.generateXMLFile(suite, bout);

        zos.closeEntry();
        return zipEntry;
    }

    /**
     * @param name
     * @param userID
     * @return
     * @throws Exception
     */
    public File packageSuite(String name, String userID) throws Exception {
        if (name == null || name.length() == 0) {
            throw new Exception(
                    "Must specify task name as name argument to this page");
        }

        SuiteInfo suite = null;
        IAdminClient adminClient = new LocalAdminClient(userID);
        SuiteInfoManifestXMLGenerator.userId = userID;

        suite = adminClient.getSuite(name);
        return packageSuite(suite, userID);
    }

    /**
     * @param suiteInfo
     * @param userID
     * @return
     * @throws Exception
     */
    public File packageSuite(SuiteInfo suiteInfo, String userID) throws Exception {
        String name = suiteInfo.getName();

        // use an LSID-unique name so that different versions of same named task
        // don't collide within zip file
        String suffix = "_"
                + Integer.toString(Math.abs(suiteInfo.getLSID()
                        .hashCode()), 36); // [a-z,0-9]

        // create zip file
        File zipFile = File.createTempFile(name + suffix, ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

        // add the manifest
        zipSuiteManifest(zos, suiteInfo);

        // insert attachments
        // find $OMNIGENE_ANALYSIS_ENGINE/taskLib/<taskName> to locate DLLs,
        // other support files

        File suiteLibDir = DirectoryManager.getSuiteLibDir(name, suiteInfo.getLSID(), userID);
        zipFiles(zos, suiteLibDir);
        zos.finish();
        zos.close();

        return zipFile;
    }

}
