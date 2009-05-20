package org.genepattern.test.gp2580;
import org.genepattern.client.GPClient;
import org.genepattern.test.Util;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;

/**
 * Test SOAP client with large input file.
 *
 * Before running this test make sure to get a copy of a large file, such as, <pre>
 *    \\Thumper21.broad.mit.edu\igv\dev\testfiles\cn\Very_large_copynumber.cn
 * </pre>
 */
public class TestGp2287 {

    public static void main(String[] args) {
        GPClient gpClient = null;
        JobResult jobResult = null;
        try {
            gpClient = Util.initFromCmdLineArgs(args);

           // 1. run ConvertLineEndings with large input file
           jobResult = gpClient.runAnalysis("ConvertLineEndings",
                    new Parameter[] { 
                        new Parameter("input.filename", "Very_large_copynumber.cn") 
                    });
           Util.printJobResults(System.out, jobResult);
        } 
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}
