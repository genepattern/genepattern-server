package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.util.PropertiesManager_3_2;

public class CustomProperties {
    private static final Logger log = Logger.getLogger("CustomSettings.class");
    
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
        PropertiesManager_3_2.storeChangesToCustomProperties(customSettings);
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
                PropertiesManager_3_2.storeChangesToCustomProperties(customSettings);
                break;
            }
        }
    }

    public void saveGenePatternURL(final String genepatternURL) {
        KeyValuePair gpURL = new KeyValuePair(GP_URL, genepatternURL);
        removeDuplicateCustomSetting(GP_URL);
        customSettings.add(gpURL);
        PropertiesManager_3_2.storeChangesToCustomProperties(customSettings);
    }

    public void storeChangesToCustomProperties() {
        PropertiesManager_3_2.storeChangesToCustomProperties(customSettings);
        //reload of genepattern.properties and custom.properties files after making changes
        //    ServerConfigurationFactory.reloadConfiguration();
    }

    protected static List<KeyValuePair> initCustomSettings() {
        try {
            Properties tmp = PropertiesManager_3_2.getCustomProperties();
            List<KeyValuePair> customSettings = new ArrayList<KeyValuePair>();
            for (Map.Entry entry : tmp.entrySet()) {
                customSettings.add(new KeyValuePair((String) entry.getKey(), (String) entry.getValue()));
            }
            return customSettings;
        } 
        catch (IOException ioe) {
            log.error(ioe);
            return null;
        }
    }

}
