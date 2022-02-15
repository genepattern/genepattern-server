/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;


public class TestNavigationToJson {
    private SearchQuery q;
    
    @Before
    public void setUp() {
        GpConfig gpConfig=new GpConfig.Builder().build();
        GpContext userContext=GpContext.getContextForUser("test_user");
        q = new SearchQuery.Builder(gpConfig, userContext, "http://127.0.0.1:8080/gp/rest/v1/jobs/").build();
    }
    
    @Test
    public void testBasicNavDetails_mock() throws JSONException {
        int pageNum=1;
        int totalNumResults=450;
        
        QueryLink filterLink1=Mockito.mock(QueryLink.class);
        when(filterLink1.toJson()).thenReturn(new JSONObject());
        
        
        PageNav pageNav=Mockito.mock(PageNav.class);
        Mockito.when(pageNav.getCurrent()).thenReturn(new PageLink(pageNum));
        Mockito.when(pageNav.getNumPages()).thenReturn(4);
        FilterNav filterNav=Mockito.mock(FilterNav.class);
        Mockito.when(filterNav.getBatchIds()).thenReturn( Arrays.asList("15", "16"));
        Mockito.when(filterNav.getGroupIds()).thenReturn( Arrays.asList("*", "admin"));
        Mockito.when(
                filterNav.getFilterLinks()).thenReturn( Arrays.asList( filterLink1, filterLink1 ));

        SearchResults results=new SearchResults(
                totalNumResults,
                new ArrayList<JobInfo>(),
                pageNav,
                filterNav,
                0
                );
        
        JSONObject nav=results.navigationDetailsToJson();
        
        /* manually compare JSON object, expected ...
           { 
             "filterLinks": [{},{}],
             "groupIds": ["*","admin"],
             "page": 1,
             "batchIds": ["15","16"],
             "numPages": 4,
             "numItems": 0
           }
         */
        assertEquals("nav.length", 6, nav.length());
        assertEquals("nav['filterLinks']", "[{},{}]", nav.getJSONArray("filterLinks").toString());
        assertEquals("nav['groupIds']", "[\"*\",\"admin\"]", nav.getJSONArray("groupIds").toString());
        assertEquals("nav['page']", "1", nav.getString("page"));
        assertEquals("nav['batchIds']", "[\"15\",\"16\"]", nav.getJSONArray("batchIds").toString());
        assertEquals("nav['numPages']", "4", nav.getString("numPages"));
        assertEquals("nav['numItems']", "0", nav.getString("numItems"));
     }
    
    @Test
    public void testBasicNavDetails_fluent() throws JSONException {
        SearchResults results=new SearchResults.Builder(q)
            .jobInfos(new ArrayList<JobInfo>())
        .build();
        JSONObject nav=results.navigationDetailsToJson();
        
        assertEquals("by default, the current page is", 
                1,
                nav.get("page")
                );
    }

    @Test
    public void testBatchIds() throws JSONException {
        List<JobInfo> jobInfos = new ArrayList<JobInfo>();
        SearchResults results=new SearchResults.Builder(q)
            .jobInfos(jobInfos)
            .batchIds(Arrays.asList("15", "16"))
            .groupIds(Arrays.asList("*", "administrators"))
        .build();
        JSONObject nav=results.navigationDetailsToJson();
        
        
        //by default, the current page is set, and it's 1
        assertEquals("page", 
                1,
                nav.get("page")
                );

        assertEquals("navLinks.current.name==1", "1",
                nav.getJSONObject("navLinks").getJSONObject("current").get("name")
                );
    }

}
