package org.genepattern.integration;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.broadinstitute.io.IOUtil;
import org.broadinstitute.io.matrix.ParseException;
import org.broadinstitute.matrix.Dataset;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

import junit.framework.TestCase;

/**
 * Tests the ability to run GenePattern Modules.
 * @author jnedzel
 *
 */
public class TestRunModule extends TestCase {
	
	public static final String PREPROCESS_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:3";
	public static final String CONVERT_LSID = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:1";

	public static final String SMALL_GCT = "resources/data/small.gct";
	public static final String SMALL_PREPROCESS_RESULT_GCT = "small.preprocessed.gct";	
	public static final String SMALL_CONVERT_RESULT_GCT = "small.cvt.gct";
	
	public static final String ALL_AML_RES_URL = "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res";
	public static final String ALL_AML_RES_PREPROCESS_RESULT = "all_aml_train.preprocessed.res";
	public static final String ALL_AML_RES_CONVERT_RESULT = "all_aml_train.cvt.res";	
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is uploaded from client.
	 * 
	 * Tests a Java module.
	 * 
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
		File[] files = runModule(params, PREPROCESS_LSID);
		validateResultFile(files, SMALL_PREPROCESS_RESULT_GCT, 2, 3);
	}

	/**
	 * Validates the result file.  Checks the result filename against the expected filename.
	 * Opens the dataset and compares the number of rows and columns against the expected values.
	 * @param files - downloaded GenePattern result files.
	 * @param expectedFilename Expected name of the result file containing microarray data.
	 * @param expectedNumRows expected number of rows in result file dataset.
	 * @param expectedNumColumns expected number of columns in result file dataset.
	 * @throws IOException
	 * @throws ParseException
	 */
	private void validateResultFile(File[] files, String expectedFilename, int expectedNumRows, int expectedNumColumns) throws IOException,
			ParseException {
		if (!expectedFilename.equals(files[0].getName())) {
			fail("found unexpected output file: " + files[0].getName());
		}
		
		Dataset dataset = IOUtil.readDataset(files[0].getAbsolutePath());
		assertNotNull(dataset);
		assertEquals(expectedNumRows, dataset.getRowCount());
		assertEquals(expectedNumColumns, dataset.getColumnCount());
	}

	/**
	 * Runs the module on GenePattern and downloads the result files.
	 * @param params Parameters for the run.
	 * @param lsid Lsid of module to run.
	 * @return Downloaded result files.
	 * @throws WebServiceException
	 * @throws IOException
	 */
	private File[] runModule(Parameter[] params, String lsid)
			throws WebServiceException, IOException {
		JobResult jobResult = runModuleHelper(params, lsid);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);
		return files;
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
	
	/**
	 * Submits a PreprocessDataset job to GenePattern.  Input file is an url to a public
	 * Broad url.
	 * 
	 * Tests a Java module.
	 * 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws WebServiceException 
	 * @throws ParseException 
	 */
	public void testPreprocessExternalUrl() throws SecurityException, IOException, WebServiceException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		File[] files = runModule(params, PREPROCESS_LSID);
		validateResultFile(files, ALL_AML_RES_PREPROCESS_RESULT, 5864, 38);
	}
	
	public void testPreprocessGpUrl() throws SecurityException, IOException, WebServiceException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		
		JobResult jobResult = runModuleHelper(params, PREPROCESS_LSID);
		File[] files = jobResult.downloadFiles(".");
		assertNotNull(files);
		assertEquals(1, files.length);		
		
		URL inputUrl = jobResult.getURLForFileName(SMALL_PREPROCESS_RESULT_GCT);
		assertNotNull(inputUrl);
		
		fileParam = new Parameter("input.filename", inputUrl.toString());
		params[0] = fileParam;
		
		files = runModule(params, PREPROCESS_LSID);
		assertNotNull(files);
		assertEquals(1, files.length);
	}
	
	/**
	 * Submits a ConvertLineEndings job to Genepattern.  Input file is an uploaded file.
	 * 
	 * Tests a Perl module.
	 * @throws SecurityException
	 * @throws IOException
	 * @throws WebServiceException
	 * @throws ParseException
	 */
	public void testConvertLineEndingsUploadedFile() throws SecurityException, IOException, WebServiceException, ParseException {
		File testFile = new File(SMALL_GCT);		
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", testFile);
		params[0] = fileParam;
		File[] files = runModule(params, CONVERT_LSID);
		validateResultFile(files, SMALL_CONVERT_RESULT_GCT, 2, 3);
	}
	
	
	/**
	 * Submits a ConvertLineEndings job to Genepattern.  Input file is an url to a public
	 * Broad url.  
	 * 
	 * Tests a Perl module.
	 * @throws SecurityException
	 * @throws IOException
	 * @throws WebServiceException
	 * @throws ParseException
	 */
	public void testConvertLineEndingsGpUrl() throws SecurityException, IOException, WebServiceException, ParseException {
		Parameter[] params = new Parameter[1];
		Parameter fileParam = new Parameter("input.filename", ALL_AML_RES_URL);
		params[0] = fileParam;
		File[] files = runModule(params, CONVERT_LSID);
		validateResultFile(files, ALL_AML_RES_CONVERT_RESULT, 7129, 38);
	}	

}
