package org.genepattern.server.webapp.rest.api.v1;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Generic representation of a hypermedia link, for serializing to JSON 
 * in response to REST API calls.
 * @author pcarr
 *
 */
public class GpLink {
    /**
     * Build a JSON representation of a link.
     * <pre>
       {
           "name": "Next",
           "rel": "next",
           "href": "http://127.0.0.1:8080/gp/rest/v1/jobs?page=2"
       }
     * </pre>
     * @author pcarr
     *
     */
    public static class BuilderJson {
        private String name;
        private String rel; //link relation, (note, if necessary, can be a space separated list)
        private String href; //fully qualified path
        public BuilderJson() {
        }

        public BuilderJson name(final String name) {
            this.name=name;
            return this;
        }
        
        public BuilderJson rel(final String rel) {
            this.rel=rel;
            return this;
        }

        public BuilderJson href(final String href) {
            this.href=href;
            return this;
        }
        
        public JSONObject build() throws JSONException {
            JSONObject jsonObj=new JSONObject();
            jsonObj.put("name", name);
            jsonObj.put("rel", rel);
            jsonObj.put("href", href);
            return jsonObj;
        }
    }
}
