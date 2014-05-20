package org.genepattern.server.dm;

import org.junit.Assert;
import org.junit.Test;


public class TestUrlUtil {

    @Test
    public void testDecode_noOp() {
        final String initialValue="all_aml_test.gct";
        Assert.assertEquals(
                initialValue,
                UrlUtil.decodeURIcomponent(initialValue) );
    }
    
    @Test
    public void testDecode_space() {
        Assert.assertEquals(
                "all aml test.gct",
                UrlUtil.decodeURIcomponent("all%20aml%20test.gct") );
    }

    @Test
    public void testDecode_slashes() {
        Assert.assertEquals(
                "sub/directory/all_aml_test.gct",
                UrlUtil.decodeURIcomponent("sub/directory/all_aml_test.gct") );
    }

}
