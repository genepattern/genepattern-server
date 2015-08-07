/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * junit test cases for the Value class.
 * @author pcarr
 *
 */
public class TestValue {
    @Test
    public void joinSingleValue() {
        Value value=new Value("test");
        Assert.assertEquals("", "test",  value.join(null));
    }

    @Test
    public void joinListOfValues() {
        Value value=new Value(Arrays.asList("A", "B", "C"));
        Assert.assertEquals("A B C",  value.join());
    }

    @Test
    public void joinNullSep() {
        Value value=new Value(Arrays.asList("A", "B", "C"));
        Assert.assertEquals("A B C",  value.join(null));
    }

    @Test
    public void joinSpaceSep() {
        Value value=new Value(Arrays.asList("A", "B", "C"));
        Assert.assertEquals("A B C",  value.join(" "));
    }

    @Test
    public void joinCommaSep() {
        Value value=new Value(Arrays.asList("A", "B", "C"));
        Assert.assertEquals("A,B,C",  value.join(","));
    }
    
    @Test
    public void joinEmptyList() {
        List<String> values=Collections.emptyList();
        Value value=new Value(values);
        Assert.assertEquals("",  value.join(","));
    }
    
    @Test
    public void joinMap() {
        Map<String,Integer> values=new HashMap<String,Integer>();
        values.put("A", 1);
        values.put("B", 2);
        Value value=new Value(values);
        Assert.assertEquals("A=1&B=2", value.join("&"));
    }
    public void joinEmptyMap() {
        Map<String,Integer> values=new HashMap<String,Integer>();
        Value value=new Value(values);
        Assert.assertEquals("", value.join("&"));
    }

}
