/*
 * Created on Jun 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import edu.mit.broad.gp.gpge.views.WelcomeView;
import edu.mit.broad.gp.gpge.views.data.DataView;
import edu.mit.broad.gp.gpge.views.module.ModuleFormView;

/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GPGEPerspective implements IPerspectiveFactory {
	 public static final String ID_PERSPECTIVE =
        "edu.mit.broad.gp.gpge.GPGEPerspective"; 
	/**
	 * 
	 */
	public GPGEPerspective() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
  	
		layout.addView(
				WelcomeView.ID_VIEW,
	            IPageLayout.RIGHT,
	            0.3f,
	            IPageLayout.ID_EDITOR_AREA);
		
		layout.addView(DataView.ID_VIEW,
				IPageLayout.LEFT,
				0.7f,
				IPageLayout.ID_EDITOR_AREA);
		
        layout.addPerspectiveShortcut(ID_PERSPECTIVE);
        layout.addShowViewShortcut(WelcomeView.ID_VIEW);
        layout.addShowViewShortcut(DataView.ID_VIEW);
        layout.addShowViewShortcut(ModuleFormView.ID_VIEW);

       

	}

	
	
	
}
