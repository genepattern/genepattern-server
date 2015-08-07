/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.search;

import org.genepattern.server.webapp.rest.api.v1.GpLink;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Representation of a Link to a page within a job search.
 * @author pcarr
 *
 */
public class PageLink {
    final int page;
    String name=null;
    Rel rel=null;
    String href=null;

    public PageLink(final int pageNum) {
        this.page=pageNum;
    }

    public int getPage() {
        return page;
    }
    public String getName() {
        return name;
    }
    public Rel getRel() {
        return rel;
    }
    public String getHref() {
        return href;
    }

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

