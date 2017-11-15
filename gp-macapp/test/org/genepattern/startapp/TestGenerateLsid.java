package org.genepattern.startapp;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * jUnit tests for the GenerateLsid class
 * @author pcarr
 *
 */
public class TestGenerateLsid {
    @Test
    public void initLsid() {
        final String lsid=GenerateLsid.lsid();
        assertNotNull(lsid);
    }

}
