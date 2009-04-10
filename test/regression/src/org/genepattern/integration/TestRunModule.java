package org.genepattern.integration;

import java.io.File;
import java.io.IOException;

import org.broadinstitute.io.IOUtil;
import org.broadinstitute.io.matrix.ParseException;
import org.broadinstitute.matrix.Dataset;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

import junit.framework.TestCase;


public class TestRunModule extends TestCase {
	
	public static final String PREPROCESS_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:3";
	public static final String GOLUB_PIPELINE_NO_VIEWERS_LSID = "urn:lsid:8020.jnedzel.gm94e-69f.broad.mit.edu:genepatternmodules:2:2";
	public static final String SMALL_GCT = "resources/data/small.gct";
	public static final String SMALL_RESULT_GCT = "small.preprocessed.gct";	
	
	public static final String ALL_AML_RES_URL = "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res";
	public static final String ALL_AML_RES_RESULT = "all_aml_train.preprocessed.res";
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is uploaded from client.
	 * @throws WebServiceException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws ParseException 
	 */
	public void testPreprocessUploadedFile() throws WebServiceException, SecurityException, IOException, ParseException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		File[] files = runPreprocessHelper(params);
		if (!SMALL_RESULT_GCT.equals(files[0].getName())) {
			fail("found unexpected output file: " + files[0].getName());
		}
		
		Dataset dataset = IOUtil.readDataset(files[0].getAbsolutePath());
		assertNotNull(dataset);
		assertEquals(2, dataset.getRowCount());
		assertEquals(3, dataset.getColumnCount());
	}

	/**
	 * Actually runs the PreprocessDataset job on GenePattern and downloads the result files.
	 * @param params Parameters for the run.
	 * @return Downloaded result files.
	 * @throws WebServiceException
	 * @throws IOException
	 */
	private File[] runPreprocessHelper(Parameter[] params)
			throws WebServiceException, IOException {
		GenePatternConfig config = new GenePatternConfig();
		GPClient client = new GPClient(config.getGenePatternUrl(), config.getUsername(), config.getPassword());
		assertNotNull(client);
		JobResult jobResult = client.runAnalysis(PREPROCESS_LSID, params);
		assertNotNull(jobResult);
		assertTrue(!jobResult.hasStandardError());
		
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);
		return files;
	}	
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is an url to a public
	 * Broad url.
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws WebServiceException 
	 * @throws ParseException 
	 */
	public void testPreprocessGpUrl() throws SecurityException, IOException, WebServiceException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		File[] files = runPreprocessHelper(params);
		if (!ALL_AML_RES_RESULT.equals(files[0].getName())) {
			fail("found unexpected output file: " + files[0].getName());
		}
		
		Dataset dataset = IOUtil.readDataset(files[0].getAbsolutePath());
		assertNotNull(dataset);
		assertEquals(5864, dataset.getRowCount());
		assertEquals(38, dataset.getColumnCount());		
	}

}
