package org.genepattern.server.webapp.rest.api.v1.job;

import java.util.List;

public class JobInputValues {
    public static class Param {
        public String name;
        public List<String> values;
        public boolean batchParam=false;
    }

    public String lsid;
    public List<Param> params;
    
    public JobInputValues() {
    }
}
