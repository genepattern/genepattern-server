package edu.mit.broad.gp.core.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import edu.mit.broad.gp.core.GPGECorePlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.BooleanFieldEditor;
/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */


public class ProjectDirPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	public static final String P_PATH = "pathPreference";
	public static final String P_STRING = "stringPreference";

	public ProjectDirPreferencePage() {
		super(GRID);
		setPreferenceStore(GPGECorePlugin.getDefault().getPreferenceStore());
		setDescription("GenePattern Project Directories");
		initializeDefaults();
	}
/**
 * Sets the default values of the preferences.
 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		store.setDefault(P_STRING, "Default value");
	}
	
/**
 * Creates the field editors. Field editors are abstractions of
 * the common GUI blocks needed to manipulate various types
 * of preferences. Each field editor knows how to save and
 * restore itself.
 */

	public void createFieldEditors() {
				
		addField(new PathEditor(GPGECorePlugin.PROJ_DIRS_PREFERENCE, "Project &Directories", "add", getFieldEditorParent()));
				
		addField(new BooleanFieldEditor(GPGECorePlugin.PROJ_DIRS_SUBDIR_PREFERENCE,"Show Project Directory sub-directories", getFieldEditorParent()));
		
	}
	
	public void init(IWorkbench workbench) {
	}
}