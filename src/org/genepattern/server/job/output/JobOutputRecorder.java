/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.util.JobResultsFilenameFilter;
import org.genepattern.util.SemanticUtil;

/**
 * Helper class for listing job output files from the working directory
 * and recording the meta-data into the database.
 * @author pcarr
 *
 */
public class JobOutputRecorder {
    private static final Logger log = Logger.getLogger(JobOutputRecorder.class);
    private static final GpFileTypeFilter defaultFileTypeFilter=new DefaultGpFileTypeFilter();
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-M-dd hh:mm:ss", Locale.ENGLISH);
    
    public static List<JobOutputFile> getAllNonRetrievedExternalOutputFiles(final GpConfig gpConfig, final GpContext jobContext, 
            final File jobDir,  List<JobOutputFile> allFiles, JSONArray externalFileList){
        List<JobOutputFile> externalFiles=new ArrayList<JobOutputFile>();
        HashSet<String> existingFileNames = new HashSet<String>(); 
        for (JobOutputFile f : allFiles) {
            existingFileNames.add(f.getPath());
        }

        if (externalFileList != null){
            // we have output files that are external and not on the local disk.
            // add them as outputs as if they were present JTL 01/19/2021
            // see AWSBatchJobRunner>>awsFakeSyncDirectory()
            try {

                for (int i=0; i<externalFileList.length(); i++){
                    JSONObject obj = externalFileList.getJSONObject(i);
                    String fileNameAndPath = obj.getString("filename");
                    File externalFile = new File(fileNameAndPath);
                    String filename = externalFile.getName();
                    // skip the ones that have already been found as actual files like stdout, .rte.out and the jobdir itself
                    if (existingFileNames.contains(filename)) continue;

                    String length = obj.getString("size");
                    String date = obj.getString("date");
                    String time = obj.getString("time");
                    JobOutputFile  out = new JobOutputFile();
                    out.setGpJobNo(jobContext.getJobNumber());
                    out.setFileLength(Long.parseLong(length));
                    
                    //get the relative path from the job dir (there might be sub directories)
                    String relativePath = jobDir.toURI().relativize(externalFile.toURI()).getPath();
                    out.setPath(relativePath);
                    out.setExtension(JobOutputFile.initExtension(externalFile));
                    GpFileType gpFileType = defaultFileTypeFilter.getGpFileType(jobDir, externalFile, null);
                    if (gpFileType != null){
                        out.setGpFileType(gpFileType.name());
                        if (gpFileType.isHidden()) {
                            out.setHidden(true);
                        }
                    } else {
                        if (externalFile.isDirectory()){
                            out.setGpFileType(GpFileType.DIR.name());
                        } else {
                            out.setGpFileType(GpFileType.FILE.name());
                        }
                    }
                    out.setKind(SemanticUtil.getKind(externalFile));
                    //  "date":"2021-01-21","time":"07:26:59"
                    //formatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));

                    String dateInString = date + " " + time;
                    Date lastModified = formatter.parse(dateInString);
                    out.setLastModified(lastModified);
                    externalFiles.add(out);
                }

            } catch (Exception ee){
                log.error(ee);
            }
        }
        return externalFiles;
    }
    
    
    public static void recordOutputFilesToDb(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final File jobDir, JSONArray externalFileList) throws DbException {
        log.debug("recording files to db, jobId="+jobContext.getJobNumber());
        List<JobOutputFile> allFiles=new ArrayList<JobOutputFile>();
        DefaultGpFileTypeFilter filter=new DefaultGpFileTypeFilter();
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig, jobContext);
        filter.setJobResultsFilenameFilter(filenameFilter);
        JobResultsLister lister=new JobResultsLister(""+jobContext.getJobNumber(), jobDir, filter);
        try {
            lister.walkFiles();
            allFiles.addAll( lister.getOutputFiles() );
            // JTL 012121 add external files that were left behind on S3 and not copied to local disk
            if (externalFileList != null){
                List<JobOutputFile> externalFiles =  getAllNonRetrievedExternalOutputFiles(gpConfig, jobContext, jobDir, allFiles, externalFileList);
                allFiles.addAll(externalFiles);
            }
        }
        catch (IOException e) {
            log.error("output files not recorded to database, disk usage will not be accurate for jobId="+jobContext.getJobNumber(), e);
            return;
        } 

        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            JobOutputDao dao=new JobOutputDao(mgr);
            dao.recordOutputFiles(allFiles);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }            
        }
        catch (Throwable t) {
            final String errorMessage="Error recording output files for jobId="+jobContext.getJobNumber();
            log.error(errorMessage, t);
            mgr.rollbackTransaction();
            throw new DbException(errorMessage, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}
