package edu.mit.broad.gp.core;

import org.eclipse.ui.plugin.*;
import org.osgi.framework.BundleContext;
import java.util.*;

import org.eclipse.jface.preference.IPreferenceStore;

;/**

 *  * The main plugin class to be used in the desktop.
 */
public class GPGECorePlugin extends AbstractUIPlugin {
	//The shared instance.
	private static GPGECorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public GPGECorePlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("edu.mit.broad.gp.core.GPGECorePluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static GPGECorePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = GPGECorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	
	/**
	 * Converts PREFERENCE_DELIMITER delimited String to a String array.
	 */
	public static final String PREFERENCE_DELIMITER = "|";
	public static final String SERVERS_PREFERENCE = "GenePatternServers";
	public static final String PROJ_DIRS_PREFERENCE = "projdirs";
	public static final String PROJ_DIRS_SUBDIR_PREFERENCE = "projdir_showsubdir";
	public static final String OLD_JOBS_PREFERENCE = "oldjobs";
	public static final String USER_ID_PREFERENCE = "username";
	
	protected void initializeDefaultPreferences(IPreferenceStore istore) {
		// XXX should load from default prefs file here
		//PreferenceStore store = (PreferenceStore)istore;
		istore.setDefault(SERVERS_PREFERENCE, "localhost:8080");
	
	}
	
	
	public static String[] prefAsArray(String preferenceValue) {
		StringTokenizer tokenizer =	new StringTokenizer(preferenceValue, PREFERENCE_DELIMITER);
		int tokenCount = tokenizer.countTokens();
		String[] elements = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}
		return elements;
	}
	public String[] getPreferenceArray(String key){
		return GPGECorePlugin.prefAsArray(getPreferenceStore().getString(key));
	}

	public boolean getPreferenceBoolean(String key){
		return (Boolean.valueOf(getPreferenceStore().getString(key))).booleanValue();
	}

	
	public void setPreferenceArray(String key, String[] elements){
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] != null){
				buffer.append(elements[i]);
				buffer.append(GPGECorePlugin.PREFERENCE_DELIMITER);
			}
		}
		getPreferenceStore().setValue(key, buffer.toString());	
	}
	
	public void addToPreferenceArray(String key, String value){
		if (value == null) return;
		String pds = GPGECorePlugin.getDefault().getPreferenceStore().getString(key);
        pds = pds + GPGECorePlugin.PREFERENCE_DELIMITER;
        pds = pds + value;
        GPGECorePlugin.getDefault().getPreferenceStore().setValue(key, pds);
	}
	
	public void removeFromPreferenceArray(String key, String value){
		String[] prefs = getPreferenceArray(key);
		
		for (int i=0; i < prefs.length; i++){
			if (prefs[i].equalsIgnoreCase(value)) prefs[i] = null;	
		}
		setPreferenceArray(key, prefs);
	}

	
	 public String getUsername() {
	 	return GPGECorePlugin.getDefault().getPreferenceStore().getString(USER_ID_PREFERENCE);
	 }
	 public void setUsername(String name) {
	 	 	GPGECorePlugin.getDefault().getPreferenceStore().putValue(USER_ID_PREFERENCE, name);
	 }
    /**
     * @return
     */
    public String[] getProjectDirectories() {
        StringTokenizer st = new StringTokenizer(getPreferenceStore().getString(PROJ_DIRS_PREFERENCE), java.io.File.pathSeparator);
        ArrayList list = new ArrayList();
        while(st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return (String[]) list.toArray(new String[0]);
    }
    
    public void setProjectDirectories(String[] projectDirs) {
        StringBuffer buf = new StringBuffer();
        
        for (int i=0; i < projectDirs.length; i++){
            if(projectDirs[i]!=null) {
                buf.append(projectDirs[i]);
                buf.append(java.io.File.pathSeparator);
        
            }
        }
        getPreferenceStore().setValue(PROJ_DIRS_PREFERENCE, buf.toString());
    }

    /**
     * @param projectDir
     */
    public void addProjectDirectory(String projectDir) {
        String current = GPGECorePlugin.getDefault().getPreferenceStore().getString(PROJ_DIRS_PREFERENCE);
        String value =  current + projectDir + java.io.File.pathSeparator;
        getPreferenceStore().setValue(PROJ_DIRS_PREFERENCE, value);
    }
    
    /**
     * @param projectDir
     */
    public void removeProjectDirectory(String projectDir) {
        String[] projectDirs = getProjectDirectories();
     
        for (int i=0; i < projectDirs.length; i++){
			if (projectDirs[i].equalsIgnoreCase(projectDir)) {
			    projectDirs[i] = null;
			    setProjectDirectories(projectDirs);
			    break;
			}
		}
    }
	
}


