package org.genepattern.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import junit.framework.TestCase;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.util.GPConstants;
import org.yaml.snakeyaml.Yaml;

/**
 * Unit tests for command line parser.
 * 
 * Notes:
 *     @cmdLine, command line from module manifest
 *     @serverProperties, properties loaded from genepattern.properties, custom.properties, and system.properties
 *     @userValues, values set by user via Run Module page or SOAP request
 *     List<String> parseCmdLine(String cmdLine, Map<String,?> serverProperties, Map<String,?> userValues);
 *     May need to get the ParameterInfo[] or TaskInfo to resolve substitutions which refer back to the module.
 *     E.g. <output.filename>=<input.file_basename>.out
 * 
 * @author pcarr
 */
public class CommandLineParserTest extends TestCase {
    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    private static File getSourceDir() {
        String cname = CommandLineParserTest.class.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }

    private static final String[][] substitutionsTestCases = {
        {"", },
        {"<java> -cp <libdir>test.jar <p1> <p2>",
            "<java>", "<libdir>", "<p1>", "<p2>" }, 
        {"\"<java>\" -cp <libdir>test.jar <p1> <p2>",
            "<java>", "<libdir>", "<p1>", "<p2>" }, 
        {"<R2.5> <p1> <p2> >> stdout.txt",
            "<R2.5>", "<p1>", "<p2>" },
        {"<R2.5> <p1> <p2> >><stdout.file> <<<stdin.file>",
            "<R2.5>", "<p1>", "<p2>", "<stdout.file>", "<stdin.file>" },  
        {"<a b>", }, 
    };
    
    public void testGetSubstitutions() {
        int i=0;
        for(String[] testCase : substitutionsTestCases) {
            testGetSubstitutions(i,testCase);
            ++i;
        }
    }
    
    private void testGetSubstitutions(int testCaseIdx, String[] testCase) {
        List<String> rval = CommandLineParser.getSubstitutionParameters(testCase[0]);
        int numExpected = testCase.length - 1;
        assertNotNull(rval);
        assertEquals("testCase["+testCaseIdx+"] num subs", numExpected, rval.size());
        for(int i=0; i<rval.size(); ++i) {
            assertEquals("sub["+i+"]", testCase[i+1], rval.get(i));
        }
    }
    
    //command line tests
    private static class CmdLineObj {
        private String name = "";
        private String description = "";
        private String cmdLine = "";
        private List<String> expected;
        private Map<String,String> env;
        
        /**
         * Initialize a test-case from the test_cases.yaml file.
         * @param map, loaded from yaml parser.
         */
        public CmdLineObj(String name, Object obj) {
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
                    fail("Error parsing config file for test-case: "+name+". Description is not a String.");
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
        
        public void runTest() {
            Properties props=new Properties();
            for(final Entry<String,String> entry : env.entrySet()) {
                props.put(entry.getKey(), entry.getValue());
            }
            GpConfig gpConfig=new GpConfig.Builder()
                .addProperties(props)
            .build();
            
            GpContext gpContext=GpContext.getServerContext();
            if (props.containsKey("libdir")) {
                gpContext.setTaskLibDir(new File(props.getProperty("libdir")));
            }

            List<String> cmdLineArgs = CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
            assertNotNull(name+": cmdLineArgs", cmdLineArgs);
            assertEquals(name+": cmdLineArgs.size", expected.size(), cmdLineArgs.size());
            //assertEquals(name+": cmdLineArgs.size", expected.size(), cmdLineArgs.length);
            int i=0;
            for(String arg : cmdLineArgs) {
                assertEquals(name+": cmdLineArg["+i+"]", expected.get(i), arg);
                ++i;
            }
        }
    }
    
    private List<CmdLineObj> loadTestCases(String filename) throws FileNotFoundException { 
        List<CmdLineObj> testCases = new ArrayList<CmdLineObj>();

        File parentDir = getSourceDir();
        File configurationFile = new File(parentDir, filename);
        Reader reader = null;
        reader = new FileReader(configurationFile);

        Yaml yaml = new Yaml();
        Object obj = yaml.load(reader);
        if (obj instanceof Map<?,?>) {
            Map<?,?> testCasesMap = (Map<?,?>) obj;
            for(Entry<?,?> entry : testCasesMap.entrySet()) {
                String testName = entry.getKey().toString();
                Object val = entry.getValue();
                
                CmdLineObj next = new CmdLineObj(testName, val);
                testCases.add(next);
            }
        }
        
        return testCases;

    }
    
    private void testGetCommandLineFromFile(String filename) {
        List<CmdLineObj> testCases = null;
        try {
            testCases = loadTestCases(filename);
        }
        catch (FileNotFoundException e) {
            fail(e.getLocalizedMessage());
        }
        
        for(CmdLineObj testCase : testCases) {
            testCase.runTest();
        }
        
    }

    public void testGetCommandLineFromFile() {
        String filename = "test_cases.yaml";
        testGetCommandLineFromFile(filename);
    }
    
    public void testRnaSeqCmdLines() {
        String filename = "rna_seq_test_cases.yaml";
        testGetCommandLineFromFile(filename);
    }
    
    public void testPathSeqCmdLines() {
        String filename = "pathseq_test_cases.yaml";
        testGetCommandLineFromFile(filename);
    }

    public void testGetCommandLine() throws IOException { 
        //test case for expression file creator
        String cmdLine="<R2.5> <libdir>expr.R parseCmdLine "+
            "-i<input.file> -o<output.file> "+
            "-m<method> -q<quantile.normalization> -b<background.correct> -c<compute.present.absent.calls> "+
            "-n<normalization.method> -l<libdir> -f<clm.file> -v<value.to.scale.to> -e<cdf.file> -a<annotate.probes>";
        
        Map<String,String> dict = new LinkedHashMap<String,String>();
        //from genepattern.properties
        dict.put("R2.5", "<java> -DR_suppress=<R.suppress.messages.file> -DR_HOME=<R2.5_HOME> -Dr_flags=<r_flags> -cp <run_r_path> RunR");
        dict.put("java",  "/usr/bin/java");
        dict.put("r_flags", "--no-save --quiet --slave --no-restore");
        dict.put("run_r_path", "/Broad/Applications/gp-3.2-dev/GenePatternServer/Tomcat/webapps/gp/WEB-INF/classes/");
        dict.put("R2.5_HOME", "/Library/Frameworks/R.framework/Versions/2.5/Resources");
        dict.put("R.suppress.messages.file", "/Broad/Applications/gp-3.2-dev/GenePatternServer/resources/R_suppress.txt");
        
        //set for a particular job
        dict.put("libdir", "../taskLib/ExpressionFileCreator.7.2190/");
        dict.put("input.file", "/path/to/test celfiles.zip");
        dict.put("output.file", "<input.file_basename>");
        dict.put("method", "RMA");
        dict.put("quantile.normalization", "yes");
        dict.put("background.correct", "no");
        dict.put("compute.present.absent.calls", "yes");
        dict.put("normalization.method", "median scaling");
        dict.put("clm.file", "");
        dict.put("value.to.scale.to", "");
        dict.put("cdf.file", "");
        dict.put("annotate.probes", "yes");
        
        //initialized by GPAT#setupProps
        dict.put("input.file" + GPConstants.INPUT_BASENAME, "test celfiles");

        String[] expected = {
                "/usr/bin/java", 
                "-DR_suppress=/Broad/Applications/gp-3.2-dev/GenePatternServer/resources/R_suppress.txt", 
                "-DR_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources",
                "-Dr_flags=--no-save --quiet --slave --no-restore", 
                "-cp", 
                "/Broad/Applications/gp-3.2-dev/GenePatternServer/Tomcat/webapps/gp/WEB-INF/classes/", 
                "RunR",
                "../taskLib/ExpressionFileCreator.7.2190/expr.R", 
                "parseCmdLine",
                "-i/path/to/test celfiles.zip",
                "-otest celfiles",
                "-mRMA", 
                "-qyes", 
                "-bno", 
                "-cyes",
                "-nmedian scaling", 
                "-l../taskLib/ExpressionFileCreator.7.2190/",
                "-f",
                "-v", 
                "-e",
                "-ayes" };
        
        GpConfig gpConfig=new GpConfig.Builder().addProperties(dict).build();
        GpContext gpContext=GpContext.getServerContext();
        List<String> cmdLineArgs = CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
        
        assertNotNull(cmdLineArgs);
        assertEquals("cmdLineArgs.size", expected.length, cmdLineArgs.size());
        int i=0;
        for(String arg : cmdLineArgs) {
            assertEquals("cmdLineArg["+i+"]", expected[i], arg);
            ++i;
        }
        
    }

}
