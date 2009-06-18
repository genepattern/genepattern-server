package org.genepattern.test.gp2580;
import org.genepattern.client.GPClient;
import org.genepattern.test.Util;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test nested pipelines from the java API.
 * This is used to test bug GP-2580, <a href="http://jira.broadinstitute.org:8008/browse/GP-2580">GP-2580</a>.
 */
public class TestGp2580 {

    public static void main(String[] args) {
        GPClient gpClient = null;
        JobResult jobResult = null;
        try {
            gpClient = Util.initFromCmdLineArgs(args);

           // 1. run innerStep with uploaded file
           jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "all_aml_test.gct") 
                    });
           Util.printJobResults(System.out, jobResult);

            // 2. run innerStep with URL
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct") 
                    });
            Util.printJobResults(System.out, jobResult);
            
            // 3. run innerStep with local server file URL
            final String url1 = "file:////Broad/Projects/gp-trunk/test/load_testing/gp-2580/all_aml_test.gct";
            final String url2 = "file:///xchip/genepattern/node255/gp-3.1.1-beta/taskLib/outer.1.931/all_aml_test.gct";
            jobResult = gpClient.runAnalysis("innerStep", 
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", url1) 
                    });
            Util.printJobResults(System.out, jobResult);
            
            // 4. run outer pipeline
            jobResult = gpClient.runAnalysis("outer",
                    new Parameter[] { 
                    });
            Util.printJobResults(System.out, jobResult);
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
