/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.util;

import org.genepattern.util.GPConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.webapp.jsf.KeyValuePair;

public class PropertiesManager_3_2 {
    private static PropertiesManager_3_2 inst;

    private static Logger log = Logger.getLogger(PropertiesManager_3_2.class);

    private Map<String, Properties> propertiesMap;

    private PropertiesManager_3_2() {
        propertiesMap = new HashMap<String, Properties>();
    }

    public synchronized Properties getCommandPrefixes() {
        return propertiesMap.get(GPConstants.COMMAND_PREFIX);

    }

    public synchronized Properties getJavaFlags() {
        Properties p = propertiesMap.get(GPConstants.JAVA_FLAGS);
        if (p == null) p = new Properties();
        return p;
    }

    
    public Properties getProperties(String name) {
        return propertiesMap.get(name);
    }

    public Properties getTaskPrefixMapping() {
        return propertiesMap.get(GPConstants.TASK_PREFIX_MAPPING);
    }

    public void reloadCommandPrefixesFromDisk() {
        reloadFromDisk(GPConstants.COMMAND_PREFIX);
        reloadFromDisk(GPConstants.TASK_PREFIX_MAPPING);
    }
    
    
    private void reloadFromDisk(String name) {
        Properties javaFlags = new Properties();
        File jfFile = new File(getPropsDir(), name + ".properties");
        log.debug("Loading: " + jfFile.getName());
        
        if (jfFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(jfFile);
                javaFlags.load(fis);
            } catch (IOException e) {
                log.error("Error loading " + name, e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {

                    }
                }
            }
        }
        log.debug("Read " + javaFlags);
        propertiesMap.put(name, javaFlags);
    }
    
    
    public void reloadJavaFlagsFromDisk() {
         reloadFromDisk(GPConstants.JAVA_FLAGS);
    }
    

    public boolean saveProperties(String name, Properties props) {
        boolean storeSuccess = false;
        propertiesMap.put(name, props);
        FileOutputStream fos = null;
        try {
            File propFile = new File(getPropsDir(), name + ".properties");
            fos = new FileOutputStream(propFile);
            props.store(fos, " ");
            storeSuccess = true;
        } catch (IOException e) {
            storeSuccess = false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        return storeSuccess;
    }

    public void setCommandPrefixes(Properties p) {
        propertiesMap.put(GPConstants.COMMAND_PREFIX, p);
    }
    
    public void setJavaFlags(Properties p) {
        propertiesMap.put(GPConstants.JAVA_FLAGS, p);
    }

    public void setTaskPrefixMapping(Properties p) {
        propertiesMap.put(GPConstants.TASK_PREFIX_MAPPING, p);
    }

    public static boolean appendArrayPropertyAndStore(String key, String val, String delimiter, boolean unique,
            boolean caseSensitive) {
        String propString = System.getProperty(key);

        if (unique) {
            ArrayList currentVals = getArrayProperty(key, "", delimiter);
            Iterator iter = currentVals.iterator();
            while (iter.hasNext()) {
                String aVal = (String) iter.next();
                if (caseSensitive) {
                    if (aVal.equals(val))
                        return true;
                } else {
                    if (aVal.equalsIgnoreCase(val))
                        return true;
                }
            }
        }
        propString = propString + delimiter + val;
        return storeChange(key, propString);
    }

    public static ArrayList getArrayProperty(String key, String defaultValue, String delimiter) {

        ArrayList<String> props = new ArrayList<String>();
        String propString = System.getProperty(key, defaultValue);
        StringTokenizer strtok = new StringTokenizer(propString, delimiter);
        while (strtok.hasMoreTokens()) {
            String p = strtok.nextToken();
            props.add(p);
        }
        return props;
    }

    public static Properties getCustomProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;

        try {
            File propFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "custom.properties");

            propFile.createNewFile();

            fis = new FileInputStream(propFile);
            props.load(fis);

        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return props;

    }
    
  
    public static Properties getDefaultProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;

        try {
            File propFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "genepattern.properties.default");
            fis = new FileInputStream(propFile);
            props.load(fis);

        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return props;

    }

    public static Properties getGenePatternProperties() throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;

        try {
            File propFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "genepattern.properties");
            fis = new FileInputStream(propFile);
            props.load(fis);

        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return props;

    }

    public static PropertiesManager_3_2 getInstance() {
        synchronized (PropertiesManager_3_2.class) {
            if (inst == null) {
                inst = new PropertiesManager_3_2();
                inst.reloadCommandPrefixesFromDisk();
                inst.reloadJavaFlagsFromDisk();
            }
            return inst;
        }
    }

    public static String getPropsDir() {
        return ServerConfigurationFactory.instance().getResourcesDir().toString();
    }

    public static boolean removeArrayPropertyAndStore(String key, String val, String delimiter, boolean caseSensitive) {
        String propString = "";
        ArrayList currentVals = getArrayProperty(key, "", delimiter);
        Iterator iter = currentVals.iterator();
        boolean first = true;
        while (iter.hasNext()) {
            String aVal = (String) iter.next();
            if (caseSensitive) {
                if (aVal.equals(val))
                    continue;
            } else {
                if (aVal.equalsIgnoreCase(val))
                    continue;
            }
            if (!first) {
                propString += delimiter;
            }
            propString += aVal;
            first = false;
        }
        return storeChange(key, propString);

    }

    public static boolean removeProperties(Vector keys) {
        Properties sysProps = System.getProperties();
        for (int i = 0; i < keys.size(); i++) {
            String key = (String) keys.get(i);
            sysProps.remove(key);
        }
        System.setProperties(sysProps);

        try {
            Properties props = getGenePatternProperties();
            StringBuffer commentBuff = new StringBuffer("#Genepattern server removed keys: ");
            for (int i = 0; i < keys.size(); i++) {
                String key = (String) keys.get(i);
                props.remove(key);
                if (i > 0)
                    commentBuff.append(", ");
                commentBuff.append(key);
            }
            storeGenePatternProperties(props, commentBuff.toString());

            return true;

        } catch (IOException ioe) {
            return false;
        }
    }

    public static boolean storeChange(String key, String value) {
        boolean storeSuccess = false;
        System.setProperty(key, value);
        try {
            Properties props = getGenePatternProperties();
            props.setProperty(key, value);
            storeGenePatternProperties(props, "#Genepattern server updated key: " + key);
            storeSuccess = true;
        } catch (Exception e) {
            storeSuccess = false;
        }

        return storeSuccess;
    }

    public static boolean storeChanges(Properties newProps) {
        boolean storeSuccess = false;
        for (Iterator iter = newProps.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String val = newProps.getProperty(key);
            System.setProperty(key, val);
        }

        try {
            int i = 0;
            Properties props = getGenePatternProperties();
            StringBuffer commentBuff = new StringBuffer("#Genepattern server updated keys: ");
            for (Iterator iter = newProps.keySet().iterator(); iter.hasNext(); i++) {
                String key = (String) iter.next();
                String val = newProps.getProperty(key);
                props.setProperty(key, val);
                if (i > 0)
                    commentBuff.append(", ");
                commentBuff.append(key);
            }
            storeGenePatternProperties(props, commentBuff.toString());
            storeSuccess = true;
        } catch (Exception e) {
            storeSuccess = false;
        }

        return storeSuccess;
    }

    public static boolean storeChangesToCustomProperties(List<KeyValuePair> newProps) {
        boolean storeSuccess = false;
        for (KeyValuePair keyValue : newProps) {
            String key = keyValue.getKey();
            String val = keyValue.getValue();
            System.setProperty(key, val);
        }

        try {
            int i = 0;
            Properties props = new Properties();
            StringBuffer commentBuff = new StringBuffer("#Genepattern server updated keys: ");
            for (KeyValuePair keyValue : newProps) {
                String key = keyValue.getKey();
                String val = keyValue.getValue();
                props.setProperty(key, val);
                if (i > 0)
                    commentBuff.append(", ");
                commentBuff.append(key);
            }
            storeCustomProperties(props, commentBuff.toString());
            storeSuccess = true;
        } catch (Exception e) {
            storeSuccess = false;
        }

        return storeSuccess;
    }

    protected static void storeCustomProperties(Properties props, String comment) throws IOException {
        FileOutputStream fos = null;
        try {
            File propFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "custom.properties");
            fos = new FileOutputStream(propFile);
            props.store(fos, comment);

        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    protected static void storeGenePatternProperties(Properties props, String comment) throws IOException {
        FileOutputStream fos = null;
        try {
            File propFile = new File(ServerConfigurationFactory.instance().getResourcesDir(), "genepattern.properties");
            fos = new FileOutputStream(propFile);
            props.store(fos, comment);

        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

}
