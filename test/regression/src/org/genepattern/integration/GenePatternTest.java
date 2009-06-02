package org.genepattern.integration;

import java.io.File;
import java.io.IOException;

import org.broadinstitute.io.matrix.ParseException;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

import junit.framework.TestCase;


public abstract class GenePatternTest extends TestCase {

	/**
	 * Runs the module on GenePattern and downloads the result files.
	 * @param params Parameters for the run.
	 * @param lsid Lsid of module to run.
	 * @param expectedNumFiles expected number of result files
	 * @return Downloaded result files.
	 * @throws WebServiceException
	 * @throws IOException
	 */
	protected File[] runModule(Parameter[] params, String lsid, int expectedNumFiles)
			throws WebServiceException, IOException {
				JobResult jobResult = runModuleHelper(params, lsid);
				File[] files = jobResult.downloadFiles(".");
				assertNotNull(files);
				assertEquals(expectedNumFiles, files.length);
				return files;
			}

	/**
	 * Validates the result file.  Checks the result filename against the expected filename.
	 * @param files - downloaded GenePattern result files.
	 * @param expectedFilename Expected name of the result file containing microarray data.
	 * @param expectedNumRows expected number of rows in result file dataset.
	 * @param expectedNumColumns expected number of columns in result file dataset.
	 * @throws IOException
	 * @throws ParseException
	 */
	protected void validateResultFile(File[] files, String expectedFilename, int expectedNumRows,
			int expectedNumColumns) throws IOException, ParseException {
				if (!expectedFilename.equals(files[0].getName())) {
					fail("found unexpected output file: " + files[0].getName());
				}
				assertNotNull(files[0]);
			}

	/**
	 * Runs the module on GenePattern and returns the jobResult.
	 * @param params Parameters for the run.
	 * @param lsid LSID of the module to run on GenePattern.
	 * @return jobResult for the module
	 * @throws WebServiceException
	 */
	private JobResult runModuleHelper(Parameter[] params, String lsid)
			throws WebServiceException {
				GenePatternConfig config = new GenePatternConfig();
				GPClient client = new GPClient(config.getGenePatternUrl(), config.getUsername(), config.getPassword());
				assertNotNull(client);
				JobResult jobResult = client.runAnalysis(lsid, params);
				assertNotNull(jobResult);
				assertTrue(!jobResult.hasStandardError());
				return jobResult;
			}

}
