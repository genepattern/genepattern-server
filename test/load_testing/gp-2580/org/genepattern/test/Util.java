package org.genepattern.test;

import java.io.OutputStream;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

public class Util {

    public static GPClient initFromCmdLineArgs(String[] args) throws WebServiceException {
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
        gpClient = new GPClient(server, username, password);
        return gpClient;
    }

    public static void printJobResults(OutputStream out, JobResult jobResult) {
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
    
    /**
     * Parses the jobResult for the value of the input parameter and constructs a URL which 
     * can be used as the input parameter when 'reloading' the job. 
     * This is for a job run via the SOAP interface with an uploaded input file.
     * <pre>
     * Example output,
     *     PreprocessDataset1.input.filename=/Broad/Applications/gp-3.1.2-dev/temp/attachments/test/Axis14954.att_all_aml_test.gct
     * should be converted to
     *     http://127.0.0.1:8080/gp/getFile.jsp?task=&job=3186&file=test/Axis14954.att_all_aml_test.gct
     * when reloading the job.
     * </pre>
     * 
     * @param gpClient
     * @param jobResult, for a job which included an uploaded input file.
     * @param inputParameterName, the parameter name for the uploaded input file.
     * 
     * @return a link to use for the input parameter when reloading the job.
     */
    public static final String getReloadInputParameter(GPClient gpClient, JobResult jobResult, String inputParameterName) {
        String prevInputFilename = null;
        for(Parameter param : jobResult.getParameters()) {
            if (inputParameterName.equals(param.getName())) {
                prevInputFilename = param.getValue();
                break;
            }
        }
        int idx = prevInputFilename.indexOf("/Axis");
        idx += "/Axis".length();
        int idx2 = prevInputFilename.indexOf(".att", idx);
        String id = prevInputFilename.substring(idx, idx2);            
        String newInputFilename = 
            gpClient.getServer() + "/gp/getFile.jsp?task=&job="+jobResult.getJobNumber()+"&file="+gpClient.getUsername()+"/Axis"+id+".att_all_aml_test.gct";
        return newInputFilename;
    }


}
