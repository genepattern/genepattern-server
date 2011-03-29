package org.genepattern.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import junit.framework.TestCase;

import org.genepattern.client.GPClient;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LabelledParameterized;
import org.genepattern.util.TestUtil;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.yaml.snakeyaml.Yaml;

/**
 * Developed for regression testing the CommandLineParser class, which was added in 3.3.1, by comparing results of jobs run on the production 3.2.3 server.
 * 
 * @author pcarr
 *
 */
@RunWith(LabelledParameterized.class)
public class CommandLineParserRegressionTest extends TestCase {
    final private static String genePatternUrl = "http://genepattern.broadinstitute.org/gp";
    //final private static String genePatternUrl = "http://genepatterntest.broadinstitute.org/gp";
    final private static String adminUser = "admin";
    //TODO: gather credentials from the env
    final private static String adminPassword = "c*****";

    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gptest_testcases.properties" };
    //final private static String serverProperties = "/xchip/gpdev/users/pcarr/regression_test/gptest_gp.properties";
    //final private static String libdirProperties = "/xchip/gpdev/users/pcarr/regression_test/gptest_lsid_libdir.properties";

    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_0000.properties" };
    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_0400.properties" };
    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_0800.properties" };
    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_1200.properties" };
    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_1600.properties" };
    final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpprod_testcases/gpprod_testcases_2000.properties" };
    //final private static String[] testCaseFiles = { "/xchip/gpdev/users/pcarr/regression_test/gpserver.mirror_testcases/gpserver.mirror_testcases_0_119.properties" };
    final private static String serverProperties = "/xchip/gpdev/users/pcarr/regression_test/gpprod_gp.properties";
    final private static String libdirProperties = "/xchip/gpdev/users/pcarr/regression_test/gpprod_libdir.properties";
   

    private static Properties _gpProperties = null;
    private static Properties _libdirProperties = null;
    
    private static void loadProperties() throws FileNotFoundException, IOException {
        _gpProperties = loadPropertiesFromFile(serverProperties);
        _libdirProperties = loadPropertiesFromFile(libdirProperties);
    }

    private static Properties loadPropertiesFromFile(String filepath) throws FileNotFoundException, IOException {
        Properties gpProperties = new Properties();
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(new File(filepath));
            gpProperties.load(fileReader);
        }
        finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }
        return gpProperties;
    }

    private static Properties getGpProperties() {
        if (_gpProperties != null) {
            return _gpProperties;
        }
        try {
            loadProperties();
        }
        catch (Throwable t) {
            t.printStackTrace();
            _gpProperties = new Properties();
        }
        return _gpProperties;
    }

    private static Collection<TestCase[]> testCases = initTestCases();
    private static Collection<TestCase[]> initTestCases() {
        List<TestCase> testCases = new ArrayList<TestCase>();
        for(String testCaseFile : testCaseFiles) {
            List<TestCase> entry = loadTestCasesFromFile(testCaseFile);
            testCases.addAll( entry );
        }
        Collection<TestCase[]> rval = new ArrayList<TestCase[]>();
        for(TestCase testCase : testCases) {
            rval.add( new TestCase[] { testCase } );
        }
        return rval;
    }

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * @return
     */
    @Parameters
    public static Collection<TestCase[]> data() {
        return testCases;
    }
    
    private static List<TestCase> loadTestCasesFromFile(String filename) {
        List<TestCase> testCases = new ArrayList<TestCase>();
        
        File testCaseFile = new File(filename);
        if (!testCaseFile.isAbsolute()) {
            File parentDir = TestUtil.getSourceDir();
            testCaseFile = new File(parentDir, filename);
        }
        Reader reader = null;
        try {
            reader = new FileReader(testCaseFile);
        }
        catch (FileNotFoundException e) {
            Assert.fail("Error reading test cases from file: "+testCaseFile.getAbsolutePath());
        }
        
        //it's a properties file
        Properties props = new Properties();
        try {
            props.load(reader);
        }
        catch (IOException e) {
            return testCases;
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                
            }
        }
        
        for(Entry<Object,Object> entry : props.entrySet()) {
            String jobId = entry.getKey().toString();
            String cmdLine = entry.getValue().toString();
            TestCase next = new TestCase(jobId, cmdLine);
            testCases.add(next);
        }
        return testCases;
    }

    
    final private TestCase testCase;
    public CommandLineParserRegressionTest(TestCase testCase) {
        this.testCase = testCase;
    }

    private static final String NL = "\n";
    private static void writeTestCase(Writer out, TestCase testCase) throws IOException {
        try {
            testCase.loadJob();
        }
        catch (Throwable t) {
            System.err.println("Ignoring job "+testCase.jobId+": "+t.getLocalizedMessage());
            return;
        }
        
        String job_status = testCase.jobInfo.getStatus();
        if (!(JobStatus.FINISHED.equalsIgnoreCase(job_status))) {
            System.err.println("Ignoring job "+testCase.jobId+": status="+job_status); 
            return;
        }
        
        writeTestCase_yaml(out, testCase); 
    }
    
    private static void writeTestCase_orig(Writer out, TestCase testCase) throws IOException {
        
        out.write("job_"+testCase.jobId+":"+NL);
        out.write("    description: auto-generated"+NL);
        out.write("    cmdLine: \""+ escapeQuotes(testCase.commandLine) + "\"" +NL);
        out.write("    env:"+NL);
        out.write("        libdir: \""+ escapeQuotes(testCase.libdir) + "\"" +NL);
        for(Entry<?,?> entry : testCase.jobProperties.entrySet()) {
            out.write("        "+ escapeQuotes(entry.getKey().toString())+": \""+ escapeQuotes(entry.getValue().toString()) + "\"" +NL);
        }
        out.write("    expected: ["+NL);
        for(String arg : testCase.lsfCmdLineArgs) {
            out.write("        \""+escapeQuotes(arg)+"\", "+NL);
        }
        out.write("    ] "+NL);
        out.flush();
    }
    
    private static void writeTestCase_yaml(Writer out, TestCase testCase) throws IOException {
        Yaml yaml = new Yaml();

        HashMap<String,Object> dataList = new HashMap<String,Object>();
        dataList.put("description", "auto-generated");
        dataList.put("cmdLine", testCase.commandLine);
        HashMap<String,String> envMap = new HashMap<String,String>();
        envMap.put("libdir", testCase.libdir);
        for(Entry<?,?> entry : testCase.jobProperties.entrySet()) {
            envMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        dataList.put("env", envMap);
        dataList.put("taskParameterInfo", testCase.taskInfo.getParameterInfo()); 
        dataList.put("expected", testCase.lsfCmdLineArgs);
        HashMap<String,Object> testCaseData = new HashMap<String,Object>();
        testCaseData.put("test_"+testCase.jobId, dataList);
        
        String str = yaml.dump(testCaseData);
        out.write(str + NL);
        out.flush();
    }

    final private static String ESC_PATTERN = Pattern.quote("\"");
    final private static String ESC_REPLACE = Matcher.quoteReplacement("\\\"");
    private static String escapeQuotes(String in) {
        String rval = in.replaceAll(ESC_PATTERN, ESC_REPLACE);
        return rval;
    }
    
    public static void main(String[] args) {
        try {
            String outputpath = "generated_test_cases.yaml";
            writeTestCases(outputpath);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private static void writeTestCases(String outputpath) throws IOException {
        File out = new File(outputpath);
        FileWriter writer = new FileWriter(out);
        
        try { 
            Collection<TestCase[]> data = data();
            for(TestCase[] entry : data) {
                TestCase testCase = entry[0];
                writeTestCase(writer, testCase);
            }
        }
        finally {
            writer.flush();
            writer.close();
        }
    }
    
    @Test
    public void testCreateCmdLine() {
        try {
            testCase.loadJob();
        }
        catch (Throwable t) {
            System.err.println("Error loading job "+testCase.jobId+": "+t.getLocalizedMessage());
            return;
            //fail("Error loading job "+testCase.jobId+": "+t.getLocalizedMessage());
        }
        testCase.runTest();
    }

    private static class TestCase {
        private GPClient client = null; 
        private AnalysisWebServiceProxy analysisProxy = null;

        final private String jobId;
        final private String lsfCmdLine;
        
        final private List<String> lsfCmdLineArgs;
        private Properties jobProperties = new Properties();
        private String libdir;

        private int jobNumber = -1;
        private JobInfo jobInfo = null;
        private TaskInfo taskInfo = null;
        private String commandLine = null;

        public TestCase(String jobId, String lsfCmdLine) {
            this.jobId = jobId;
            this.lsfCmdLine = lsfCmdLine;
            this.lsfCmdLineArgs = tokenizeLsfCmdLine(lsfCmdLine);
        }
        
        private void connectToServer() throws WebServiceException {
            client = new GPClient(genePatternUrl, adminUser, adminPassword);
            analysisProxy = new AnalysisWebServiceProxy(genePatternUrl, adminUser, adminPassword, false);
        }
        
        private List<String> tokenizeLsfCmdLine(String str) {
            List<String> cmdLineArgs = new ArrayList<String>();
            //('[^']*'|>>)
            String patternRegex = "('[^']*'|>>)";
            Pattern pattern = null;
            try {
                pattern = Pattern.compile(patternRegex);
            }
            catch (PatternSyntaxException e) {
                fail("Error creating pattern for regexp='"+patternRegex+"', : "+e.getLocalizedMessage());
            }
            catch (Throwable t) {
                fail("Error creating pattern for regexp='"+patternRegex+"', : "+t.getLocalizedMessage());
            }
            Matcher matcher = pattern.matcher(str);
            while(matcher.find()) {
                int sidx = matcher.start();
                int eidx = matcher.end();
                String param = str.substring(sidx, eidx);
                //remove wrapping quote (') chars
                if (param.length() >= 2) {
                    if (param.startsWith("'") && param.endsWith("'")) {
                        param = param.substring(1, param.length()-1);
                    }
                }
                cmdLineArgs.add(param);
            }

            if (cmdLineArgs.size() > 2) {
                cmdLineArgs.remove(cmdLineArgs.size()-1);
                cmdLineArgs.remove(cmdLineArgs.size()-1);
            }
            return cmdLineArgs;
        }

        private JobInfo getJobInfo(int jobID) throws WebServiceException {
            JobInfo jobInfo = analysisProxy.checkStatus(jobID);
            return jobInfo;
        }
        
        public void loadJob() {
            try {
                jobNumber = Integer.parseInt(jobId);
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid jobId="+jobId);
            }
            try {
                connectToServer();
            }
            catch (Throwable t) {
                throw new IllegalArgumentException(t.getLocalizedMessage());
            }
            try { 
                jobInfo = getJobInfo(jobNumber);
                String lsidStr = jobInfo.getTaskLSID();
                taskInfo = client.getModule(lsidStr);
                TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
                commandLine = taskInfoAttributes.get(GPConstants.COMMAND_LINE);
                loadProperties();
                initJobProperties();
                this.libdir = _libdirProperties.getProperty(taskInfo.getLsid());
            }
            catch (Throwable t) {
                throw new IllegalArgumentException(t.getLocalizedMessage());
            }
        }

        private void initJobProperties() {
            if (jobProperties == null) {
                jobProperties = new Properties();
            }
            else {
                jobProperties.clear();
            }
           
            for(ParameterInfo jobParam : jobInfo.getParameterInfoArray()) {
                String name = jobParam.getName();
                String value = null;
                if (jobParam.isOutputFile()) {
                    //skip output files
                }
                else if (isInputFile(jobParam)) {
                    //convert the value to a local server path, to duplicate the actual command line arg
                    value = convertInputValueToServerPath(jobInfo, jobParam);

                    //add input file params to properties, e.g. <_basename>
                    if (value != null && value.length() > 0) {
                        String filename = value;
                        String basename = null;
                        String extension = null;
                        int idx = value.lastIndexOf("/");
                        if (idx > 0) {
                            filename = value.substring(idx+1);
                        }
                        idx = filename.lastIndexOf(".");
                        if (idx > 0) {
                            basename = filename.substring(0, idx);
                            if (idx < (filename.length()-1)) {
                                extension = filename.substring(idx+1);
                            }
                            
                            //special-case Axis in basename, e.g. Axis4947781235416423435.att_small
                            if (basename.startsWith("Axis")) {
                                int basename_idx = basename.indexOf("_");
                                if (basename_idx>0) {
                                    ++basename_idx;
                                    if (basename_idx < basename.length()) {
                                        basename = basename.substring(basename_idx);
                                    }
                                }
                            }
                        }
                        
                        jobProperties.put(jobParam.getName() + GPConstants.INPUT_FILE, filename);
                        if (basename != null) {
                            jobProperties.put(jobParam.getName() + GPConstants.INPUT_BASENAME, basename);
                        }
                        if (extension != null) {
                            jobProperties.put(jobParam.getName() + GPConstants.INPUT_EXTENSION, extension);
                        }
                    }
                }
                else {
                    value = jobParam.getValue();
                }
                if (value != null) {
                    jobProperties.put(name, value);
                }
            }
        }
        
        private boolean isInputFile(ParameterInfo param) {
            //Note: formalParameters is one way to check if a given ParameterInfo is an input file
            //ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
            if (param.isInputFile()) {
                return true;
            }
            //not to be confused with 'TYPE'
            String type = (String) param.getAttributes().get("type");
            if (type != null && type.equals("java.io.File")) {
                return true;
            }
            //special case: URL input via SOAP interface
            String mode = (String) param.getAttributes().get("MODE");
            if (mode != null && mode.equals("URL_IN")) {
                return true;
            }
            return false;
        }


        private String convertInputValueToServerPath(JobInfo jobInfo, ParameterInfo inputParam) {
            try {
                URL url = new URL(inputParam.getValue());
                String path = url.getPath();
                
                //1) check for prev job result file
                int idx = path.indexOf("/jobResults/");
                if (idx >= 0) {
                    idx += "/jobResults/".length();
                    path = path.substring(idx);
                    path = _gpProperties.getProperty("jobs") +"/" + path;
                    return path;
                }
                
                //2) check for user uploaded files from web client
                if (path.endsWith("gp/getFile.jsp")) {
                    //file=admin_run5948159958897378872.tmp/test.fastq
                    String query = url.getQuery();
                    idx = query.lastIndexOf("file=");
                    if (idx >= 0) {
                        idx += "file=".length();
                        String relPath = query.substring(idx);
                        int nextIdx = query.indexOf("&", idx);
                        if (nextIdx > idx) {
                            relPath = query.substring(idx, nextIdx);
                        }
                        path = _gpProperties.getProperty("java.io.tmpdir") + "/" + relPath;
                        return path;
                    }
                }

                //2) assume it is in working directory for the job
                idx = path.lastIndexOf("/");
                if (idx >= 0) {
                    path = path.substring(idx+1);
                    path = _gpProperties.getProperty("jobs") + "/" + jobInfo.getJobNumber() + "/" + path;
                }
                return path;
            }
            catch (MalformedURLException e) {
                //not a URL
            }
            
            return inputParam.getValue();
        }

        public void runTest() {
            ParameterInfo[] taskInfoParameters = taskInfo.getParameterInfoArray();
            Properties props = new Properties();
            Properties gpProps = getGpProperties();
            for(Entry<?,?> entry : gpProps.entrySet()) {
                props.put(entry.getKey().toString(), entry.getValue().toString());
            }
            
            for(Entry<?,?> entry : jobProperties.entrySet()) {
                props.put(entry.getKey().toString(), entry.getValue().toString());
            }
            
            //special-case, insert libdir for module
            taskInfo.getLsid();
            props.setProperty("libdir", libdir);
            List<String> actualCmdLineArgs = CommandLineParser.createCmdLine(commandLine, props, taskInfoParameters); 

            //compare expected to actual
            //assertEquals("numArgs in commandLine="+commandLine, lsfCmdLineArgs.size(), actualCmdLineArgs.size());

            //TODO: not yet implemented an exact match from the job results on the server and the values for all of the command substitutions
            //int i=0;
            //for(String expectedArg : lsfCmdLineArgs) {
            //    assertEquals("expecting arg["+i+"] to match", expectedArg, actualCmdLineArgs.get(i));
            //    ++i;
            //}
        }
        
        public String toString() {
            return "job_"+jobId;
        }
    }

}
