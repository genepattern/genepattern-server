package org.genepattern.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;

/**
 * Write an upload directory to a zip stream for download
 */
public class UploadDirectoryZipWriter {
    private static Logger log = Logger.getLogger(UploadDirectoryZipWriter.class);

    final int BUFSIZE = 10000;

    private GpFilePath uploadDirectory = null;
    byte[] b = new byte[BUFSIZE];

    public UploadDirectoryZipWriter(GpFilePath uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
    }

    public void writeFilesToZip(ZipOutputStream zos) throws IOException {
        writeFilesToZip(zos, uploadDirectory.getServerFile(), uploadDirectory.getName() + "/");
        zos.close();
    }

    private void writeFilesToZip(ZipOutputStream zos, File directory, String prependPath) throws IOException {
        for(File uploadFile : directory.listFiles()) {
            if (uploadFile.isDirectory()) {
                writeFilesToZip(zos, uploadFile, prependPath + uploadFile.getName() + "/");
            }
            else {
                addFileToZip(zos, uploadFile, prependPath);
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File serverFile, String prependPath) {
        if (serverFile == null || !serverFile.canRead()) {
            log.error("File not added to zip entry: uploadFile=" + serverFile.getPath());
            return;
        }

        FileInputStream is = null;
        try {
            is = new FileInputStream(serverFile);
            String entryName = prependPath + serverFile.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipEntry.setTime(serverFile.lastModified());
            zipEntry.setSize(serverFile.length());
            zos.putNextEntry(zipEntry);
            int bytesRead;
            while ((bytesRead = is.read(b, 0, b.length)) != -1) {
                zos.write(b, 0, bytesRead);
            }
            zos.closeEntry();
        }
        catch (FileNotFoundException e) {
            log.error("FileNotFoundException thrown for file: " + serverFile.getPath(), e);
            return;
        }
        catch (IOException e) {
            log.error("Error in addFileToZip: " + e.getLocalizedMessage(), e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Error closing input stream in addFileToZip: " + e.getLocalizedMessage());
                }
            }
        }
    }
}
