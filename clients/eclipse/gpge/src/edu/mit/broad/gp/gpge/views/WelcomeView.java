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


package edu.mit.broad.gp.gpge.views;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import edu.mit.broad.gp.core.GPGECorePlugin;
import edu.mit.broad.gp.gpge.GpgePlugin;
import edu.mit.broad.gp.gpge.util.BrowserLauncher;
import edu.mit.broad.gp.gpge.views.data.nodes.ProjectDirNode;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class WelcomeView extends ViewPart {
    public static final String ID_VIEW = "edu.mit.broad.gp.gpge.views.WelcomeView";

    private DrillDownAdapter drillDownAdapter;
    HyperlinkAdapter linkAdapter = new HyperlinkAdapter() {
		public void linkActivated(HyperlinkEvent e) {
			try {
				String url = (String)e.getHref();
				
				BrowserLauncher.openURL(url);
			} catch (Exception ee) {ee.printStackTrace();}
		}
	};
   
	HyperlinkAdapter usernameAdapter = new HyperlinkAdapter() {
		public void linkActivated(HyperlinkEvent e) {
			try {
				String oldId = GPGECorePlugin.getDefault().getUsername();

				String name = getUserId(oldId);
				GPGECorePlugin.getDefault().setUsername(name);
				updateUsername(name);
				
			} catch (Exception ee) {ee.printStackTrace();}
		}
	};
	
	private void updateUsername(String name){
		idtxt.setText(name);
		identitySectioParent.reflow(true);
	}
	
	
    private SashForm sash_form;

     
    /**
     * The constructor.
     */
    public WelcomeView() {
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    private SashForm secondSash = null;

    public void createPartControl(Composite parent) {
        
  
        try {
            sash_form = new SashForm(parent, SWT.HORIZONTAL | SWT.NULL);
             
            FormToolkit toolkit = new FormToolkit(sash_form.getDisplay());
            final ScrolledForm visibleForm = toolkit.createScrolledForm(sash_form);
            if("macosx".equals(System.getProperty("osgi.os"))) {
                Color black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
                visibleForm.setForeground(black);
    			}
            
            visibleForm.setText("Welcome to GenePattern");
            ColumnLayout layout = new ColumnLayout();
            visibleForm.getBody().setLayout(layout);
            ColumnLayoutData td = new ColumnLayoutData();
           	
            createIdentitySection(toolkit, visibleForm);
            createDocumentationSection(toolkit, visibleForm);
            createResourcesSection(toolkit, visibleForm);
    		
    		visibleForm.setVisible(true);
             
    		
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public String getUserId(String oldId){
   		InputDialog dlg = new InputDialog(null, "GenePattern Identity",
				"Please provide your email address to login:", oldId, null) ;

		int okCancel = dlg.open();
		if (okCancel == InputDialog.CANCEL) return oldId;
		return dlg.getValue();
    }

    
    ScrolledForm identitySectioParent = null;
    Text idtxt = null;
    public void createIdentitySection( FormToolkit toolkit, final ScrolledForm parent){
    	// listen to changes via the preferences pages
    	GPGECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new UsernameChangeListener());
        
    	
    	Section identitySection = toolkit.createSection(parent.getBody(),
				Section.DESCRIPTION | Section.TWISTIE | Section.EXPANDED);
    	identitySectioParent = parent;
		String id = GPGECorePlugin.getDefault().getUsername();

		identitySection.setText("Identity");
		identitySection.setDescription("You are logged in as ");

		Composite sectionClient = toolkit.createComposite(identitySection);
		sectionClient.setLayout(new GridLayout());

		idtxt = toolkit.createText(sectionClient, id);
		
		Hyperlink usernameLnk = toolkit.createHyperlink(sectionClient, "change", SWT.WRAP);
		usernameLnk.addHyperlinkListener(usernameAdapter);

		identitySection.setClient(sectionClient);

		identitySection.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				parent.reflow(true);
			}
		});    	
   }
    
    
    public void createDocumentationSection( FormToolkit toolkit, final ScrolledForm parent){
    	 Section section = toolkit.createSection(parent.getBody(),Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
	
        section.setText("Documentation");
        section.setDescription("Click on the link to see\nthe documentation");
       
        Composite sectionClient = toolkit.createComposite(section);
    	sectionClient.setLayout(new GridLayout());
    	
     	ImageHyperlink imgLnk = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	imgLnk.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_DOC));
	 	imgLnk.setText("User's Manual/Tutorial");
	 	imgLnk.setHref("http://www.broad.mit.edu/cancer/software/genepattern/tutorial/");
	 	imgLnk.addHyperlinkListener(linkAdapter);
	 	
	 	ImageHyperlink linkRelNote = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	linkRelNote.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_DOC));
	 	linkRelNote.setText("Release Notes");
	 	linkRelNote.setHref("http://www.broad.mit.edu/cancer/software/genepattern/doc/relnotes/current/");
	 	linkRelNote.addHyperlinkListener(linkAdapter);
	 	
	 	
	 	ImageHyperlink linkFAQ = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	linkFAQ.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_DOC));
	 	linkFAQ.setText("FAQ");
	 	linkFAQ.setHref("http://www.broad.mit.edu/cancer/software/genepattern/faq/");
	 	linkFAQ.addHyperlinkListener(linkAdapter);
	
	 	
	 	ImageHyperlink linkPub = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	linkPub.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_FILE_NAV));
	 	linkPub.setText("Public Datasets");
	 	linkPub.setHref("http://www.broad.mit.edu/cancer/software/genepattern/datasets/");
	 	linkPub.addHyperlinkListener(linkAdapter);
	 	 	
	 	section.setClient(sectionClient);
	 	
		section.addExpansionListener(new ExpansionAdapter() {
		public void expansionStateChanged(ExpansionEvent e) {
			parent.reflow(true);
		}
		});    	
    }
    
    
    public void createResourcesSection( FormToolkit toolkit, final ScrolledForm parent){
   	 	Section section = toolkit.createSection(parent.getBody(),Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
	
   	 	section.setText("Resources");
   	 	section.setDescription("GenePattern Resources");
   	 	Composite sectionClient = toolkit.createComposite(section);
   	 	sectionClient.setLayout(new GridLayout());
   	
   	 	ImageHyperlink imgLnk = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	imgLnk.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_EMAIL));
	 	imgLnk.setText("Subscribe to mailing list");
	 	imgLnk.setHref("mailto:gp-users-join@broad.mit.edu?body=Just send this!");
	 	imgLnk.addHyperlinkListener(linkAdapter);
	 	
	 	ImageHyperlink linkQuestion = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	linkQuestion.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_QUESTION));
	 	linkQuestion.setText("Send a question to gp-help");
	 	linkQuestion.setHref("mailto:gp-help@broad.mit.edu");
	 	linkQuestion.addHyperlinkListener(linkAdapter);
	 		
	 	
	 	ImageHyperlink linkBug = toolkit.createImageHyperlink(sectionClient, SWT.WRAP);
	 	linkBug.setImage(GpgePlugin.getDefault().getImageRegistry().get(GpgePlugin.IMG_OBJ_BUG));
	 	linkBug.setText("Report a bug");
	 	linkBug.setHref("mailto:gp-help@broad.mit.edu");
	 	linkBug.addHyperlinkListener(linkAdapter);
	 		
		 	 	
	 	section.setClient(sectionClient);
	 	
		section.addExpansionListener(new ExpansionAdapter() {
		public void expansionStateChanged(ExpansionEvent e) {
			parent.reflow(true);
		}
		});   	
   }

    

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        sash_form.setFocus();
        
    }
    
    class UsernameChangeListener implements IPropertyChangeListener{
    	public void propertyChange(PropertyChangeEvent event) {
    		if (event.getProperty().equals(GPGECorePlugin.USER_ID_PREFERENCE)) {
    			//Update the view by adding/removing new directories
    			String id = GPGECorePlugin.getDefault().getUsername();
    			updateUsername(id);			
    		}
    	}
    }
    
}


