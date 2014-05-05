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
    final int pageNum;
    String name=null;
    Rel rel=null;
    String href=null;

    public PageLink(final int pageNum) {
        this.pageNum=pageNum;
    }

    public int getPage() {
        return pageNum;
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

