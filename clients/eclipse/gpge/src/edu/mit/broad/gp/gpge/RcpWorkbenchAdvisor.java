/*
 * Created on Jun 18, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;

import edu.mit.broad.gp.core.GPGECorePlugin;
import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.views.WelcomeView;
import edu.mit.broad.gp.gpge.views.module.ModuleFormView;

/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RcpWorkbenchAdvisor extends WorkbenchAdvisor {
    static RcpWorkbenchAdvisor instance;
	/**
	 * 
	 */
	public RcpWorkbenchAdvisor() {
		super();
		instance = this;
		// TODO Auto-generated constructor stub
	}
	
	public static RcpWorkbenchAdvisor getInstance() {
	    return instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchAdvisor#getInitialWindowPerspectiveId()
	 */
	public String getInitialWindowPerspectiveId() {
        return GPGEPerspective.ID_PERSPECTIVE; //$NON-NLS-1$
    }

	public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
		super.preWindowOpen(configurer);
        configurer.setInitialSize(new Point(1024, 768));
        configurer.setShowCoolBar(false);
        configurer.setShowStatusLine(true);
		configurer.setTitle("GenePattern 2.0 pre-alpha"); //$NON-NLS-1$	
	
	}
	
	
	
	public boolean preShutdown(){
	    boolean ret = super.preShutdown();
	    GpgePlugin.setShutdownInProgress(true);
	    return ret;
	}
	
	public void postStartup() {
		createServerViews();
		try {
			// make the welcome page the top view hiding the connected servers
			getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(WelcomeView.ID_VIEW,null,IWorkbenchPage.VIEW_ACTIVATE);
		} catch (Exception e){};
	}
	
	public void createServerViews(){
		IPreferenceStore store = GPGECorePlugin.getDefault().getPreferenceStore();
		//System.out.println(store.getString(GPGECorePlugin.SERVERS_PREFERENCE));
		
		Set servers = ServiceManager.getServerNames();
		try {
			for (Iterator iter = servers.iterator(); iter.hasNext(); ){
				String serverName = (String)iter.next();
				IViewPart newView = getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ModuleFormView.ID_VIEW,serverName,IWorkbenchPage.VIEW_ACTIVATE);
			}
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public void refreshServer(IViewPart view, String server) {
	    try {
	        String id = ServiceManager.addServer(server);
	        getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(view);
            getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ModuleFormView.ID_VIEW,id,IWorkbenchPage.VIEW_ACTIVATE);
        } catch (PartInitException e) {
            e.printStackTrace();
        }
	}
	public void fillActionBars(IWorkbenchWindow window, 
			IActionBarConfigurer configurer,
		    int flags) {
		    	super.fillActionBars(window, configurer, flags);
		    	if ((flags & FILL_MENU_BAR) != 0) {
		        fillMenuBar(window, configurer);
		    }
		}
	
	private void fillMenuBar(
		    IWorkbenchWindow window,
		    IActionBarConfigurer configurer) {
		    IMenuManager menuBar = configurer.getMenuManager();
		    menuBar.add(createFileMenu(window));
		    menuBar.add(createWindowMenu(window));
		    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		    menuBar.add(createHelpMenu(window));
		    
		   
		}
	
	private MenuManager createFileMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("File", IWorkbenchActionConstants.M_FILE);
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(createNewServerAction()); // to connect to another server
		
		menu.add(ActionFactory.QUIT.create(window));
		menu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
		return menu;
	}
	private MenuManager createWindowMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("Window", IWorkbenchActionConstants.M_WINDOW);
		menu.add(ActionFactory.OPEN_NEW_WINDOW.create(window));
		menu.add(new Separator());
		MenuManager viewMenu = new MenuManager("Show_View"); //$NON-NLS-1$
		IContributionItem viewList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		viewMenu.add(viewList);
		menu.add(viewMenu);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(ActionFactory.PREFERENCES.create(window));
		menu.add(ContributionItemFactory.OPEN_WINDOWS.create(window));

		return menu;
	}
	private MenuManager createHelpMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("Help", IWorkbenchActionConstants.M_HELP); //$NON-NLS-1$
		// Welcome or intro page would go here
		menu.add(ActionFactory.HELP_CONTENTS.create(window));
		// Tips and tricks page would go here
		menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
		menu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
		menu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		// About should always be at the bottom
		// To use the real RCP About dialog uncomment these lines
		// menu.add(new Separator());
		menu.add(ActionFactory.ABOUT.create(window));

		return menu;
	}
	
	private Action createNewServerAction(){
		
		Action action1 = new Action() {
			public void run() {
				try{
					String server = getServer();
					String id = ServiceManager.addServer(server);
					System.out.println("id " + id);
					if (server != null){
					    getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ModuleFormView.ID_VIEW,id,IWorkbenchPage.VIEW_ACTIVATE);
					}
					
					if (confirmMessage("Remember server "+server+" when you log out?")){
					    GPGECorePlugin.getDefault().addToPreferenceArray(GPGECorePlugin.SERVERS_PREFERENCE, server);
					}
					
				} catch (Exception e){
					e.printStackTrace();
				}
			}
		};
		action1.setText("Connect to server...");
		action1.setToolTipText("Connect to server tooltip");		
		return action1;
	}
	
	private void showMessage(String message) {
		//swingMessage(message);
		
		MessageDialog.openInformation(
				getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getShell(),
			"GenePattern Information",
			message);
	}
	
	private boolean confirmMessage(String message) {
		//swingMessage(message);
		
		return MessageDialog.openConfirm(
				getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getShell(),
			"Please Confirm",
			message);
	}
	
	private String getServer() {
		
		InputDialog dlg = new InputDialog(getWorkbenchConfigurer().getWorkbench().getActiveWorkbenchWindow().getShell(), "Connect to server",
				"Please enter the URL of the server:", "localhost:8080", null) ;
		
		int okCancel = dlg.open();
		if (okCancel == InputDialog.CANCEL) return null;
		String newUrl = dlg.getValue();
		// System.out.println("Server = " + newUrl);
		return newUrl;
	}
	
	
	
}
