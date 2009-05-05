package org.genepattern.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoWrapper.OutputFile;

/**
 * Helper class for writing all output files for a job to a zip file.
 * 
 * @author pcarr
 */
public class JobInfoZipFileWriter {
    private static Logger log = Logger.getLogger(JobInfoZipFileWriter.class);

    private File zipFile = null;
    private JobInfoWrapper jobInfo = null;
    byte[] b = new byte[10000];

    public JobInfoZipFileWriter(JobInfoWrapper jobInfo) {
        this.jobInfo = jobInfo;
    }

    public JobInfoZipFileWriter(File zipFile, JobInfoWrapper jobInfo) {
        this.zipFile = zipFile;
        this.jobInfo = jobInfo;
    }
    
    public void writeZipFile() {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            writeOutputFilesToZip(zos);
        }
        catch (IOException e) {
            log.error("Didn't write zip file: "+e.getLocalizedMessage(), e);
            return;
        }
    }
    
    public void writeOutputFilesToZip(ZipOutputStream zos) throws IOException {
        for(OutputFile outputFile : jobInfo.getOutputFiles()) {
            addFileToZip(zos, jobInfo.getJobNumber(), outputFile.getOutputFile());            
        }
        for(JobInfoWrapper step : jobInfo.getAllSteps()) {
            for(OutputFile outputFile : step.getOutputFiles()) {
                addFileToZip(zos, step.getJobNumber(), outputFile.getOutputFile());                            
            }
        }
        zos.close();
    }
    
    private void addFileToZip(ZipOutputStream zos, int jobNumber, File attachment) {
        if (attachment == null || !attachment.canRead()) {
            log.error("File not added to zip entry, jobNumber="+jobNumber+", outputFile="+attachment);
            return;
        }
        
        FileInputStream is = null;
        try {
            is = new FileInputStream(attachment);
            String zipEntryName = jobNumber + "/" + attachment.getName();
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipEntry.setTime(attachment.lastModified());
            zipEntry.setSize(attachment.length());
            zos.putNextEntry(zipEntry);
            int bytesRead;
            while ((bytesRead = is.read(b, 0, b.length)) != -1) {
                zos.write(b, 0, bytesRead);
            }
            zos.closeEntry();
        }
        catch (FileNotFoundException e) {
            log.error("Error in addFileToZip: "+e.getLocalizedMessage(), e);
            return;
        }
        catch (IOException e) {
            log.error("Error in addFileToZip: "+e.getLocalizedMessage(), e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Error closing input stream in addFileToZip: "+e.getLocalizedMessage());
                }
            }
        } 
    }
}
