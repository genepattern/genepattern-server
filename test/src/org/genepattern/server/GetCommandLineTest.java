package org.genepattern.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.util.LabelledParameterized;
import org.genepattern.util.TestUtil;
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
@RunWith(LabelledParameterized.class)
public class GetCommandLineTest {
    private static String[] testCaseFiles={ "test_cases.yaml", "rna_seq_test_cases.yaml" };

    /**
     * This parameterized test runs a single unit test for each test case in the Collection of TestData.
     * @return
     */
    @Parameters
    public static Collection<TestData[]> data() {
        List<TestData> testCases = new ArrayList<TestData>();
        for(String testCaseFile : testCaseFiles) {
            testCases.addAll( loadTestCasesFromFile(testCaseFile) );
        }
        
        Collection<TestData[]> rval = new ArrayList<TestData[]>();
        for(TestData testCase : testCases) {
            rval.add( new TestData[] { testCase } );
        }
        return rval;
    }

    /**
     * Helper class for representing a single test case loaded from the (yaml) configuration file.
     */
    private static class TestData {
        private String name = "";
        private String description = "";
        private String cmdLine = "";
        private Map<String,String> env;
        private List<String> expected;
        
        /**
         * Initialize a test-case from the test_cases.yaml file.
         * @param map, loaded from yaml parser.
         */
        public TestData(final String name, final Object obj) {
            this.name = name;
            
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
        }
        
        private void setCmdLine(Object obj) {
            cmdLine = obj.toString();
        }
        private void setEnv(Object obj) {
            env = new LinkedHashMap<String,String>();
            if (obj instanceof Map<?,?>) {
                for(Entry<?,?> entry : ((Map<?,?>)obj).entrySet() ) {
                    env.put( entry.getKey().toString(), entry.getValue().toString());
                }
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

    private static List<TestData> loadTestCasesFromFile(String filename) {
        List<TestData> testCases = new ArrayList<TestData>();
        File parentDir = TestUtil.getSourceDir();
        File configurationFile = new File(parentDir, filename);
        Reader reader = null;
        try {
            reader = new FileReader(configurationFile);
        }
        catch (FileNotFoundException e) {
            Assert.fail("Error reading test cases from file: "+configurationFile.getAbsolutePath());
        }
        Yaml yaml = new Yaml();
        Object obj = yaml.load(reader);
        if (obj instanceof Map<?,?>) {
            Map<?,?> testCasesMap = (Map<?,?>) obj;
            for(Entry<?,?> entry : testCasesMap.entrySet()) {
                String testName = entry.getKey().toString();
                Object val = entry.getValue();
                
                TestData next = new TestData(testName, val);
                testCases.add(next);
            }
        }
        return testCases;
    }

    public GetCommandLineTest(TestData testCaseData) {
        this.name = testCaseData.name;
        this.cmdLine = testCaseData.cmdLine;
        this.env = testCaseData.env;
        this.expected = testCaseData.expected;
    }
    
    private final String name;
    private final String cmdLine;
    private final Map<String,String> env;
    private final List<String> expected;

    @Test
    public void testTranslateCmdLine() {
        List<String> cmdLineArgs = CommandLineParser.translateCmdLine(cmdLine, env);
        Assert.assertNotNull(name+": cmdLineArgs", cmdLineArgs);
        Assert.assertEquals(name+": cmdLineArgs.size", expected.size(), cmdLineArgs.size());
        int i=0;
        for(String arg : cmdLineArgs) {
            Assert.assertEquals(name+": cmdLineArg["+i+"]", expected.get(i), arg);
            ++i;
        }
    }

}
