package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.util.PropertiesManager_3_2;

public class CustomProperties implements Serializable {
    private static final Logger log = Logger.getLogger("CustomSettings.class");
    private static final long serialVersionUID = 1330010307546274883L;
    
    public static final String GP_URL = "GenePatternURL";

    private static final Comparator<KeyValuePair> sortByKeyIgnoreCase = new Comparator<KeyValuePair>() {
        @Override
        public int compare(final KeyValuePair o1, final KeyValuePair o2) {
            final String k1 = o1 == null ? "" : o1.getKey();
            final String k2 = o2 == null ? "" : o2.getKey();
            return k1.compareToIgnoreCase(k2);
        } 
    };

    private final List<KeyValuePair> customSettings;
    
    public CustomProperties() {
        this.customSettings=initCustomSettings();
    }
    
    public List<KeyValuePair> getCustomSettings() {
        Collections.sort(customSettings, sortByKeyIgnoreCase);
        return customSettings;
    }
    
    public void addCustomSetting(final String key, final String value) {
        //first remove any existing keys with same name
        removeDuplicateCustomSetting(key);
        customSettings.add(new KeyValuePair(key, value));
        storeChangesToCustomProperties(true);
    }

    private void removeDuplicateCustomSetting(final String key) {
        int removeIndex = -1;
        //check if the key already exists
        int i=0;
        Iterator<KeyValuePair> csIterator = customSettings.iterator();
        while(csIterator.hasNext()) {
            KeyValuePair kvp = csIterator.next();
            if(kvp.getKey().equals(key))
            {
                removeIndex = i;
            }
            i++;
        }

        if(removeIndex != -1) {
            customSettings.remove(removeIndex);
        }
    }

    public void deleteCustomSetting(final String keyToRemove) {
        for (KeyValuePair element : customSettings) {
            if (element.getKey().equals(keyToRemove)) {
                customSettings.remove(element);
                System.getProperties().remove(keyToRemove);
                storeChangesToCustomProperties(true);
                break;
            }
        }
    }

    public void saveGenePatternURL(final String genepatternURL) {
        KeyValuePair gpURL = new KeyValuePair(GP_URL, genepatternURL);
        removeDuplicateCustomSetting(GP_URL);
        customSettings.add(gpURL);
        storeChangesToCustomProperties(true);
    }

    /**
     * Save custom properties to file system and optionally reload the
     * genepattern.properties and custom.properties files.
     * 
     * @param reloadConfiguration
     */
    public void storeChangesToCustomProperties(final boolean reloadConfiguration) {
        PropertiesManager_3_2.storeChangesToCustomProperties(customSettings);
        if (reloadConfiguration) {
            ServerConfigurationFactory.reloadConfiguration();
        }
    }

    protected static List<KeyValuePair> initCustomSettings() {
        try {
            final Properties customProps = PropertiesManager_3_2.getCustomProperties();
            return KeyValuePair.fromProperties(customProps);
        }
        catch (IOException ioe) {
            log.error(ioe);
            return null;
        }
    }

}
