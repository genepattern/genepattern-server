package org.genepattern.server.process;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.TimerTask;

import org.genepattern.server.webservice.server.AnalysisJobDataSource;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.indexer.Indexer;
import org.genepattern.server.util.BeanReference;
import org.genepattern.webservice.JobInfo;

/**
 * Periodically purge jobs that completed some number of days ago.
 * 
 * @author Jim Lerner
 */
public class Purger extends TimerTask {
	/** number of days back to preserve completed jobs */
	int purgeInterval = -1;

	public Purger(int purgeInterval) {
		this.purgeInterval = purgeInterval;
	}

	public void run() {
		if (purgeInterval != -1) {
			try {
				// find all purgeable jobs
				GregorianCalendar gcPurgeDate = new GregorianCalendar();
				gcPurgeDate.add(GregorianCalendar.DATE, -purgeInterval);
				System.out.println("Purger: purging jobs completed before "
						+ gcPurgeDate.getTime());

				AnalysisJobDataSource ds = BeanReference
						.getAnalysisJobDataSourceEJB();
				JobInfo[] purgeableJobs = ds.getJobInfo(gcPurgeDate.getTime());

				// purge expired jobs
				for (int jobNum = 0; jobNum < purgeableJobs.length; jobNum++) {
					try {
						int jobID = purgeableJobs[jobNum].getJobNumber();
						System.out.println("Purger: deleting jobID " + jobID);

						// delete search indexes for job
						try {
							Indexer.deleteJob(jobID);
						} catch (IOException ioe) {
							System.err
									.println(ioe
											+ " while deleting search index while deleting job "
											+ jobID);
						}

						// enumerate output files for this job and delete them
						File jobDir = new File(GenePatternAnalysisTask
								.getJobDir(Integer.toString(jobID)));
						File[] files = jobDir.listFiles();
						if (files != null) {
							for (int i = 0; i < files.length; i++) {
								files[i].delete();
							}
						}

						// delete the job directory
						jobDir.delete();

						// TODO: figure out which input files to purge. This is
						// hard, because an input file could be shared among
						// numerous jobs, some of which are not old enough to
						// purge yet

						ds.deleteJob(jobID);
					} catch (Exception e) {
						System.err.println(e + " while purging jobs");
					}
				}

				try {
					Indexer.optimize(Indexer.getIndexDir());
				} catch (IOException ioe) {
					System.err.println(ioe + " while optimizing search index");
				}

				long dateCutoff = gcPurgeDate.getTime().getTime();
				purge(System.getProperty("jobs"), dateCutoff);
				purge(System.getProperty("java.io.tmpdir"), dateCutoff);

			} catch (Exception e) {
				System.err.println(e + " while purging jobs");
			}
			System.out.println("Purger: done");
		}
	}

	protected void purge(String dirName, long dateCutoff) throws IOException {
		File[] moreFiles = new File(dirName).listFiles();
		//		System.out.println("cutoff: " + new Date(dateCutoff).toString());
		if (moreFiles != null) {
			for (int i = 0; i < moreFiles.length; i++) {
				//				System.out.println(moreFiles[i].getName() + ": " + new
				// Date(moreFiles[i].lastModified()).toString());
				if (moreFiles[i].getName().startsWith("Lucene")
						&& moreFiles[i].getName().endsWith(".lock"))
					continue;
				if (/* moreFiles[i].getName().startsWith("pipe") && */
				moreFiles[i].lastModified() < dateCutoff) {
					try {
						if (moreFiles[i].isDirectory()) {
							//						System.out.println("Purger: deleting pipeline " +
							// moreFiles[i]);
							File[] files = moreFiles[i].listFiles();
							if (files != null) {
								for (int j = 0; j < files.length; j++) {
									try {
										files[j].delete();
									} catch (SecurityException se) {
										System.err.println("unable to delete "
												+ files[j].getPath());
									}
								}
							}
						}
						try {
							moreFiles[i].delete();
						} catch (SecurityException se) {
							System.err.println("unable to delete "
									+ moreFiles[i].getPath());
						}
					} catch (SecurityException se) {
						System.err.println("unable to browse "
								+ moreFiles[i].getPath());
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