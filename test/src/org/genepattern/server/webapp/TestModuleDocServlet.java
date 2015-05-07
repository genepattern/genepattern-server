/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestModuleDocServlet {
    private HttpServletRequest req;
    private final String name="ConvertLineEndings";
    private final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    
    @Before
    public void setUp() {
        req=mock(HttpServletRequest.class);
    }
    
    @Test
    public void splitPathInfo_noPath() {
        when(req.getPathInfo()).thenReturn("/"+lsid);
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{lsid, null}, actual);
    }

    @Test
    public void splitPathInfo_emptyPath() {
        when(req.getPathInfo()).thenReturn("/"+lsid+"/");
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{lsid, ""}, actual);
    }

    @Test
    public void splitPathInfo_withPath() {
        String filepath="index.html";
        when(req.getPathInfo()).thenReturn("/"+lsid+"/"+filepath);
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{lsid, filepath}, actual);
    }

    @Test
    public void splitPathInfo_withSubPath() {
        String filepath="images/img.png";
        when(req.getPathInfo()).thenReturn("/"+lsid+"/"+filepath);
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{lsid, filepath}, actual);
    }
    
    @Test
    public void splitPathInfo_withSubPath_taskName() {
        String filepath="images/img.png";
        when(req.getPathInfo()).thenReturn("/"+name+"/"+filepath);
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{name, filepath}, actual);
    }
    

    @Test
    public void splitPathInfo_empty() {
        when(req.getPathInfo()).thenReturn("");        
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{"", null}, actual);
    }

    @Test
    public void splitPathInfo_null() {
        when(req.getPathInfo()).thenReturn(null);
        
        String[] actual=ModuleDocServlet.splitPathInfo(req);
        Assert.assertArrayEquals("splitPathInfo", new String[]{"", null}, actual);
    }
    
    @Test
    public void sanitizePath() {
        String filename="index.html";
        assertEquals(filename, ModuleDocServlet.sanitizePath(filename));
    }

    @Test
    public void sanitizePath_subdir() {
        String filename="images/img.png";
        assertEquals(filename, ModuleDocServlet.sanitizePath(filename));
    }

    @Test
    public void sanitizePath_absolute() {
        String filename="/xchip/images/img.png";
        assertEquals("an absolute file gets truncated to its filename",
                "img.png", ModuleDocServlet.sanitizePath(filename));
    }
    
    @Test
    public void sanitizePath_parentDir() {
        String filename="../../resources/genepattern.properties";
        assertEquals(
                "a path to a parent dir is not allowed, it gets truncated to its filename",
                "genepattern.properties", ModuleDocServlet.sanitizePath(filename));
    }

    @Test
    public void sanitizePath_parentDir_hack() {
        String filename="help.txt/../../resources/genepattern.properties";
        assertEquals(
                "a path to a parent dir is not allowed, it gets truncated to its filename",
                "genepattern.properties", ModuleDocServlet.sanitizePath(filename));
    }

}
