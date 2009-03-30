package org.genepattern.test.gp2580;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test nested pipelines from the java API.
 * This is used to test bug GP-2580, <a href="http://jira.broad.mit.edu:8008/browse/GP-2580">GP-2580</a>.
 */
public class TestGp2580 {

    public static void main(String[] args) {
        String server = "http://127.0.0.1:8080";
        String username = "pcarr";
        String password = "pcarr";

        if (args.length > 0) {
            server = args[0];
        }
        if (args.length > 1) {
            username = args[1];
            if (args.length > 2) {
                password = args[2];
            } 
            else {
                password = null;
            }
        }

        GPClient gpClient = null;
        JobResult jobResult = null;
        try {
            gpClient = new GPClient(server, username, password);

            // 1. run innerStep with uploaded file
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "all_aml_test.gct") 
                    });

            // 2. run innerStep with URL
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "ftp://ftp.broad.mit.edu/pub/genepattern/datasets/all_aml/all_aml_test.gct") 
                    });
            
            // 3. run innerStep with local server file URL
            jobResult = gpClient.runAnalysis("innerStep",
                    new Parameter[] { 
                        new Parameter("PreprocessDataset1.input.filename", "file:///xchip/genepattern/node255/gp-3.1.1-beta/taskLib/outer.1.931/all_aml_test.gct") 
                    });
            
            // 4. run outer pipeline
            jobResult = gpClient.runAnalysis("outer",
                    new Parameter[] { 
                    });
            
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
