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


/*
 * Created on Jun 22, 2004
 *
 * Preference Page for managing the Project Directories
 */
package edu.mit.broad.gp.core.preferences;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.*;
import edu.mit.broad.gp.core.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;


/**
 * @author genepattern
 *
 */
public class ServerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private List server;
	//The newEntryText is the text where new bad words are specified
	private Text newEntryText;

	/**
	 * 
	 */
	public ServerPreferencePage() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		setPreferenceStore(GPGECorePlugin.getDefault().getPreferenceStore());

	}
	
	
	public String[] getDefaultServersPreference(){
		return GPGECorePlugin.prefAsArray(getPreferenceStore().getDefaultString(GPGECorePlugin.SERVERS_PREFERENCE));
	}
	public String[] getServersPreference() {
		return GPGECorePlugin.getDefault().getPreferenceArray(GPGECorePlugin.SERVERS_PREFERENCE);
	}
	
	protected Control createContents(Composite parent) {

		Composite entryTable = new Composite(parent, SWT.NULL);

		//Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);			
				
		//Add in a dummy label for spacing
		Label label = new Label(entryTable,SWT.NONE);
		label.setText("GenePattern Servers");
		
		server = new List(entryTable, SWT.BORDER);
		server.setItems(getServersPreference());

		//Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH);
		server.setLayoutData(data);
		
		Composite buttonComposite = new Composite(entryTable,SWT.NULL);
		
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 2;
		buttonComposite.setLayout(buttonLayout);

		//Create a data that takes up the extra space in the dialog and spans both columns.
		data = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING);
		buttonComposite.setLayoutData(data);		
		
		Button addButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		addButton.setText("Add to List"); //$NON-NLS-1$
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				server.add(newEntryText.getText(), server.getItemCount());
			}
		});
		
		newEntryText = new Text(buttonComposite, SWT.BORDER);
		//Create a data that takes up the extra space in the dialog .
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		newEntryText.setLayoutData(data);
		
		
		Button removeButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);

		removeButton.setText("Remove Selection"); //$NON-NLS-1$
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				server.remove(server.getSelectionIndex());
			}
		});
		
		data = new GridData();
		data.horizontalSpan = 2;
		removeButton.setLayoutData(data);
	
		return entryTable;
	}

	protected void performDefaults() {
		server.setItems(getDefaultServersPreference());
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		String[] elements = server.getItems();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(elements[i]);
			buffer.append(GPGECorePlugin.PREFERENCE_DELIMITER);
		}
		getPreferenceStore().setValue(GPGECorePlugin.SERVERS_PREFERENCE, buffer.toString());	

		return super.performOk();
	}

}
