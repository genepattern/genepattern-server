package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.ArrayList;
import java.util.List;

import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * Helper class for generating a JSON representation of a link to another resource,
 * for example a link to a job result file.
 * 
 * @author pcarr
 *
 */
public class GpLink {

    final String href;
    final String name;
    final List<Rel> rels;
    
    public GpLink(Builder in) {
        this.name=in.name;
        this.rels=ImmutableList.copyOf(in.rels);
        this.href=in.href;
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject link = new JSONObject();
        link.put("href", href);
        link.put("name", name);
        if (rels != null) {
            String relStr=Joiner.on(" ").join(rels);
            link.put("rel", relStr);
        }
        return link;
    }
    
    public static class Builder {
        private String href=null;
        private String name=null;
        private List<Rel> rels=null;
        
        public Builder href(String href) {
            this.href=href;
            return this;
        }
        
        public Builder name(String name) {
            this.name=name;
            return this;
        }
        
        public Builder addRels(List<Rel> rels) {
            if (rels==null || rels.size()==0) {
                // ignore null or empty arg
                return this;
            }
            for(final Rel rel : rels) {
                addRel(rel);
            }
            return this;
        }

        public Builder addRel(Rel rel) {
            if (rel==null) {
                // ignore null arg
                return this;
            }
            if (rels==null) {
                rels=new ArrayList<Rel>();
            }
            rels.add(rel);
            return this;
        }

        public GpLink build() {
            return new GpLink(this);
        }
    }

}
