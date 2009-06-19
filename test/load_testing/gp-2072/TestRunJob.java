import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test submitting a GenePattern job from the java API.
 * This is used to verify bug GP-2513, http://jira.broadinstitute.org:8008/browse/GP-2513
 * 
 * This test program depends on the 'GenePattern Java Library'.
 * To run this program,<ol> 
 *     <li>download and unzip GenePattern.zip, 
 *     <li>add GenePattern.jar and all the jars in the lib directory to the classpath
 * </ol>
 *
 * After running this program (which executes three GenePattern jobs on the server),
 * connect to the server via a web client and verify that input files can be downloaded
 * after you select Info from the job result arrow drop down.
 */
public class TestRunJob {

    public static void main(String[] args) {
        String server = "http://127.0.0.1:8080";
        String username = "test";
        String password = "test";

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
        try {
            gpClient = new GPClient(server, username, password);

            // run ExpressionFileCreator with an uploaded input file
            JobResult efcResult = gpClient.runAnalysis("ExpressionFileCreator",
                    new Parameter[] {
                        new Parameter("input.file", "2cel_files.zip"),
                        new Parameter("quantile.normalization", "yes"),
                        new Parameter("background.correct", "yes") 
                    });

            // run PreprocessDataset with a URL input file
            String inputDataset = "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_train.res";
            JobResult preprocess = gpClient.runAnalysis("PreprocessDataset",
                    new Parameter[] { 
                        new Parameter("input.filename", inputDataset) 
                    });

            // run PreprocessDataest with an uploaded input file
            JobResult preprocess2 = gpClient.runAnalysis("PreprocessDataset",
                    new Parameter[] { 
                        new Parameter("input.filename", "all_aml_test.gct") 
                    });
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
