package org.genepattern.test.gp2580;

import org.genepattern.client.GPClient;
import org.genepattern.test.Util;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test case:
 *     From the SOAP client run a job which takes an uploaded input file;
 *     Then reload that job using a link back to the previously uploaded input file.
 */
public class TestGp2645 {

    public static void main(String[] args) {
        GPClient gpClient = null;
        JobResult jobResult = null;
        try {
            gpClient = Util.initFromCmdLineArgs(args);

            // 1. run a module with an uploaded input file,
            jobResult = gpClient.runAnalysis("PreprocessDataset",
                    new Parameter[] { 
                        new Parameter("input.filename", "all_aml_test.gct") 
                    });
            Util.printJobResults(System.out, jobResult); 
            
            // 2. then reload the job using a URL to the upload to the previous job
            String reloadInputFilename = Util.getReloadInputParameter(gpClient, jobResult, "input.filename");
            jobResult = gpClient.runAnalysis("PreprocessDataset",
                    new Parameter[] { 
                        new Parameter("input.filename", reloadInputFilename) 
                    });
            Util.printJobResults(System.out, jobResult); 
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
 }
