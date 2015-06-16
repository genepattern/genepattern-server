/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Representation of page navigation links, based on initial JSF implementation in JobBean.java
 * and jobResults.xhtml.
 * This can be used to re-create the existing page navigation element at the bottom of the page.
 * @author pcarr
 *
 */
public class PageNav {
    private final SearchQuery query;
    private final int numItems;
    private final int numPages;
    private PageLink prev=null;
    private PageLink first=null;
    private List<PageLink> prevRange=new ArrayList<PageLink>();
    private PageLink current=null;
    private List<PageLink> nextRange=new ArrayList<PageLink>();
    private PageLink last=null;
    private PageLink next=null;

    public PageNav(final SearchQuery query, final int numItems) {
        this.query=query;
        this.numItems=numItems;
        this.numPages=calcNumPages(query.getPageSize(), numItems);
        initPageLinks(numPages);
    }

    private void initPageLinks(final int numPages) {
        // a list of page numbers to display as links
        final int[] pageRange=getPageRange(query.getPageNum(), numPages);
        final int startNum=pageRange[0];
        final int endNum=pageRange[1];

        //add 'Current' link
        current=query.makePageLink(Rel.self, ""+query.getPageNum(), query.getPageNum());
        //add 'First' link
        first=query.makePageLink(Rel.first, "1", 1);
        //add 'Last' link
        last=query.makePageLink(Rel.last, ""+numPages, numPages);
        //if necessary add 'Previous' link
        if (query.getPageNum()>1) {
            prev=query.makePageLink(Rel.prev, "Previous", query.getPageNum()-1);
        }
        //if necessary add 'Next' link
        if (query.getPageNum()<numPages) {
            next=query.makePageLink(Rel.next, "Next", query.getPageNum()+1);
        }

        //add links to prev range
        for(int page=startNum; page<query.getPageNum(); ++page) {
            final PageLink pageLink=query.makePageLink(""+page, page);
            prevRange.add(pageLink);
        }
        //add links to next range
        for(int page=query.getPageNum()+1; page<=endNum; ++page) {
            final PageLink pageLink=query.makePageLink(""+page, page);
            nextRange.add(pageLink);
        }
    }

    /// utility methods
    /**
     * Helper method for computing the number of pages.
     * @param pageSize, the number if items per page
     * @param numItems, the total number of items in the search result.
     * @return
     */
    protected static int calcNumPages(final int pageSize, final int numItems) {
        if (numItems>0) {
            return (int) Math.ceil(numItems / (double) pageSize);
        }
        //always have at least one page
        return 1;
    }

    /**
     * Helper method for computing the range of navigation links to generate.
     * 
     * @param pageNumber, the current page
     * @param pageCount, the total number of pages, based on the number of job results and the pageSize.
     * 
     * @return an 2 element array, range[0] is the page number of the first link and range[1] is the page number of the last link.
     */
    protected static int[] getPageRange(final int pageNumber, final int pageCount) {
        final int MAX_PAGES = 25;
        int startNum = 1;
        int endNum = pageCount;
        if (pageCount > MAX_PAGES) {
            endNum = Math.max(pageNumber + (MAX_PAGES / 2), MAX_PAGES);
            endNum = Math.min(endNum, pageCount);
            startNum = endNum - MAX_PAGES - 1;
            startNum = Math.max(startNum, 1);
        }
        return new int[]{ startNum, endNum };
    }
    


    private static List<Integer> getPages(final int pageNumber, final int pageCount) {
        // this is the number of links to display in a page
        final int MAX_PAGES = 25;
        int startNum = 1;
        int endNum = pageCount;
        if (pageCount > MAX_PAGES) {
            endNum = Math.max(pageNumber + (MAX_PAGES / 2), MAX_PAGES);
            endNum = Math.min(endNum, pageCount);
            startNum = endNum - MAX_PAGES - 1;
            startNum = Math.max(startNum, 1);
        }
        List<Integer> pages = new ArrayList<Integer>();
        if (startNum > 1) {
            pages.add(1);
        }
        if (startNum > 2) {
            pages.add(-1); // GAP
        }
        for (int i = startNum; i <= endNum; i++) {
            pages.add(i);
        }
        if (endNum < (pageCount - 1)) {
            pages.add(-1);
        }
        if (endNum < pageCount) {
            pages.add(pageCount);
        }
        return pages;
    }

    public int getNumItems() {
        return numItems;
    }
    
    public int getNumPages() {
        return numPages;
    }
    
    /**
     * Get the link to the previous page, can be null.
     * @return
     */
    public PageLink getPrev() {
        return prev;
    }
    /**
     * Get the link to the first page in the list of results.
     * @return
     */
    public PageLink getFirst() {
        return first;
    }
    /**
     * Get the range of links immediately previous to the current page.
     * @return
     */
    public List<PageLink> getPrevRange() {
        return prevRange;
    }

    /**
     * Get the current page.
     * @return
     */
    public PageLink getCurrent() {
        return current;
    }

    /**
     * Get the range of links immediately next to the current page.
     * @return
     */
    public List<PageLink> getNextRange() {
        return nextRange;
    }

    /**
     * Get the last page, can be null.
     * @return
     */
    public PageLink getLast() {
        return last;
    }

    /**
     * Get the next page, can be null.
     * @return
     */
    public PageLink getNext() {
        return next;
    }


    //JSON format
    /**
     * Helper class for JSON representation of the page 'nav' section of the search results.
     * <pre>
           navLinks: {
                prev: {   <-- link
                    "rel": "",
                    "name": "",
                    "href": "" 
                }, 
                first: {}, <-- link
                prevItems: [ {}, {}, ..., {} ], <-- links
                current: {}, <-- link
                nextItems: [ {}, {}, ..., {}], <-- links
                last: {},
                next: {} <-- link
            }
     * </pre>
     * @return
     */
    public JSONObject navLinksToJson() throws JSONException {
        JSONObject nav=new JSONObject();
        if (prev != null) {
            nav.put("prev", prev.toJson());
        }
        if (first != null) {
            nav.put("first", first.toJson());
        }
        if (prevRange != null) {
            JSONArray prevArr=new JSONArray();
            for(final PageLink pageLink : prevRange) {
                prevArr.put(pageLink.toJson());
            }
            nav.put("prevItems", prevArr);
        }
        nav.put("current", current.toJson());
        if (nextRange != null) {
            JSONArray nextArr=new JSONArray();
            for(final PageLink pageLink : nextRange) {
                nextArr.put(pageLink.toJson());
            }
            nav.put("nextItems", nextArr);
        }
        if (last != null) {
            nav.put("last", last.toJson());
        }
        if (next != null) {
            nav.put("next", next.toJson());
        }
        return nav;
    }
}
