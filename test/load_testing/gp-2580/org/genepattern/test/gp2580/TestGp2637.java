package org.genepattern.test.gp2580;

import org.genepattern.client.GPClient;
import org.genepattern.test.Util;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test nested pipelines from the java API.
 * This is used to test bug GP-2637, <a href="http://jira.broadinstitute.org:8008/browse/GP-2580">GP-2637</a>.
 */
public class TestGp2637 {

    public static void main(String[] args) {
        GPClient gpClient = null;
        JobResult jobResult = null;
        try {
            gpClient = Util.initFromCmdLineArgs(args);

            // 0. run innerStep with URL
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct") 
                });
            Util.printJobResults(System.out, jobResult);

            // 1. run innerStep with uploaded file
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "all_aml_test.gct") 
                    });
            Util.printJobResults(System.out, jobResult); 
            
            // 2. reload, run innerStep using a URL to the previously uploaded file
            String reloadInputFilename = Util.getReloadInputParameter(gpClient, jobResult, "PreprocessDataset1.input.filename");
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", reloadInputFilename) 
                    });
            Util.printJobResults(System.out, jobResult); 

        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
 }
