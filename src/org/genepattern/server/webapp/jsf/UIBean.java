package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.ServerConfiguration;

public class UIBean {
    public String getSkin() {
        String env = ServerConfiguration.instance().getGPProperty(UIBeanHelper.getUserContext(), "display.skin", ".");
        return env;
    }
    
    public boolean newUI() {
        boolean env = ServerConfiguration.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "display.ui", false);
        return env;
    }
}
