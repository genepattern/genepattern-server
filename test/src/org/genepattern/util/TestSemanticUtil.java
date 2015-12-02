package org.genepattern.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestSemanticUtil {
    protected void checkExtension(final String filename, final String expected_extension) {
        assertEquals("getExtension("+filename+")", expected_extension,
                SemanticUtil.getExtension(filename));
    }
    
    @Test
    public void getExtension() {
        checkExtension("filename.txt", "txt");
    }

    @Test
    public void getExtension_no_ext() {
        checkExtension("name_no_ext", "");
    }

    @Test
    public void getExtension_trailing_slash() {
        checkExtension("dirname/", "");
    }

    @Test
    public void getExtension_hidden() {
        checkExtension(".hidden.txt", "txt");
    }

    @Test
    public void getExtension_hidden_no_ext() {
        checkExtension(".hidden_no_ext", "");
    }

    @Test
    public void getExtension_null() {
        checkExtension(null, null);
    }
    

}
