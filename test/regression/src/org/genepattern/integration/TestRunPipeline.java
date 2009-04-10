package org.genepattern.integration;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

/**
 * Tests submitting a job to GenePattern using GenePattern client.
 * @author jnedzel
 *
 */
public class TestRunPipeline extends TestCase {
	
	public static final String GOLUB_PIPELINE_NO_VIEWERS_LSID = "urn:lsid:8020.jnedzel.gm94e-69f.broad.mit.edu:genepatternmodules:2:2";
	public static final String EXECUTION_LOG = "gp_execution_log.gct";
	

	public void testGolubPipeline() throws WebServiceException, IOException {
		GenePatternConfig config = new GenePatternConfig();
		GPClient client = new GPClient(config.getGenePatternUrl(), config.getUsername(), config.getPassword());
		assertNotNull(client);
		JobResult jobResult = client.runAnalysis(GOLUB_PIPELINE_NO_VIEWERS_LSID, (Parameter []) null);
		assertNotNull(jobResult);
		assertTrue(!jobResult.hasStandardError());
		
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);
		String filename = files[0].getName();
		assertNotNull(filename);
		assertTrue(filename.contains("execution_log.html"));
	}

}
