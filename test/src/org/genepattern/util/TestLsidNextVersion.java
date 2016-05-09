package org.genepattern.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestLsidNextVersion {

    @Parameters(name="version: \"{0}\" ")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // { initialVersion, expectedVersion }
                {"", "0"},
                {"0", "1"},
                {"1", "2"},
                {"9", "10"},
                {"10", "11" },
                // minor
                {"0.0", "0.1"}, 
                {"0.1", "0.2"}, 
                {"1.0", "1.1"}, 
                {"1.1", "1.2"}, 
                {"1.9", "1.10"}, 
                {"1.10", "1.11"}, 
                {"2.0", "2.1"}, 
                // patch
                {"0.0.0", "0.0.1"}, 
                {"0.0.1", "0.0.2"}, 
                {"0.0.9", "0.0.10"}, 
                {"0.0.10", "0.0.11"}, 
                {"0.0.11", "0.0.12"}, 

                {"0.1.0", "0.1.1"}, 

                {"1.0.0", "1.0.1"}, 
                
                // known issues
                // {null, "0" },   <--- throws NullPointerException
                // {"0.", "0.0"},  <--- converts to "01" rather than "0.0"
        });
    }
    
    @Parameter
    public String initialVersion;

    @Parameter(value = 1)
    public String expectedVersion;
    
    @Test
    public void incrementedMinorVersion() {
        assertEquals(
                "Increment: '"+initialVersion+"' -> '"+expectedVersion+"'", 
                // expected
                expectedVersion, 
                // actual
                LSID.getIncrementedMinorVersion(initialVersion));
    }

}
