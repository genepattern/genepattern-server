package org.genepattern.server.webapp.jsf;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationFactory;

public class UIBean {
    public String getSkin() {
        String env = ServerConfigurationFactory.instance().getGPProperty(UIBeanHelper.getUserContext(), "display.skin", "frozen");
        return env;
    }
    
    public boolean getNewUI() {
        boolean env = ServerConfigurationFactory.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "display.ui", true);
        return env;
    }

    public boolean getParameterGroups() {
        boolean env = ServerConfigurationFactory.instance().getGPBooleanProperty(UIBeanHelper.getUserContext(), "group.parameters", true);
        return env;
    }

    public static String skin() {
        return (new UIBean()).getSkin();
    }
}
