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

package edu.mit.broad.gp.ws;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the GPServer class.
 * 
 * @author Joshua Gould
 */
public class TestGPServer extends Helper {
    final static String ALL_AML_TRAIN = "ftp://ftp.broad.mit.edu/pub/genepattern/all_aml/all_aml_train.res";

    /*
     * public void testConcurrentSubmissions() { List threads = new ArrayList();
     * for(int i = 0; i < 10; i++) { Thread t = new Thread() { public void run() {
     * JobResult r = runPreprocess(new Parameter[]{new
     * Parameter("input.filename", ALL_AML_TRAIN)});
     * assertTrue(r.hasStandardError() == false);
     * assertTrue(r.getOutputFileNames().length == 1); } }; t.start();
     * threads.add(t); } while(threads.size() > 0) { Thread t = (Thread)
     * threads.remove(0); t.join(); } }
     */

    /*
     * public void testMultipleConnections() { int numConnections = 100; for(int
     * i = 0; i < numConnections; i++) { new GPServer(server, userName); }
     * JobResult r = runPreprocess(new Parameter[]{new
     * Parameter("input.filename", ALL_AML_TRAIN)}); }
     */

    @Test
    public void testInvalidParameter() {
        JobResult r = runPreprocessFail(new Parameter[] { new Parameter("input.filename", ALL_AML_TRAIN),
                new Parameter("invalid", "test") });
        assertTrue("An exception should have been thrown before submission", r == null);
    }

    /** Runs PreprocessDataset with missing input.filename */
    @Test
    public void testMissingRequiredParameter() {
        JobResult r = runPreprocessFail(new Parameter[] { new Parameter("threshold", 10) });
        assertTrue("An exception should have been thrown before submission", r == null);
    }

    @Test
    public void testMissingOptionalParameter() {
        JobResult r = runPreprocess(new Parameter[] { new Parameter("input.filename", ALL_AML_TRAIN) });
        assertTrue("" + r.getJobNumber(), r.hasStandardError() == false);
        assertTrue(r.getOutputFileNames().length == 2);
    }

    @Test
    public void testInvalidValueForParameter() {
        JobResult r = runPreprocessFail(new Parameter[] { new Parameter("input.filename", ALL_AML_TRAIN),
                new Parameter("output.file.format", "invalid value") });
        assertTrue("An exception should have been thrown before submission", r == null);
    }

    @Test
    public void testDownloadOutputFiles() throws Exception {
        String outputFile = "test.html";
        JobResult r = gpServer.runAnalysis("ConvertLineEndings", new Parameter[] {
                new Parameter("input.filename", "http://www.google.com"), new Parameter("output.file", outputFile) });
        r.downloadFiles("download");
    }

    private JobResult runPreprocessFail(Parameter[] params) {
        return _runPreprocess(params, true);
    }

    private JobResult runPreprocess(Parameter[] params) {
        return _runPreprocess(params, false);
    }

    private JobResult _runPreprocess(Parameter[] params, boolean shouldFail) {
        try {
            JobResult r = gpServer.runAnalysis("PreprocessDataset", params);
            return r;
        } catch (Exception wse) {
            if (!shouldFail) {
                wse.printStackTrace();
                fail("Unexpected exception");
            }
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {

    }
}
