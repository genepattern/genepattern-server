package edu.mit.broad.gp.gpge;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class GpgePlugin extends AbstractUIPlugin {
	//The shared instance.
	private static GpgePlugin plugin;
	public static String ID_PLUGIN = "edu.mit.broad.gp.gpge";
	//Resource bundle.
	private ResourceBundle resourceBundle;
	public static boolean SHUTDOWN_IN_PROGRESS = false;
	
	/**
	 * The constructor.
	 */
	public GpgePlugin() {
		super();
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("edu.mit.broad.gp.gpge.GpgePluginResources");
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
	public static GpgePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = GpgePlugin.getDefault().getResourceBundle();
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
	
	public static boolean isShutdownInProgress(){
	    return SHUTDOWN_IN_PROGRESS;
	}
	public static void setShutdownInProgress(boolean val){
	    SHUTDOWN_IN_PROGRESS = val;
	}
	
	
	public static String IMG_OBJ_SERVER = "server";
	public static String IMG_OBJ_FILE = ISharedImages.IMG_OBJ_FILE;
	public static String IMG_OBJ_FILE_NAV = "fileNav";
	public static String IMG_OBJ_FOLDER = ISharedImages.IMG_OBJ_FOLDER;
	public static String IMG_OBJ_ELEMENT = ISharedImages.IMG_OBJ_ELEMENT;
	public static String IMG_OBJ_FOLDER_OUTPUT = "OutputFolder";
	public static String IMG_OBJ_DOC = "documents";
	public static String IMG_OBJ_EMAIL = "email";
	public static String IMG_OBJ_BUG = "bug";
	public static String IMG_OBJ_QUESTION = "question";
	public static String IMG_OBJ_FILE_UNKNOWN = "unknownFile";
	
	protected void initializeImageRegistry(ImageRegistry reg) {
	   reg.put(IMG_OBJ_SERVER, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/console_view_small.gif"));
	   reg.put(IMG_OBJ_FOLDER_OUTPUT, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/output_folder_attrib.gif"));
	   reg.put(IMG_OBJ_DOC, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/library_obj.gif"));
	   reg.put(IMG_OBJ_EMAIL, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/email_obj.gif"));
	   reg.put(IMG_OBJ_BUG, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/bug_obj.gif"));
	   reg.put(IMG_OBJ_FILE_NAV, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/filenav_nav.gif"));
	   reg.put(IMG_OBJ_QUESTION, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/question_obj.gif"));
	   reg.put(IMG_OBJ_FILE_UNKNOWN, GpgePlugin.imageDescriptorFromPlugin(GpgePlugin.ID_PLUGIN ,"icons/unknown_obj.gif"));
       
	   reg.put(IMG_OBJ_FILE, PlatformUI.getWorkbench().getSharedImages().getImage(IMG_OBJ_FILE));
	   reg.put(IMG_OBJ_FOLDER, PlatformUI.getWorkbench().getSharedImages().getImage(IMG_OBJ_FOLDER));
	   reg.put(IMG_OBJ_ELEMENT, PlatformUI.getWorkbench().getSharedImages().getImage(IMG_OBJ_ELEMENT));
		    
	}
	
	/**
	 * XXX add logic here to create a map of file types to icons 
	 * @param type
	 * @return
	 */
	public Image getImageForFileType(String type){
	    if (type == null)  return getImageRegistry().get(IMG_OBJ_FILE_UNKNOWN);
	    
	    String fileType = type.toLowerCase();
	    String key = IMG_OBJ_FILE;  // the default
	    
	    if (fileType.equals("pl")){  // as an example, perl files are 'unknown'
	        key = IMG_OBJ_FILE_UNKNOWN;
	    }
	    
	    return getImageRegistry().get(key);
	}
	
	
}
