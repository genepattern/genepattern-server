/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package edu.mit.broad.gp.core.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import edu.mit.broad.gp.core.GPGECorePlugin;
import org.eclipse.jface.preference.IPreferenceStore;

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


public class UserIdentityPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
	
	
	public UserIdentityPreferencePage() {
		super(GRID);
		setPreferenceStore(GPGECorePlugin.getDefault().getPreferenceStore());
		setDescription("Username (email address)");
		initializeDefaults();
	}
/**
 * Sets the default values of the preferences.
 */
	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();
		
		store.setDefault(GPGECorePlugin.USER_ID_PREFERENCE, System.getProperty("user.name"));
	}
	
/**
 * Creates the field editors. Field editors are abstractions of
 * the common GUI blocks needed to manipulate various types
 * of preferences. Each field editor knows how to save and
 * restore itself.
 */

	public void createFieldEditors() {
	
		addField(
			new StringFieldEditor(GPGECorePlugin.USER_ID_PREFERENCE, "Username:", getFieldEditorParent()));
	}
	
	public void init(IWorkbench workbench) {
	}
}