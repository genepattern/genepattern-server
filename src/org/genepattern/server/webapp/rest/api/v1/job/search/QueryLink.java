/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import org.genepattern.server.webapp.rest.api.v1.GpLink;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * More general implementation of the PageLink.
 * @author pcarr
 */
public class QueryLink {
    final String name;
    final Rel rel;
    final String href;

    /**
     * Create a representation of a link to do a job search by filter, 
     * where the filter is defined by the queryParam, e.g. groupId=test_group.
     * 
     * @param fromQuery, the original search query
     * @param name, the name of the link
     * @param rel, the link relation
     * @param queryParam, the query parameter, e.g. <name>=<value>
     */
    public QueryLink(final SearchQuery fromQuery, final String name, final Rel rel, final GpQueryParam queryParam) {
        this.name=name;
        this.rel=rel;
        if (queryParam != null) {
            this.href=fromQuery.jobsResourcePath+"?"+queryParam.toString();
        }
        else {
            this.href=fromQuery.jobsResourcePath;
        }            
    }

    /**
     * The name of the link, can be null.
     * @return
     */
    public String getName() {
        return name;
    }
    
    /**
     * The link relation, can be null.
     * @return
     */
    public Rel getRel() {
        return rel;
    }
    
    /**
     * The href
     * @return
     */
    public String getHref() {
        return href;
    }

    /**
     * Generate JSON representation.
     * @return
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        GpLink.BuilderJson b=new GpLink.BuilderJson();
        if (rel!=null) {
            b.rel(rel.name());
        }
        if (name!=null) {
            b.name(name);
        }
        if (href!=null) {
            b.href(href);
        }
        JSONObject jsonObj=b.build();
        return jsonObj;
    }
}

