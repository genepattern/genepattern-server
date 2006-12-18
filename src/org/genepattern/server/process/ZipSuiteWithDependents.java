/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package org.genepattern.server.process;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipOutputStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class ZipSuiteWithDependents extends ZipSuite {

	/* (non-Javadoc)
	 * @see org.genepattern.server.process.ZipSuite#packageSuite(org.genepattern.webservice.SuiteInfo, java.lang.String)
	 */
	public File packageSuite(SuiteInfo suiteInfo, String userID) throws Exception {
		String name = suiteInfo.getName();
		
		// create zip file
		File zipFile = File.createTempFile(name, ".zip");
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		try {
			// find dependent tasks (if a pipeline) and add them to the zip
			// file as zip files
			zipDependentTasks(zos, suiteInfo, userID);
			zos.finish();
			zos.close();
			return zipFile;
		} catch (Exception e) {
			zos.close();
			zipFile.delete();
			throw e;
		}
	}

	/**
	 * @param zos
	 * @param suiteInfo
	 * @param userID
	 * @throws Exception
	 */
	private void zipDependentTasks(ZipOutputStream zos, SuiteInfo suiteInfo,
			String userID) throws Exception {
		File tmpDir = new File(System.getProperty("java.io.tmpdir"), suiteInfo.getName());
		try {
			tmpDir.mkdir();
			File parent = super.packageSuite(suiteInfo, userID);
			zipFile(zos, parent);
			parent.delete();

			String[] lsids = suiteInfo.getModuleLsids();
			ZipTask zt = new ZipTaskWithDependents();
			for (String lsid:lsids) {
				File zipTask = zt.packageTask(lsid, userID);
				zipFile(zos, zipTask);
				zipTask.delete();
			}
		} finally {
			tmpDir.delete();
		}
	}
}