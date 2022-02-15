/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.MockGpFilePath;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.webapp.rest.api.v1.job.search.OutputFileComparator.OrderFilesBy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for sorting job result files.
 * @author pcarr
 *
 */
public class TestOutputFileComparator {
    // list of output files
    private List<GpFilePath> outputFiles;
    
    @Before
    public void before() {
        outputFiles=new ArrayList<GpFilePath>();
        File cur=FileUtil.getSourceDir(this.getClass());
        File dir=new File(cur, "test_01");
        
        File[] files=dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File pathname) {
                if (pathname==null || pathname.getName().startsWith(".")) {
                    return false;
                }
                return true;
            }
        });
        for (final File file : files) {
            GpFilePath gpFilePath=new MockGpFilePath.Builder(file).build();
            outputFiles.add(gpFilePath);
        }
    }
    
    
    @Test
    public void testDefault() {
        final String orderFilesBy=null;
        Comparator<GpFilePath> c=new OutputFileComparator.Builder(orderFilesBy).build();
        Collections.sort(outputFiles, c);
        Assert.assertTrue(true);
    }
    
    @Test
    public void testBySize() {
        final String orderFilesBy=OrderFilesBy.size.name();
        Comparator<GpFilePath> c=new OutputFileComparator.Builder(orderFilesBy).build();
        Collections.sort(outputFiles, c);
        Assert.assertTrue(true);
    }

    @Test
    public void testBySizeDesc() {
        final String orderFilesBy="-"+OrderFilesBy.size.name();
        Comparator<GpFilePath> c=new OutputFileComparator.Builder(orderFilesBy).build();
        Collections.sort(outputFiles, c);
        Assert.assertTrue(true);
    }

    @Test
    public void testBySizeAsc() {
        final String orderFilesBy="+"+OrderFilesBy.size.name();
        Comparator<GpFilePath> c=new OutputFileComparator.Builder(orderFilesBy).build();
        Collections.sort(outputFiles, c);
        Assert.assertTrue(true);
    }
    
    @Test
    public void testByName() {
        final String orderFilesBy=OrderFilesBy.name.name();
        Comparator<GpFilePath> c=new OutputFileComparator.Builder(orderFilesBy).build();
        Collections.sort(outputFiles, c);
        Assert.assertTrue(true);
        Assert.assertEquals("by_name[2]", "c.txt", outputFiles.get(2).getName());
    }

}
