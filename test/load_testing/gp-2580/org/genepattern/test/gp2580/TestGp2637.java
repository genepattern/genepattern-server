package org.genepattern.test.gp2580;
import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test nested pipelines from the java API.
 * This is used to test bug GP-2637, <a href="http://jira.broad.mit.edu:8008/browse/GP-2580">GP-2637</a>.
 */
public class TestGp2637 {

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
            
            System.out.println("Job "+jobResult.getJobNumber());
            boolean first = true;
            for(Parameter param : jobResult.getParameters()) {
                if (first) {
                    System.out.println("Parameters");
                    first = false;
                }
                System.out.println("\t"+param.getName() + "=" + param.getValue());
            }
            first = true;
            for(String outputFile : jobResult.getOutputFileNames()) {
                if (first) {
                    System.out.println("Output Files");
                    first = false;
                }
                System.out.println("\t"+outputFile+", "+jobResult.getURLForFileName(outputFile));
            }
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
