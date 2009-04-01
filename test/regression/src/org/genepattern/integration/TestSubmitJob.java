package org.genepattern.integration;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.broadinstitute.io.IOUtil;
import org.broadinstitute.io.matrix.ParseException;
import org.broadinstitute.matrix.Dataset;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

/**
 * Tests submitting a job to GenePattern using GenePattern client.
 * @author jnedzel
 *
 */
public class TestSubmitJob extends TestCase {
	
	public static final String PREPROCESS_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:3";
	public static final String SMALL_GCT = "small.gct";
	public static final String SMALL_RESULT_GCT = "small.preprocessed.gct";
	public static final String EXECUTION_LOG = "gp_execution_log.gct";
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.
	 * @throws WebServiceException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ParseException 
	 */
	public void testPreprocess() throws WebServiceException, SecurityException, IOException, ParseException {
		Parameter[] params = new Parameter[1];
		File testFile = new File(SMALL_GCT);
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		GenePatternConfig config = new GenePatternConfig();
		GPClient client = new GPClient(config.getGenePatternUrl(), config.getUsername(), config.getPassword());
		assertNotNull(client);
		JobResult jobResult = client.runAnalysis(PREPROCESS_LSID, params);
		assertNotNull(jobResult);
		assertTrue(!jobResult.hasStandardError());
		
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);
		if (!SMALL_RESULT_GCT.equals(files[0].getName())) {
			fail("found unexpected output file: " + files[0].getName());
		}
		
		Dataset dataset = IOUtil.readDataset(files[0].getAbsolutePath());
		assertNotNull(dataset);
		assertEquals(2, dataset.getRowCount());
		assertEquals(3, dataset.getColumnCount());
	}

}
