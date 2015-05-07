/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoWrapper.OutputFile;

/**
 * Helper class for writing all output files for a job to a zip file.
 * 
 * The method uses the OutputFile.getName() method to create each entry in the zip file.
 * In order for this to work correctly for jobs with sub directories and pipelines, the following assumptions are made:
 * 1. Each *job* gets its own top-level entry in the zip file. This applies to nested jobs.
 *    For example, if a pipeline (root.jobNo=1) has two steps (step1.jobNo=2, step2.jobNo=3),
 *    the directory structure of the resulting zip file includes all jobs at the top level.
 *         
 *         2/output1.txt   <-- step 1
 *         2/output2.txt   
 *         3/output1.txt   <-- step 2
 *         3/output2.txt
 *         
 *         Note: In most cases there will not be an entry for the root job, because it has no outputs.
 *         
 * 2. Each *output file*, gets an entry relative to its parent job, based on OutputFile#getName() (which is not the same as {@link java.io.File#getName()}). 
 *    Output files in sub directories will preserve the directory structure in the zip entry.
 *    <b>It is the responsibility of the JobInfoWrapper to ensure that OutputFile#getName() returns a String with '/' characters denoting the directory path.</b>
 *    For example, a job (jobNo=12) with some results in a sub directory,
 *    
 *        12/stdout.txt
 *        12/summary.htm
 *        12/data/f1.txt   <-- sub directory
 *        12/data/f2.txt
 * 
 * @author pcarr
 */
public class JobInfoZipFileWriter {
    private static Logger log = Logger.getLogger(JobInfoZipFileWriter.class);
    
    final int BUFSIZE = 10000;

    private JobInfoWrapper jobInfo = null;
    byte[] b = new byte[BUFSIZE];

    public JobInfoZipFileWriter(JobInfoWrapper jobInfo) {
        this.jobInfo = jobInfo;
    }
    
    public void writeOutputFilesToZip(ZipOutputStream zos) throws IOException {
        for(OutputFile outputFile : jobInfo.getOutputFiles()) {
            addFileToZip(zos, ""+jobInfo.getJobNumber(), outputFile);
        }
        for(JobInfoWrapper step : jobInfo.getAllSteps()) {
            for(OutputFile outputFile : step.getOutputFiles()) {
                addFileToZip(zos, ""+step.getJobNumber(), outputFile);
            }
        }
        zos.close();
    }
    
    private void addFileToZip(ZipOutputStream zos, String jobId, OutputFile outputFile) {
        File attachment = outputFile.getOutputFile();
        if (attachment == null || !attachment.canRead()) {
            log.error("File not added to zip entry, jobId="+jobId+", outputFile="+attachment);
            return;
        }
        
        FileInputStream is = null;
        try {
            is = new FileInputStream(attachment);
            String entryName = jobId + "/" + outputFile.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
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
            log.error("FileNotFoundException thrown for file: "+attachment.getPath(), e);
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
