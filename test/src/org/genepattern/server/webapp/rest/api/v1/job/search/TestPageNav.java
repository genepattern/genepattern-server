/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * junit tests for the PageNav class.
 * @author pcarr
 *
 */
public class TestPageNav {
    private static final String jobsResourcePath="http://127.0.0.1:8080/gp/rest/v1/jobs";
    private static final boolean initIsAdmin=false;
    private static GpConfig gpConfig;
    private static final GpContext userContext=GpContext.getContextForUser("test", initIsAdmin);
    
    @BeforeClass
    public static void beforeClass() {
        gpConfig=new GpConfig.Builder()
            .build();
    }
    
    @Test
    public void testDefaultSearch() {
        int numItems=20;
        SearchQuery q=new SearchQuery.Builder(gpConfig, userContext, jobsResourcePath)
            .pageNum(1)
            .pageSize(20)
        .build();
        PageNav nav=new PageNav(q, numItems);
        Assert.assertEquals("current page", 1, nav.getCurrent().getPage());
        
    }
    
    /**
     * Test page nav with page > 1.
     */
    @Test
    public void testPageNavWithRanges() {
        int numItems=405;
        SearchQuery q=new SearchQuery.Builder(gpConfig, userContext, jobsResourcePath)
            .pageNum(22)
            .pageSize(10)
        .build();
        PageNav nav=new PageNav(q, numItems);

        Assert.assertEquals("numPages", 41, nav.getNumPages());
        Assert.assertEquals("numItems", 405, nav.getNumItems());
        //prev
        Assert.assertNotNull("expecting non-null prev", nav.getPrev());
        Assert.assertEquals("prev.name", "Previous", nav.getPrev().getName());
        Assert.assertEquals("prev.page", 21, nav.getPrev().getPage());
        Assert.assertEquals("prev.rel", Rel.prev, nav.getPrev().getRel());
        Assert.assertEquals("prev.href", jobsResourcePath+"?page=21", nav.getPrev().getHref());
        
        //next
        Assert.assertNotNull("expecting non-null next", nav.getNext());
        Assert.assertEquals("next.name", "Next", nav.getNext().getName());
        Assert.assertEquals("next.page", 23, nav.getNext().getPage());
        Assert.assertEquals("next.rel", Rel.next, nav.getNext().getRel());
        Assert.assertEquals("next.href", jobsResourcePath+"?page=23", nav.getNext().getHref());
        
        //first
        final PageLink first=nav.getFirst();
        Assert.assertEquals("first.name", "1", first.getName());
        Assert.assertEquals("first.page", 1, first.getPage());
        Assert.assertEquals("fitst.rel", Rel.first, first.getRel());
        Assert.assertEquals("first.href", jobsResourcePath+"?page=1", first.getHref());
        
        //last
        final PageLink last=nav.getLast();
        Assert.assertEquals("last.name", "41", last.getName());
        Assert.assertEquals("last.page", 41, last.getPage());
        Assert.assertEquals("last.rel", Rel.last, last.getRel());
        Assert.assertEquals("last.href", jobsResourcePath+"?page=41", last.getHref());
        
        //current
        final PageLink cur=nav.getCurrent();
        Assert.assertEquals("cur.name", "22", cur.getName());
        Assert.assertEquals("cur.page", 22, cur.getPage());
        Assert.assertEquals("cur.rel", Rel.self, cur.getRel());
        Assert.assertEquals("cur.href", jobsResourcePath+"?page=22", cur.getHref());
        
        //prev ranges
        Assert.assertEquals("prevRange.size", 14, nav.getPrevRange().size());
        Assert.assertEquals("prevRange.startPage", 8, nav.getPrevRange().get(0).getPage());
        Assert.assertEquals("prevRange.endPage", 21, nav.getPrevRange().get(13).getPage());
        
        //next ranges
        Assert.assertEquals("nextRange.size", 12, nav.getNextRange().size());
        Assert.assertEquals("nextRange.startPage", 23, nav.getNextRange().get(0).getPage());
        Assert.assertEquals("nextRange.endPage", 34, nav.getNextRange().get(11).getPage());
    }

}
