package org.genepattern.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.MyLabelledParameterized;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import org.yaml.snakeyaml.Yaml;

/**
 * Test cases for CommandLineParser#getCommandLine.
 * To add a new test case edit one of the testCaseFiles or add a new testCaseFile to the list.
 * @author pcarr
 */
@RunWith(MyLabelledParameterized.class)
public class GetCommandLineTest {
    private static String[] testCaseFiles={ 
        //"debug_test_case.yaml",
        //"test_cases.yaml", 
        //"gptest_generated_test_cases.yaml",
        "gpprod_generated_testcases_0000.yaml",
        "gpprod_generated_testcases_0400.yaml",
        "gpprod_generated_testcases_0800.yaml",
        "gpprod_generated_testcases_1200.yaml",
        "gpprod_generated_testcases_1600.yaml",
        "gpprod_generated_testcases_2000.yaml",
        };
    private static String[] testCaseGpProperties={ 
        //"gpprod_gp.properties",
        //null, 
        //null, 
        //"gptest_gp.properties",
        "gpprod_gp.properties",
        "gpprod_gp.properties",
        "gpprod_gp.properties",
        "gpprod_gp.properties",
        "gpprod_gp.properties",
        "gpprod_gp.properties",
        };
    private static String[] testCaseIds={
        //"gpprod_",
        //"",
        //"",
        //"gptest_",
        "gpprod_",
        "gpprod_",
        "gpprod_",
        "gpprod_",
        "gpprod_",
        "gpprod_",
    };

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * @return
     */
    @Parameters
    public static Collection<TestData[]> data() {
        Properties emptyProps = new Properties();
        List<TestData> testCases = new ArrayList<TestData>();
        int i=0;
        for(String testCaseFile : testCaseFiles) {
            Properties props = emptyProps;
            try {
                if (testCaseGpProperties[i] != null) {
                    props = loadPropertiesFromFile(testCaseGpProperties[i]);
                }
                props.setProperty("path.separator", ":");
                props.setProperty("file.separator", "/");

                testCases.addAll( loadTestCasesFromFile(testCaseIds[i], testCaseFile, props) );
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            ++i;
        }
        
        Collection<TestData[]> rval = new ArrayList<TestData[]>();
        for(TestData testCase : testCases) {
            rval.add( new TestData[] { testCase } );
        }
        return rval;
    }
    
    private static Properties loadPropertiesFromFile(String filepath) throws FileNotFoundException, IOException {
        Properties gpProperties = new Properties();
        File propsFile = new File(filepath);
        if (!propsFile.isAbsolute()) {
            propsFile = FileUtil.getSourceFile(GetCommandLineTest.class, filepath);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propsFile);
            gpProperties.load(fis);
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }
        return gpProperties;
    }


    /**
     * Helper class for representing a single test case loaded from the (yaml) configuration file.
     */
    private static class TestData {
        private String name = "";
        private String description = "";
        private String cmdLine = "";
        private Map<String,String> env;
        private Map<String,ParameterInfo> parameterInfoMap = Collections.emptyMap();

        private List<String> expected;
        
        private Properties gpProperties = new Properties();
        
        /**
         * Initialize a test-case from the test_cases.yaml file.
         * @param map, loaded from yaml parser.
         */
        public TestData(final String name, final Properties gpProperties, final Object obj) {
            this.name = name;
            this.gpProperties = gpProperties;
            
            Map<?,?> map = null;
            if (obj instanceof Map<?,?>) {
                map = (Map<?,?>) obj;
            }
            else {
                return;
            }
            Object descriptionObj = map.get("description");
            if (descriptionObj != null) {
                if (descriptionObj instanceof String) {
                    this.description = (String) descriptionObj;
                }
                else {
                    Assert.fail("Error parsing config file for test-case: "+name+". Description is not a String.");
                }
            }
            else {
                this.description = this.name;
            }
            Object cmdLineObj = map.get("cmdLine");
            Object envObj = map.get("env");
            Object expectedObj = map.get("expected");
            
            setCmdLine(cmdLineObj);
            setEnv(envObj);
            setExpected(expectedObj);
            
            Object formalParametersObj = map.get("taskParameterInfo");
            if (formalParametersObj != null && formalParametersObj instanceof String) {
                String formalParametersStr = (String) formalParametersObj;
                ParameterInfo[] formalParameters = ParameterFormatConverter.getParameterInfoArray(formalParametersStr);
                setFormalParameters(formalParameters);
            }
        }
        
        private void setCmdLine(Object obj) {
            cmdLine = obj.toString();
        }
        private void setEnv(Object obj) {
            env = new LinkedHashMap<String,String>();
            for(Entry<?,?> entry : gpProperties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    env.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            
            if (obj instanceof Map<?,?>) {
                for(Entry<?,?> entry : ((Map<?,?>)obj).entrySet() ) {
                    String key = null;
                    String value = null;
                    if (entry.getKey() != null) {
                        key = entry.getKey().toString();
                    }
                    if (entry.getValue() != null) {
                        value = entry.getValue().toString();
                    }
                    if (key != null && value != null) {
                        env.put( key, value );
                    }
                }
            }
        }
        private void setFormalParameters(ParameterInfo[] params) {
            parameterInfoMap = new HashMap<String, ParameterInfo>();
            for(ParameterInfo param : params) {
                parameterInfoMap.put(param.getName(), param);
            }
        }
        private void setExpected(Object env) {
            expected = new ArrayList<String>();
            if (env instanceof List) {
                List<?> in = (List<?>) env;
                for(Object arg : in) {
                    expected.add(arg.toString());
                }
            }
        }
        
        public String toString() {
            return name;
        }
    }

    private static List<TestData> loadTestCasesFromFile(String testId, String filename, Properties gpProperties) {
        List<TestData> testCases = new ArrayList<TestData>();
        File configurationFile = FileUtil.getSourceFile(GetCommandLineTest.class, filename);

        Reader reader = null;
        try {
            reader = new FileReader(configurationFile);
        }
        catch (FileNotFoundException e) {
            Assert.fail("Error reading test cases from file: "+configurationFile.getAbsolutePath());
        }
        
        Map<?,?> testCasesMap = null;
        try {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(reader);
            if (obj instanceof Map<?,?>) {
                testCasesMap = (Map<?,?>) obj;
            }
        }
        catch (Throwable t) {
            System.err.println("Error loading testData from file "+filename+": "+t.getLocalizedMessage());
        }
        if (testCasesMap == null) {
            return testCases;
        }
        for(Entry<?,?> entry : testCasesMap.entrySet()) {
            String testName = entry.getKey().toString();
            Object val = entry.getValue();
            try {
                TestData next = new TestData(testId+""+testName, gpProperties, val);
                testCases.add(next);
            }
            catch (Throwable t) {
                System.err.println("Error loading testData for "+testName+": "+t.getLocalizedMessage());
            }
        }
        return testCases;
    }

    public GetCommandLineTest(TestData testCaseData) {
        this.name = testCaseData.name;
        this.cmdLine = testCaseData.cmdLine;
        this.env = testCaseData.env;
        this.parameterInfoMap = testCaseData.parameterInfoMap;
        this.expected = testCaseData.expected;
    }
    
    private final String name;
    private final String cmdLine;
    private final Map<String,String> env;
    private final Map<String,ParameterInfo> parameterInfoMap;
    private final List<String> expected;
    
    private boolean strequals(String a, String b) {
        if (a == null) {
            return b==null;
        }
        return a.equals(b);
    }

    @Test
    public void testTranslateCmdLine() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperties(env)
        .build();
        GpContext gpContext=GpContext.getServerContext();
        List<String> cmdLineArgs = CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine, parameterInfoMap);
        Assert.assertNotNull(name+": cmdLineArgs", cmdLineArgs);
        Assert.assertEquals(name+": cmdLineArgs.size", expected.size(), cmdLineArgs.size());
        int i=0;
        for(String actual_arg : cmdLineArgs) {
            String expected_arg = expected.get(i);
            
            boolean match = strequals(expected_arg, actual_arg);
            if (match) {
                return;
            }
            
            //special-cases
            //null-check
            if (expected_arg == null) {
                Assert.fail(name+": cmdLineArg["+i+"]: Expecting a null value");
            }
            
            //special-case for libdir path
            if (expected_arg.contains("/taskLib/")) {
                int a0 = expected_arg.indexOf("/taskLib/") + "/taskLib/".length();
                int a2 = expected_arg.indexOf("/", a0);
                if (a2 > 0) {
                    int a1 = expected_arg.lastIndexOf(".", a2);
                    if (a1 >= 0) {
                        expected_arg = expected_arg.substring(0, a1) + expected_arg.substring(a2);
                    }
                }
            }
            
            //ignore <java_flags>
            if ("-Xmx512M".equals(actual_arg)) {
                //the actual arg is hard-coded to '-Xmx512M'
                expected_arg = "-Xmx512M";
            }
            Assert.assertEquals(name+": cmdLineArg["+i+"]", expected_arg, actual_arg);                
            ++i;
        }
    }
    
    private String hackFilterArg(String expected, String actual) {
        //ignore <java_flags>
        if (actual.equals("-Xmx512M")) {
            return "-Xmx512M";
        }
        
        return expected;
    }

}
