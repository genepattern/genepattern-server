/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.genepattern.util.GPConstants;
import org.junit.Ignore;

/**
 * Utility class for extracting all nested zips from an exported GenePattern pipeline.
 * It recursively traverses each included zip file, so that it can handle nested pipelines.
 * The zip files are saved to a new temporary directory.
 * 
 * @author pcarr
 */
@Ignore
public class ZipWalker {
    public static File createTempDirectory() throws IOException  {
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if(!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if(!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    private File pipelineIncludingModules;
    private List<File> nestedZips;
    private File tmpdir=null;

    private File initTmpDir() throws IOException {
        //lazy init
        if (tmpdir == null) {
            tmpdir = createTempDirectory();
            //TODO: clean this up on exit of the walk() method
            tmpdir.deleteOnExit();
        }
        return tmpdir;
    }

    /**
     * @param file, the zip file created by exporting a pipeline from the GP server.
     */
    public ZipWalker(File zip) {
        this.pipelineIncludingModules = zip;
        this.nestedZips=new ArrayList<File>();
    }

    public List<File> getNestedZips() {
        return Collections.unmodifiableList(nestedZips);
    }

    public void walk() throws Exception {
        this.nestedZips.clear();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(pipelineIncludingModules);
            walkFile(zipFile);
        }
        catch (IOException e) {
            throw new Exception("IOException walking zipFile="+zipFile+": "+e.getLocalizedMessage(), e);
        }
        catch (Exception e) {
            throw new Exception("Exception walking zipFile="+zipFile+": "+e.getLocalizedMessage(), e);
        }
        finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                }
                catch (IOException e) {
                    throw new Exception("Unexpected expection closing zipFile="+zipFile+": "+e.getLocalizedMessage(), e);
                }
            }
        }
    }

    private void walkFile(final ZipFile zipFile) throws Exception {
        if (zipFile.getEntry(GPConstants.MANIFEST_FILENAME) != null) {
            //ignore zipfiles which contain a manifest, assume it's a top level module or pipeline
            return;
        }

        //ignore zipfiles which contain anything other than other zip files
        for (Enumeration<? extends ZipEntry> eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
            ZipEntry zipEntry = eEntries.nextElement();
            if (!zipEntry.getName().toLowerCase().endsWith(".zip")) {
                return;
            }
        }

        //if we are here, assume it's a zip of zips
        for (Enumeration<? extends ZipEntry> eEntries = zipFile.entries(); eEntries.hasMoreElements();) {
            ZipEntry zipEntry = eEntries.nextElement();
            if (zipEntry.getName().toLowerCase().endsWith(".zip")) {
                final File nestedZipFile = extractZip(zipFile, zipEntry); 
                nestedZips.add( nestedZipFile );
                //recursive call
                ZipFile child = new ZipFile(nestedZipFile);
                walkFile(child);
            }
        }
    }

    private File extractZip(ZipFile fromZipFile, ZipEntry zipEntry) throws Exception {
        //it's a nested zip file
        initTmpDir();
        final File nestedZipFile = new File(tmpdir, zipEntry.getName());
        long fileLength = zipEntry.getSize();
        long numRead = 0;
        InputStream is=null;
        OutputStream os=null;
        try {                    
            is = fromZipFile.getInputStream(zipEntry);
            os = new FileOutputStream(nestedZipFile);
            byte[] buf = new byte[100000];
            int i=0;
            while ((i = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, i);
                numRead += i;
            }
            nestedZipFile.setLastModified(zipEntry.getTime());
            if (numRead != fileLength) {
                throw new Exception("only read " + numRead + " of " + fileLength + " bytes in " + fromZipFile.getName() + "'s " + zipEntry.getName());
            }
        }
        finally {
            IOException osException=null;
            IOException isException=null;
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {
                    osException=e;
                }
            }
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    isException=e;
                }
            }
            if (osException != null) throw osException;
            if (isException != null) throw isException;
        }
        return nestedZipFile;
    }

    public void deleteTmpFiles() throws Exception {
        List<File> filesNotDeleted=new ArrayList<File>();
        for(File file : nestedZips) {
            boolean deleted = file.delete();
            if (!deleted) {
                filesNotDeleted.add(file);
            }
        }
        if (tmpdir != null) {
            boolean deleted=tmpdir.delete();
            if (!deleted) {
                filesNotDeleted.add(tmpdir);
            }
        }
        if (filesNotDeleted.size()>0) {
            final String NL = "\n";
            String errorMessage="Failed to delete "+filesNotDeleted.size()+" tmp files.";
            for(File file : filesNotDeleted) {
                errorMessage += NL + "    "+file.getPath();
            }
            throw new Exception(errorMessage);
        }
    }

}
