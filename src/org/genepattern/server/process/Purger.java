/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

/**
 * Periodically purge jobs that completed some number of days ago and input files.
 * 
 */
public class Purger extends TimerTask {

    private static Logger log = Logger.getLogger(Purger.class);

    /** number of days back to preserve completed jobs */
    private int purgeInterval = -1;

    public Purger(int purgeInterval) {
	this.purgeInterval = purgeInterval;
    }

    @Override
    public void run() {
	if (purgeInterval != -1) {
	    try {
		HibernateUtil.beginTransaction();
		// find all purgeable jobs
		GregorianCalendar purgeDate = new GregorianCalendar();
		purgeDate.add(GregorianCalendar.DATE, -purgeInterval);
		log.info("Purger: purging jobs completed before " + purgeDate.getTime());

		AnalysisDAO ds = new AnalysisDAO();
		JobInfo[] purgeableJobs = ds.getJobInfo(purgeDate.getTime());

		for (int i = 0; i < purgeableJobs.length; i++) {
		    int jobID = purgeableJobs[i].getJobNumber();
		    ds.deleteJob(jobID); // delete the job from the database and recursively delete the job directory
		}

		HibernateUtil.commitTransaction();
		long dateCutoff = purgeDate.getTime().getTime();
		// remove input files uploaded using web form
		purge(new File(System.getProperty("java.io.tmpdir")), dateCutoff);

		File soapAttachmentDir = new File(System.getProperty("soap.attachment.dir"));
		File[] userDirs = soapAttachmentDir.listFiles();
		if (userDirs != null) {
		    for (File f : userDirs) {
			purge(f, dateCutoff);
			File[] files = f.listFiles();
			if (files == null || files.length == 0) {
			    f.delete();
			}
		    }
		}

	    } catch (Exception e) {
		HibernateUtil.rollbackTransaction();
		log.error("Error while purging jobs", e);
	    } finally {
		HibernateUtil.closeCurrentSession();
	    }
	}
    }

    protected void purge(File dir, long dateCutoff) {
	File[] files = dir.listFiles();
	if (files != null) {
	    for (int i = 0; i < files.length; i++) {
		if (files[i].lastModified() < dateCutoff) {
		    if (files[i].isDirectory()) {
			Delete del = new Delete();
			del.setDir(files[i]);
			del.setIncludeEmptyDirs(true);
			del.setProject(new Project());
			del.execute();
		    } else {
			files[i].delete();
		    }
		}
	    }
	}

    }

    public static void main(String args[]) {
	Purger purger = new Purger(7);
	purger.run();
    }
}
