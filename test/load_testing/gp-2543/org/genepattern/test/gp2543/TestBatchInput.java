package org.genepattern.test.gp2543;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.genepattern.webservice.WebServiceException;

/**
 * Demo program which executes a module in batch input mode.
 * 
 * Run a module using each file in the input directory.
 * One job per input file.
 * 
 * See JIRA ticket <a href="http://jira.broadinstitute.org:8008/browse/GP-2513">GP-2453</a>.
 *
 * To run this test:
 *     1. Make sure the GenePattern.jar file is on the classpath; 
 *     2. Make sure there is an input folder with proper input files available to the working directory, e.g.
 *     <pre>
       mkdir preprocess_dataset_inputs
       cd preprocess_dataset_inputs
       wget all_aml_test.gct
       wget all_aml_train.gct
 *        
 */
public class TestBatchInput {
    //local path (relative to working directory) of batch input files
    private String inputFolder;
    //server connection
    private String server;
    private String username;
    private String password;
    GPClient gpClient;
   
    private TestBatchInput() {
    }
    
    private void initCmdLineArgs(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.out.println( "usage: java TestBatchInput <inputFolder> <server> <username> [<password>]" );
            System.out.println("\t e.g. input_files http://localhost:8080 <username> <password>");
            return;
        }
        if (args.length > 0) {
            inputFolder = args[0];
        }
        if (args.length > 1) {
            server = args[1];
        }
        if (args.length > 2) {
            username = args[2];
            if (args.length > 3) {
                password = args[3];
            } 
            else {
                password = null;
            }
        }
    }
    
    private void initGpClient() throws WebServiceException {
        gpClient = new GPClient(server, username, password);       
    }
    
    private List<File> getInputFiles(String inputFolderName) {
        SortedSet<File> files = new TreeSet<File>();
       
        File inputFolder = new File(inputFolderName);
        if (!inputFolder.canRead()) {
            //TODO: log exception: can't read folder
            return new ArrayList<File>(files);
        }
        if (!inputFolder.isDirectory()) {
            //TODO: log exception: input folder is not a directory
            return new ArrayList<File>(files);
        }

        File[] inputFileList = inputFolder.listFiles();
        for(File file : inputFileList) {
            if (file.isFile()) {
                files.add(file);
            }
            else {
                //TODO: process subdirectories or log error
            }
        }
        return new ArrayList<File>(files);
    }

    private void submitJobs() throws WebServiceException {
        for(File inputFile : getInputFiles(inputFolder)) {
            System.out.println("running module with input file: "+inputFile.getPath());
            //runPreprocessDataset(inputfile);
            runConvertLineEndings(inputFile);
        }
    }

    /**
     * Based on Java code output from the 'View Code' button on the GenePattern web page for the PreprocessDataset module.
     * @param inputFile
     * @throws WebServiceException
     */
    private void runPreprocessDataset(File inputFile) throws WebServiceException {
        final String moduleName = "PreprocessDataset";
        final String inputFileParamName = "input.filename";
        final String inputFileParamVal = inputFile.getPath();
        // run job with an uploaded input file
        JobResult jobResult = gpClient.runAnalysis(moduleName,
                new Parameter[] { new Parameter(inputFileParamName, inputFileParamVal) });
    }

    /**
     * Based on Java code output from the 'View Code' button on the GenePattern web page for the ConvertLineEndings module.
     * @param inputFile
     * @throws WebServiceException
     */
    private void runConvertLineEndings(File inputFile) throws WebServiceException {
        //String moduleName = "urn:lsid:broadinstitute.org:cancer.software.genepattern.module.analysis:00002:1";
        final String moduleName = "ConvertLineEndings";
        JobResult result = gpClient.runAnalysis(moduleName, 
                new Parameter[]{
                    new Parameter("input.filename", inputFile.getPath()), 
                    new Parameter("output.file", "<input.filename_basename>.cvt.<input.filename_extension>")}
        );
    }

    public static void main(String[] args) {
        try {
            TestBatchInput m = new TestBatchInput();
            m.initCmdLineArgs(args);
            m.initGpClient();
            m.submitJobs();
        }
        catch (Exception e) {
            e.printStackTrace();
            return; 
        }
    }
}
