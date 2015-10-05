package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;

public class TestQueryStringBuilder {
    private QueryStringBuilder q;
    
    @Before
    public void setUp() {
        q=new QueryStringBuilder();
    }
    
    @Test
    public void basicQueryString() throws UnsupportedEncodingException {
        final String queryString=
            q.param("A", "B")
        .build();
        assertEquals("A=B", queryString);
    }
    
    @Test
    public void encodeUrlValue() throws UnsupportedEncodingException {
        q.param("input.file", "http://127.0.0.1:8080/gp/getFile.jsp?task=urn:lsid:8080.pcarr.69.173.120.54:genepatternmodules:33:1&file=all_aml_train.gct");
        assertEquals("encoded GP file path", 
                // expected
                "input.file=http%3A%2F%2F127.0.0.1%3A8080%2Fgp%2FgetFile.jsp%3Ftask%3Durn%3Alsid%3A8080.pcarr.69.173.120.54%3Agenepatternmodules%3A33%3A1%26file%3Dall_aml_train.gct", 
                // actual
                q.build());
    }
    
    @Test
    public void multipleValues()  throws UnsupportedEncodingException {
        q.param("A", "arg1")
         .param("A", "arg2");
        
        assertEquals("multiple values",
                "A=arg1&A=arg2", q.build());
    }
    
    @Test
    public void noQueryString() {
        String queryString=new QueryStringBuilder().build();
        assertEquals("null means no queryString", null, queryString);
    }
    
    @Test
    public void nullValue() throws UnsupportedEncodingException {
        q.param("A");
        assertEquals("A", q.build());
    }

    @Test
    public void emptyValue() throws UnsupportedEncodingException {
        q.param("A", "");
        assertEquals("A=", q.build());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void nullName() throws UnsupportedEncodingException {
        q.param(null, "value");
    }
}
