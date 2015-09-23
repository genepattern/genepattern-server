package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * test cases for the CmdLineSubToken class
 * @author pcarr
 *
 */
public class TestCmdLineSubToken {
    @Test
    public void testSub_nullArg() {
        final String arg=null;
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(), 
                CmdLineSubToken.splitIntoSubTokens(arg));
    }

    @Test
    public void testSub_emptyArg() {
        // test 1: empty string
        final String arg="";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(), 
                CmdLineSubToken.splitIntoSubTokens(arg));
    }
    
    @Test
    public void testSub_literal() {
        // test 3: literal
        final String arg="this is a value";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(new CmdLineSubToken("this is a value")), 
                CmdLineSubToken.splitIntoSubTokens(arg));
        assertEquals("Sub.sub('"+arg+"')[0].pname", 
                null, 
                CmdLineSubToken.splitIntoSubTokens(arg).get(0).pname);
        
    }
    
    @Test
    public void testSub_match() {
        final String arg="<input.param>";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(new CmdLineSubToken("<input.param>")), 
                CmdLineSubToken.splitIntoSubTokens(arg));
        assertEquals("Sub.sub('"+arg+"')[0].pname", 
                "input.param", 
                CmdLineSubToken.splitIntoSubTokens(arg).get(0).pname);
    }

    @Test
    public void testSub_match_with_prefix() {
        final String arg="PREFIX <input.param>";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(new CmdLineSubToken("PREFIX "), new CmdLineSubToken("<input.param>")), 
                CmdLineSubToken.splitIntoSubTokens(arg));
        assertEquals("Sub.sub('"+arg+"')[1].pname", 
                "input.param", 
                CmdLineSubToken.splitIntoSubTokens(arg).get(1).pname);
    }

    @Test
    public void testSub_match_with_postfix() {
        final String arg="<input.param> POSTFIX";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(new CmdLineSubToken("<input.param>"), new CmdLineSubToken(" POSTFIX")), 
                CmdLineSubToken.splitIntoSubTokens(arg));
        assertEquals("Sub.sub('"+arg+"')[0].pname", 
                "input.param", 
                CmdLineSubToken.splitIntoSubTokens(arg).get(0).pname);
    }

    // use-case: default_value of 'output.filename' param uses standard _basename and _extension substitutions form 'input.file' param
    @Test
    public void testSub_match_multi() {
        final String arg="<input.file_basename>.cp.<input.file_extension>";
        assertEquals("Sub.sub('"+arg+"')", 
                Arrays.asList(new CmdLineSubToken("<input.file_basename>"), new CmdLineSubToken(".cp."), new CmdLineSubToken("<input.file_extension>")), 
                CmdLineSubToken.splitIntoSubTokens(arg));
        assertEquals("Sub.sub('"+arg+"')[0].pname", 
                "input.file_basename", 
                CmdLineSubToken.splitIntoSubTokens(arg).get(0).pname);
        assertEquals("Sub.sub('"+arg+"')[2].pname", 
                "input.file_extension", 
                CmdLineSubToken.splitIntoSubTokens(arg).get(2).pname);
    }
    
}
