package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.ServerConfiguration;

public class UIBean {
    public String getSkin() {
        String env = ServerConfiguration.instance().getGPProperty(UIBeanHelper.getUserContext(), "display.skin", "frozen");
        return env;
    }
    
    public boolean getNewUI() {
        boolean env = ServerConfiguration.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "display.ui", true);
        return env;
    }

    public boolean getParameterGroups() {
        boolean env = ServerConfiguration.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "group.parameters", false);
        return env;
    }

    public static String skin() {
        return (new UIBean()).getSkin();
    }
}
