/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.util.GPConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
public class CommandLineParserTest {
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    private File rootTasklibDir;
    
    @Before
    public void setUp() throws IOException {
        rootTasklibDir=tmp.newFolder("taskLib");
    }

    @Test
    public void getSubstitutionParams_emptyString() {
       List<String> actual = CommandLineParser.getSubstitutionParameters(""); 
       assertEquals(Arrays.asList(), actual);
    }
    
    @Test
    public void getSubstitutionParams_javaCmd() {
        assertEquals(
                // expected
                Arrays.asList( "<java>", "<libdir>", "<p1>", "<p2>" ),
                // actual
                CommandLineParser.getSubstitutionParameters("<java> -cp <libdir>test.jar <p1> <p2>"));
    }

    @Test
    public void getSubstitutionParams_javaCmdInQuotes() {
        assertEquals(
                // expected
                Arrays.asList( "<java>", "<libdir>", "<p1>", "<p2>" ),
                // actual
                CommandLineParser.getSubstitutionParameters("\"<java>\" -cp <libdir>test.jar <p1> <p2>"));
    }

    @Test
    public void getSubstitutionParams_redirectToStdout() {
        assertEquals(
                // expected
                Arrays.asList( "<R2.5>", "<p1>", "<p2>" ),
                // actual
                CommandLineParser.getSubstitutionParameters("<R2.5> <p1> <p2> >> stdout.txt"));
    }

    @Test
    public void getSubstitutionParams_redirectToStdinAndStdout() {
        assertEquals(
                // expected
                Arrays.asList( "<R2.5>", "<p1>", "<p2>", "<stdout.file>", "<stdin.file>" ),
                // actual
                CommandLineParser.getSubstitutionParameters("<R2.5> <p1> <p2> >><stdout.file> <<<stdin.file>"));
    }

    @Test
    public void getSubstitutionParams_spaceInParamName() {
        assertEquals(
                // expected
                Arrays.asList( ),
                // actual
                CommandLineParser.getSubstitutionParameters("<a b>"));
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

        File configurationFile = FileUtil.getSourceFile(this.getClass(), filename);
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

    @Test
    public void getCommandLineFromFile() {
        String filename = "test_cases.yaml";
        testGetCommandLineFromFile(filename);
    }
    
    // not yet implemented
    @Ignore @Test
    public void inputFileBasename() {
        final File libdir=new File(rootTasklibDir, "MockModule.1.1000");
        final String cmdLine="echo basename=<input.file_basename> -o<input.file_basename>.cvt.<input.file_extension>";

        final GpConfig gpConfig=new GpConfig.Builder()
            // requires an actual config_yaml file in order to mock loading values from the gpContext.jobInput instance
            .configFile(new File("resources/config_local_job_runner.yaml"))
        .build();

        final JobInput jobInput=new JobInput();
        jobInput.addValue("input.file", "/path/to/test celfiles.zip");

        GpContext gpContext=new GpContext.Builder()
            .taskLibDir(libdir)
            .jobInput(jobInput)
        .build();
        assertEquals(
                // expected
                Arrays.asList("echo", "basename=test celfiles", "-otest celfiles.cvt.zip"), 
                // actual
                CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine));
    }
    
    @Test
    public void getCommandLine() throws IOException { 
        final File libdir=new File(rootTasklibDir, "ExpressionFileCreator.7.2190");
        //test case for expression file creator
        final String cmdLine="<R2.5> <libdir>expr.R parseCmdLine "+
            "-i<input.file> -o<output.file> "+
            "-m<method> -q<quantile.normalization> -b<background.correct> -c<compute.present.absent.calls> "+
            "-n<normalization.method> -l<libdir> -f<clm.file> -v<value.to.scale.to> -e<cdf.file> -a<annotate.probes>";
        
        final GpConfig gpConfig=new GpConfig.Builder()
            // requires an actual config_yaml file in order to mock loading values from the gpContext.jobInput instance
            .configFile(new File("resources/config_local_job_runner.yaml"))
            //from genepattern.properties
            .addProperty("R2.5", "<java> -DR_suppress=<R.suppress.messages.file> -DR_HOME=<R2.5_HOME> -Dr_flags=<r_flags> -cp <run_r_path> RunR")
            .addProperty("java",  "/usr/bin/java")
            .addProperty("r_flags", "--no-save --quiet --slave --no-restore")
            .addProperty("run_r_path", "/Broad/Applications/gp-3.2-dev/GenePatternServer/Tomcat/webapps/gp/WEB-INF/classes/")
            .addProperty("R2.5_HOME", "/Library/Frameworks/R.framework/Versions/2.5/Resources")
            .addProperty("R.suppress.messages.file", "/Broad/Applications/gp-3.2-dev/GenePatternServer/resources/R_suppress.txt")
            //initialized by GPAT#setupProps
            .addProperty("input.file" + GPConstants.INPUT_BASENAME, "test celfiles")
        .build();

        final JobInput jobInput=new JobInput();
        jobInput.addValue("input.file", "/path/to/test celfiles.zip");
        jobInput.addValue("output.file", "<input.file_basename>");
        jobInput.addValue("method", "RMA");
        jobInput.addValue("quantile.normalization", "yes");
        jobInput.addValue("background.correct", "no");
        jobInput.addValue("compute.present.absent.calls", "yes");
        jobInput.addValue("normalization.method", "median scaling");
        jobInput.addValue("clm.file", "");
        jobInput.addValue("value.to.scale.to", "");
        jobInput.addValue("cdf.file", "");
        jobInput.addValue("annotate.probes", "yes");

        String[] expected = {
                "/usr/bin/java", 
                "-DR_suppress=/Broad/Applications/gp-3.2-dev/GenePatternServer/resources/R_suppress.txt", 
                "-DR_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources",
                "-Dr_flags=--no-save --quiet --slave --no-restore", 
                "-cp", 
                "/Broad/Applications/gp-3.2-dev/GenePatternServer/Tomcat/webapps/gp/WEB-INF/classes/", 
                "RunR",
                new File(libdir,"expr.R").toString(), 
                "parseCmdLine",
                "-i/path/to/test celfiles.zip",
                "-otest celfiles",
                "-mRMA", 
                "-qyes", 
                "-bno", 
                "-cyes",
                "-nmedian scaling", 
                "-l"+libdir+File.separator,
                "-f",
                "-v", 
                "-e",
                "-ayes" };
        
        GpContext gpContext=new GpContext.Builder()
            .taskLibDir(libdir)
            .jobInput(jobInput)
        .build();
        List<String> cmdLineArgs = CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
        
        assertNotNull(cmdLineArgs);
        assertEquals("cmdLineArgs.size", expected.length, cmdLineArgs.size());
        int i=0;
        for(String arg : cmdLineArgs) {
            assertEquals("cmdLineArg["+i+"]", expected[i], arg);
            ++i;
        }
    }

    @Test
    public void scriptureCmdLine() {
        final File libdir=new File(rootTasklibDir, "Scripture.0.1234");
        final String cmdLine="<perl> <libdir>Scripture_wrapper.pl -gplibdir <libdir> -java <java> -javaflags '<java_flags>' -alignment <alignment.file> -out <output.filename> -sizeFile <chromosome.size.file> -chr <chromosome> -chrSequence <chromosome.sequence.file>"; 
        final GpConfig gpConfig=new GpConfig.Builder()
            .configFile(new File("resources/config_local_job_runner.yaml"))
            .addProperty("perl", "/usr/bin/perl")
            .addProperty("java", "C:\\Program Files\\GenePatternServer\\jre\\bin\\java")
            .addProperty("java_flags", "-Djava.awt.headless=true")
        .build();

        final JobInput jobInput=new JobInput();
        jobInput.addValue("alignment.file", "alignment_file");
        jobInput.addValue("output.filename", "a.out");
        jobInput.addValue("chromosome.size.file", "chr.sz.file");
        jobInput.addValue("chromosome", "chr1");
        jobInput.addValue("chromosome.sequence.file", "chr.seq.file");

        GpContext gpContext=new GpContext.Builder()
            .taskLibDir(libdir)
            .jobInput(jobInput)
        .build();

        List<String> expected = Arrays.asList(
                "/usr/bin/perl", new File(libdir,"Scripture_wrapper.pl").toString(), 
                "-gplibdir", libdir.toString()+File.separator,
                "-java", "C:\\Program Files\\GenePatternServer\\jre\\bin\\java",
                "-javaflags", "'-Djava.awt.headless=true'",
                "-alignment", "alignment_file",
                "-out", "a.out",
                "-sizeFile", "chr.sz.file",
                "-chr", "chr1",
                "-chrSequence", "chr.seq.file" 
                );
        
        List<String> actual=CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
        assertEquals(expected, actual);
    }
    
    @Test
    public void abyssCmdLine() {
        final File libdir=new File(rootTasklibDir, "AByss.0.1234");
        final String cmdLine="<perl> <libdir>abyss_wrapper.pl -gplibdir <libdir> -name <output.prefix> -in <input.file> -lib <lib.file> -se <se.file> -k <kmer.size> -n <minimum.pairs> -c <kmer.coverage.threshold> -b <bubble.threshold> -s <min.seed.contig.length> -j <num.threads> -cs <convert.contigs>";
        
        final GpConfig gpConfig=new GpConfig.Builder()
            .configFile(new File("resources/config_local_job_runner.yaml"))
            .addProperty("perl", "/usr/bin/perl")
        .build();

        final JobInput jobInput=new JobInput();
        jobInput.addValue("output.prefix", "prefix_");
        jobInput.addValue("input.file", "a.in");
        jobInput.addValue("lib.file", "a.lib");
        jobInput.addValue("se.file", "a.se");
        jobInput.addValue("kmer.size", "128");
        jobInput.addValue("minimum.pairs", "128");
        jobInput.addValue("kmer.coverage.threshold", "-1000");
        jobInput.addValue("bubble.threshold", "-1000");
        jobInput.addValue("min.seed.contig.length", "0");
        jobInput.addValue("num.threads", "8");
        jobInput.addValue("convert.contigs", "<convert.contigs>");

        final GpContext gpContext=new GpContext.Builder()
            .taskLibDir(libdir)
            .jobInput(jobInput)
        .build();

        final List<String> expected=Arrays.asList(
                "/usr/bin/perl", new File(libdir,"abyss_wrapper.pl").toString(),
                "-gplibdir", libdir.toString()+File.separator,
                "-name", "prefix_", "-in", "a.in",
                "-lib", "a.lib",
                "-se", "a.se",
                "-k", "128",
                "-n", "128",
                "-c", "-1000",
                "-b", "-1000", 
                "-s", "0",
                "-j", "8",
                "-cs", "<convert.contigs>" );
        
        List<String> actual=CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
        assertEquals(expected, actual);
    }

    @Test
    public void velvetCmdLine() {
        final String cmdLine="<perl> <velvet.home>/contrib/VelvetOptimiser-2.1.0/VelvetOptimiser.pl -f <reads> -s <min.kmer> -e <max.kmer>";
        final GpConfig gpConfig=new GpConfig.Builder()
            .configFile(new File("resources/config_local_job_runner.yaml"))
            .addProperty("perl", "/usr/bin/perl")
            .addProperty("velvet.home", "/usr/local/velvet_0.7.61")
        .build();

        final JobInput jobInput=new JobInput();
        jobInput.addValue("reads", "/usr/shared_data/example.fa");
        jobInput.addValue("min.kmer", "17");
        jobInput.addValue("max.kmer", "21");
        final GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        
        final List<String> expected=Arrays.asList(
                "/usr/bin/perl", "/usr/local/velvet_0.7.61/contrib/VelvetOptimiser-2.1.0/VelvetOptimiser.pl", 
                "-f", "/usr/shared_data/example.fa", "-s", "17",
                "-e", "21" );
        List<String> actual=CommandLineParser.translateCmdLine(gpConfig, gpContext, cmdLine);
        assertEquals(expected, actual);
    }

}
