/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

import java.util.List;

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {

    public void testSplitCommandLineEmptyString() {
        List<String> rval = StringUtil.splitCommandLine("");
        assertNotNull("should return non-null", rval);
        assertEquals("should return zero length list", 0, rval.size());
    }
    
    public void testSplitCommandLineWithQuotedArg() {
        List<String> rval = StringUtil.splitCommandLine("lsf_wrapper.sh stdout.txt java -cp a.jar:b.jar:c.jar org.broadinstitute.test.MyTest \"Test One\"");
        
        //for debugging
        for(String token : rval) {
            System.out.println(token);
        }
        
        assertNotNull(rval);
        assertEquals(7, rval.size());
        assertEquals("Test One", rval.get(6));
    }
    
    public void testSplitCommandLineWithEscapedQuote() {
        List<String> rval = StringUtil.splitCommandLine("\"arg1 has an escaped quote (\\\") character\"");
        assertNotNull(rval);
        assertEquals(1, rval.size());
        assertEquals("arg1 has an escaped quote (\\\") character", rval.get(0));
    }

}
