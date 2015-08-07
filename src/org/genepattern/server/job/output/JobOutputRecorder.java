/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.util.JobResultsFilenameFilter;

/**
 * Helper class for listing job output files from the working directory
 * and recording the meta-data into the database.
 * @author pcarr
 *
 */
public class JobOutputRecorder {
    private static final Logger log = Logger.getLogger(JobOutputRecorder.class);
   
    public static void recordOutputFilesToDb(GpConfig gpConfig, GpContext jobContext, File jobDir) throws DbException {
        log.debug("recording files to db, jobId="+jobContext.getJobNumber());
        List<JobOutputFile> allFiles=new ArrayList<JobOutputFile>();
        DefaultGpFileTypeFilter filter=new DefaultGpFileTypeFilter();
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig, jobContext);
        filter.setJobResultsFilenameFilter(filenameFilter);
        JobResultsLister lister=new JobResultsLister(""+jobContext.getJobNumber(), jobDir, filter);
        try {
            lister.walkFiles();
            allFiles.addAll( lister.getOutputFiles() );
        }
        catch (IOException e) {
            log.error("output files not recorded to database, disk usage will not be accurate for jobId="+jobContext.getJobNumber(), e);
            return;
        } 

        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobOutputDao dao=new JobOutputDao();
            dao.recordOutputFiles(allFiles);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }            
        }
        catch (Throwable t) {
            final String errorMessage="Error recording output files for jobId="+jobContext.getJobNumber();
            log.error(errorMessage, t);
            HibernateUtil.rollbackTransaction();
            throw new DbException(errorMessage, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}
