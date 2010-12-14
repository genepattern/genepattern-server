package org.genepattern.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.genepattern.server.genepattern.CommandLineParser;
import org.genepattern.util.GPConstants;

/**
 * Unit tests for command line parser.
 * 
 * @author pcarr
 */
public class CommandLineParserTest extends TestCase {
    private static final String[][] testCases = {
        {"", },
        {"<java> -cp <libdir>test.jar <p1> <p2>",
            "<java>", "<libdir>", "<p1>", "<p2>" }, 
        {"\"<java>\" -cp <libdir>test.jar <p1> <p2>",
            "<java>", "<libdir>", "<p1>", "<p2>" }, 
        {"<R2.5> <p1> <p2> >> stdout.txt",
                "<R2.5>", "<p1>", "<p2>" },
    };
    
    public void testGetSubstitutions() {
        for(String[] testCase : testCases) {
            testGetSubstitutions(testCase);
        }
    }
    
    private void testGetSubstitutions(String[] testCase) {
        List<String> rval = CommandLineParser.getSubstitutionParameters(testCase[0]);
        int numExpected = testCase.length - 1;
        assertNotNull(rval);
        assertEquals("num subs", numExpected, rval.size());
        for(int i=0; i<rval.size(); ++i) {
            assertEquals("sub["+i+"]", testCase[i+1], rval.get(i));
        }
    }

    public void testGetCommandLine() { 
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
        
        List<String> cmdLineArgs = CommandLineParser.translateCmdLine(dict, cmdLine);
        assertNotNull(cmdLineArgs);
        assertEquals("cmdLineArgs.size", expected.length, cmdLineArgs.size());
        int i=0;
        for(String arg : cmdLineArgs) {
            assertEquals("cmdLineArg["+i+"]", expected[i], arg);
            ++i;
        }
        
    }

}
