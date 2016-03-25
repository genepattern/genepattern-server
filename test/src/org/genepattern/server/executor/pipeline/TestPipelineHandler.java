package org.genepattern.server.executor.pipeline;

import static org.junit.Assert.assertEquals;

import org.genepattern.webservice.ParameterInfo;
import org.junit.Test;

public class TestPipelineHandler {
    /**
     * Test-cases for PipelineHandler, encoding of inherited file parameters
     * Example input and converted output
     *     1880/all_aml_train.cvt.gct -> <GenePatternURL>jobResults/1880/all_aml_train.cvt.gct
     *     1882/file path.txt         -> <GenePatternURL>jobResults/1882/file%20path.txt
     *     1881?scatter&filter=*      -> <GenePatternURL>jobResults/1881?scatter&filter=*
     * 
     * Note: these use-cases are not (yet) covered by junit tests.
     * 
     * @param paramValue
     */
    protected void doEncodedValueTest(final String paramValue) {
        doEncodedValueTest(paramValue, paramValue);
    }
    protected void doEncodedValueTest(final String expected, final String paramValue) {
        ParameterInfo inheritedFile=new ParameterInfo();
        inheritedFile.setValue(paramValue);
        assertEquals(expected, 
                PipelineHandler.getEncodedValue(inheritedFile));
    }

    @Test
    public void encodedValue() {
        doEncodedValueTest("1880/all_aml_train.cvt.gct");
    }

    @Test
    public void encodedValue_with_spaces() {
        doEncodedValueTest("1880/all%20aml%20train.cvt.gct", "1880/all aml train.cvt.gct");
    }

    @Test
    public void encodedValue_with_other_special_chars() {
        doEncodedValueTest("1880/all%20%5B%20aml%20test.gct", "1880/all [ aml test.gct");
    }

    @Test
    public void encodedValue_subDirectory() {
        doEncodedValueTest("1880/sub/all_aml_train.cvt.gct");
    }

    @Test
    public void encodedValue_nullValue() {
        doEncodedValueTest(null);
    }
    
}
