package edu.mit.broad.gp.gpge.views.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableTreeItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import edu.mit.broad.gp.core.GPGECorePlugin;
import edu.mit.broad.gp.gpge.GpgePlugin;
import edu.mit.broad.gp.gpge.JobSubmissionRegistry;
import edu.mit.broad.gp.gpge.job.JobEventListenerAdaptor;
import edu.mit.broad.gp.gpge.views.data.nodes.AbstractFileNode;
import edu.mit.broad.gp.gpge.views.data.nodes.FileNode;
import edu.mit.broad.gp.gpge.views.data.nodes.JobResultFileNode;
import edu.mit.broad.gp.gpge.views.data.nodes.JobResultNode;
import edu.mit.broad.gp.gpge.views.data.nodes.ProjectDirNode;
import edu.mit.broad.gp.gpge.views.data.nodes.ServerNode;
import edu.mit.broad.gp.gpge.views.data.nodes.SubDirNode;
import edu.mit.broad.gp.gpge.views.data.nodes.TreeNode;
import edu.mit.broad.gp.gpge.views.module.ModuleFormView;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.util.GPConstants;

/**
 *  * This sample class demonstrates how to plug-in a new workbench view. The view
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

public class DataView extends ViewPart {
    public static String ID_VIEW = "edu.mit.broad.gp.gpge.views.data.DataView";

    private TableTreeViewer tableTreeViewer;

    //private TableViewer tableTreeViewer;

    private Action openProjectDirAction;

    private Action removeProjectDirAction;;

    DataTreeModel treeModel = DataTreeModel.getInstance();

    private Action removeJobResultAction;

    private Action removeJobResultFileAction;

    private Action reloadAction;

    private Action saveAsAction;

    private IAction refreshAction;
    
    private IPropertyChangeListener projectDirListener = null;
    private IPropertyChangeListener projectSubDirListener = null;
    
    private MySorter tableSorter = null;

    /**
     * The constructor.
     */
    public DataView() {
    }

    private void setDragSource() {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        int operations = DND.DROP_COPY | DND.DROP_MOVE;

        final DragSource source = new DragSource(tableTreeViewer.getTableTree()
                .getTable(), operations);
        source.setTransfer(types);
        final TableTreeItem[] dragSourceItem = new TableTreeItem[1];

        source.addDragListener(new DragSourceListener() {
            public void dragStart(DragSourceEvent event) {
                TableTreeItem[] selection = tableTreeViewer.getTableTree()
                        .getSelection();
                //FIXME add check for type to prohibit dragging project dirs
                // e.g.
                if (selection.length > 0) { // && selection[0].getItemCount() ==
                    // 0) {
                    event.doit = true;
                    dragSourceItem[0] = selection[0];
                } else {
                    event.doit = false;
                }
            };

            public void dragSetData(DragSourceEvent event) {
                Object data = dragSourceItem[0].getData();
                if (data instanceof FileNode) {
                    FileNode node = (FileNode) data;
                    event.data = node.getFile().getPath();
                } else if (data instanceof JobResultFileNode) {
                    JobResultFileNode node = (JobResultFileNode) data;
                    event.data = node.getURL().toString();
                }
            }

            public void dragFinished(DragSourceEvent event) {
                //if (event.detail == DND.DROP_MOVE)
                //dragSourceItem[0].dispose();
                //dragSourceItem[0] = null;
            }
        });

    }

    class TableTreeContentProvider implements ITreeContentProvider {
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object parentElement) {
            TreeNode node = (TreeNode) parentElement;
            if (node == null) return new Object[0];
            return node.children();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
         */
        public Object getParent(Object element) {
            return ((TreeNode) (element)).parent();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
         */
        public boolean hasChildren(Object element) {
            return getChildren(element).length > 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }
    }

    class ViewLabelProvider extends LabelProvider implements
            ITableLabelProvider {
        public String getColumnText(Object obj, int index) {
            return ((TreeNode) (obj)).getColumnText(index);

        }
        
        
      
        
        public Image getColumnImage(Object obj, int index) {
            if (index == 2) return null;
            
            ImageRegistry imgReg = GpgePlugin.getDefault().getImageRegistry();
            Image img = null;
            
            if (obj instanceof SubDirNode){
                img = imgReg.get(GpgePlugin.IMG_OBJ_FOLDER);
            } else if ( obj instanceof ProjectDirNode ){
                img = imgReg.get(GpgePlugin.IMG_OBJ_ELEMENT);
            } else if ( obj instanceof AbstractFileNode){
                AbstractFileNode afn = (AbstractFileNode)obj;
                img = GpgePlugin.getDefault().getImageForFileType(afn.getFileExtension());
            } else if ( obj instanceof JobResultNode){
                img = imgReg.get(GpgePlugin.IMG_OBJ_FOLDER_OUTPUT);
            } else if ( obj instanceof ServerNode){
                img = imgReg.get(GpgePlugin.IMG_OBJ_SERVER);
            } else {	
                img = imgReg.get(GpgePlugin.IMG_OBJ_FILE);
            }
            return img;
        }

        
    }

    class MySorter extends ViewerSorter {
        private int column = 0;
        private boolean reverse = false;
        
        public void setSortColumn(int idx){
            if (column == idx) reverse = !reverse;
            else {
                column = idx;
                reverse=false;
            }
        }
        public int category(Object element) {
            System.out.println("CAT");
        	return 0;
        }
        public int compare(Viewer tableTreeViewer, Object e1, Object e2) {
            // TODO Auto-generated method stub
            TreeNode n1 = (TreeNode)e1;
        	TreeNode n2 = (TreeNode)e2;
        	int val = 0;
        	if (reverse){
        	    val = n2.getColumnText(column).compareTo(n1.getColumnText(column));
        	} else {
        	    val = n1.getColumnText(column).compareTo(n2.getColumnText(column));
        	}
        	return val;
        }

    }

    class ProjectDirListener implements IPropertyChangeListener{
    		public void propertyChange(PropertyChangeEvent event) {
        		if (event.getProperty().equals(GPGECorePlugin.PROJ_DIRS_PREFERENCE)) {
        			//Update the view by adding/removing new directories
        			String[] pds = GPGECorePlugin.getDefault().getProjectDirectories();
        			ArrayList pdirs = treeModel.getProjectDirNodes();
        			HashSet newPDirSet = new HashSet();
        			for (int i=0; i < pds.length;i++){
        		       	File newPDir = new File(pds[i]);
        		       	newPDirSet.add(newPDir);
        			}
        			//
        			// compare the new list of pdirs and what the prefs store says it should
        			// be.  Remove missing dirs and add new
        			//
     		     	for (Iterator iter = pdirs.iterator(); iter.hasNext(); ){
     		     		ProjectDirNode pdnode = (ProjectDirNode)iter.next();
     		     		File file = pdnode.getFile();
     		     		if (!newPDirSet.contains(file)){
     		     			treeModel.removeProjectDir(pdnode);
     		     		} else {
     		     			newPDirSet.remove(file);
     		     		}
     		     	}
     		     	for (Iterator iter = newPDirSet.iterator(); iter.hasNext(); ){
     		     		File pdir = (File)iter.next();
     		     		treeModel.addProjectDirectory(pdir);
     		     	}
        			
        		}
    		}
    }
    
    /**
     * ProjectSubDirListener
     * 
     * When the user decides they want to start/stop showing sub directories
     * in the dataview, we drop the project dirs and recreate them
     * @author genepattern
     *
     * TODO To change the template for this generated type comment go to
     * Window - Preferences - Java - Code Style - Code Templates
     */
    class ProjectSubDirListener implements IPropertyChangeListener{
		public void propertyChange(PropertyChangeEvent event) {
    		if (event.getProperty().equals(GPGECorePlugin.PROJ_DIRS_SUBDIR_PREFERENCE)) {
    			//Update the view by adding/removing new directories
    			String[] pds = GPGECorePlugin.getDefault().getProjectDirectories();
    			ArrayList pdirs = treeModel.getProjectDirNodes();
    			
    			//
    			// compare the new list of pdirs and what the prefs store says it should
    			// be.  Remove missing dirs and add new
    			//
 		     	for (Iterator iter = pdirs.iterator(); iter.hasNext(); ){
 		     		ProjectDirNode pdnode = (ProjectDirNode)iter.next();
 		     		treeModel.removeProjectDir(pdnode);
 		     		
 		     	}
 		     	for (int i=0; i < pds.length; i++ ){
 		     		File pdir = new File(pds[i]);
 		     		treeModel.addProjectDirectory(pdir);
 		     	}
    			
    		}
		}
}
    /**
     * This is a callback that will allow us to create the tableTreeViewer and
     * initialize it.
     */
    public void createPartControl(Composite parent) {
        SashForm sash_form = new SashForm(parent, SWT.HORIZONTAL | SWT.NULL);
        tableTreeViewer = new TableTreeViewer(sash_form, SWT.MULTI
                | SWT.H_SCROLL | SWT.V_SCROLL);
        tableTreeViewer.setContentProvider(new TableTreeContentProvider());
        tableTreeViewer.setLabelProvider(new ViewLabelProvider());
        
        tableSorter = new MySorter();
        tableTreeViewer.setSorter(tableSorter);
        tableTreeViewer.setInput(treeModel.getRoot());

        treeModel.addDataTreeModelListener(new DataTreeModelListener() {

            public void dataTreeChanged(DataTreeModelEvent event) {
               refreshViewer(event.getElement());

            }
        });
             
        // set up listeners for preference changes
        projectDirListener = new ProjectDirListener();
        GPGECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(projectDirListener);
        projectSubDirListener = new ProjectSubDirListener();
        GPGECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(projectSubDirListener);
       
        // set up listener for job submissions
        JobSubmissionRegistry.getDefault().addJobEventListener(
                new  JobEventListenerAdaptor() {
                    public void jobFinished(AnalysisJob job){
                        
                        treeModel.addJob(job.getTaskName(), job);
                        
            		  }
                }
        );
        
        setDragSource();
        final Action doubleClickAction = new Action() {
            public void run() {
                final TableTreeItem[] selection = tableTreeViewer
                        .getTableTree().getSelection();
                final Object data = selection[0].getData();
                new Thread() {
                    public void run() {
                        if (data instanceof FileNode) {
                            File f = ((FileNode) (data)).getFile();
                            org.eclipse.swt.program.Program.launch(f.getPath());
                        } else if (data instanceof JobResultFileNode) {
                            JobResultFileNode node = (JobResultFileNode) data;
                            try {
                                java.io.File f = new java.io.File(System
                                        .getProperty("java.io.tmpdir"), node
                                        .getFileName());
                                node.refresh();
                                if(!node.exists()) {
                                    node.removeFromParent();
                                    treeModel.nodeChanged(node.parent());
                                } else {
                                    node.downloadFile(f);
                                    org.eclipse.swt.program.Program.launch(f
                                        .getCanonicalPath());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }.start();

            }
        };

        tableTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent e) {
                doubleClickAction.run();
            }
        });

        SelectionListener colSelectionListener = new SelectionListener(){
            
            public int calculateColumnIndex(TableColumn col){
                TableColumn[] cols = tableTreeViewer.getTableTree().getTable().getColumns();
                for (int i=0; i < cols.length; i++){
                    if (col == cols[i]) return i;
                }
                return 0;
            }
            
            public void widgetSelected(SelectionEvent se){
                TableColumn col = (TableColumn)se.getSource();
                tableSorter.setSortColumn(calculateColumnIndex(col));
                tableTreeViewer.refresh();
            }
            public void widgetDefaultSelected(SelectionEvent se){
                widgetSelected(se);
            }
        };
        
        TableColumn column = new TableColumn(tableTreeViewer.getTableTree()
                .getTable(), SWT.LEFT);
        column.setText("Name");
        column.setWidth(120);

        column.addSelectionListener(colSelectionListener);
        
        TableColumn column2 = new TableColumn(tableTreeViewer.getTableTree()
                .getTable(), SWT.LEFT);
        column2.setText("Kind");
        column2.setWidth(75);
        column2.addSelectionListener(colSelectionListener);
        
        TableColumn column3 = new TableColumn(tableTreeViewer.getTableTree()
                .getTable(), SWT.LEFT);
        column3.setText("Date Modified");
        column3.setWidth(100);
        column3.addSelectionListener(colSelectionListener);
        
        tableTreeViewer.getTableTree().getTable().setHeaderVisible(true);

        tableTreeViewer.refresh();
        makeActions();
        hookContextMenu();
        
        // set the default project directories in
        String[] pds =  GPGECorePlugin.getDefault().getProjectDirectories();
        //GPGECorePlugin.getDefault().getPreferenceArray(GPGECorePlugin.PROJ_DIRS_PREFERENCE);
        for (int i=0; i < pds.length;i++){
        	treeModel.addProjectDirectory(new File(pds[i]));
        }
        
        JobSubmissionRegistry.getDefault().rememberOldJobs();
        
    }

    private void refreshViewer() {
        tableTreeViewer.getTableTree().getTable().getDisplay()
        .asyncExec(new Runnable() {
            public void run() {
                Object[] expanded = tableTreeViewer.getExpandedElements();
                tableTreeViewer.refresh();
                tableTreeViewer.setExpandedElements(expanded);
            }
        });
    }
    
    private void refreshViewer(final TreeNode node) {
        tableTreeViewer.getTableTree().getTable().getDisplay()
        .asyncExec(new Runnable() {
            public void run() {
                tableTreeViewer.refresh(node);
            }
        });
    }
    
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                // get selected row in table and customize pop-up menu
                DataView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(tableTreeViewer.getControl());
        menu.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event e) {
                //System.out.println("xxx");
            }
        });

        tableTreeViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, tableTreeViewer);
    }

    
    // pop-up menu
    private void fillContextMenu(IMenuManager manager) {
        manager.add(openProjectDirAction);
        TableTreeItem[] selection = tableTreeViewer.getTableTree()
        .getSelection();
        boolean projectDirNodeFound = false;
        boolean jobResultNodeFound = false;
        boolean jobResultFileNodeFound = false;
        removeJobResultAction.setText("Remove Job");
        if (selection != null && selection.length > 0) {
            for(int i = 0, length = selection.length; i < length; i++) {
                if(selection[i].getData() instanceof ProjectDirNode) {
                    projectDirNodeFound = true;
                }
                if(selection[i].getData() instanceof JobResultNode) {
                    if(jobResultNodeFound) {
                      removeJobResultAction.setText("Remove Jobs");
                    }
                    jobResultNodeFound = true;
                }
                if(selection[i].getData() instanceof JobResultFileNode) {
                    jobResultFileNodeFound = true;
                }
            }
            if(projectDirNodeFound) {
                manager.add(removeProjectDirAction);
            }
            if(projectDirNodeFound || jobResultNodeFound) {
                manager.add(refreshAction); 
            }
            if(jobResultNodeFound) {
                manager.add(removeJobResultAction);
                manager.add(reloadAction);
            }
            if(jobResultFileNodeFound) {
                manager.add(removeJobResultFileAction);
                manager.add(saveAsAction);
            }
        }
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    }

    /*
     * private void fillLocalToolBar(IToolBarManager manager) {
     * manager.add(openProjectDirAction); manager.add(action2); }
     */

private void makeActions() {
        removeProjectDirAction = new Action() {
            public void run() {

                ISelection selection = tableTreeViewer.getSelection();
                Object obj = ((IStructuredSelection) selection)
                        .getFirstElement();
                if (obj instanceof ProjectDirNode) {
                   ProjectDirNode node = (ProjectDirNode)obj;
                   treeModel.removeProjectDir(node);
                   GPGECorePlugin.getDefault().removeProjectDirectory(node.getFile().getAbsolutePath());
                }
            }
        };
        removeProjectDirAction.setText("Remove Project Directory");

        removeJobResultAction = new Action() {
            public void run() {
                TableTreeItem[] selection = tableTreeViewer.getTableTree()
                .getSelection();
                if(selection!=null && selection.length >0) {
                    for(int i = 0, length = selection.length; i < length; i++) {
                        if(selection[i].getData() instanceof JobResultNode) {
                            JobResultNode jrn = (JobResultNode) selection[i].getData();
                            JobSubmissionRegistry.getDefault().forget(jrn.getAnalysisJob());
                            treeModel.removeJobResult(jrn);
                          
                        }
                    }
                }
            }
        };
        removeJobResultAction.setText("Remove Job");

        removeJobResultFileAction = new Action() {
            public void run() {
                ISelection selection = tableTreeViewer.getSelection();
                Object obj = ((IStructuredSelection) selection)
                        .getFirstElement();
                if (obj instanceof JobResultFileNode) {
                    treeModel.removeFile((JobResultFileNode) obj);
                }
            }
        };

        removeJobResultFileAction.setText("Remove File");

        openProjectDirAction = new Action() {
            DirectoryDialog dialog = null;

            public void run() {
                if (dialog == null)
                    dialog = new DirectoryDialog(tableTreeViewer.getControl()
                            .getShell());
                dialog.setText("Select Project Directory");
                String projectDir = dialog.open();
                if (projectDir == null) {
                    return;
                }
                // FIXME add check to see if project dir already added
                treeModel.addProjectDirectory(new File(projectDir));
                GPGECorePlugin.getDefault().addProjectDirectory(projectDir);
            }
        };
        openProjectDirAction.setText("Open Project Directory");
        openProjectDirAction.setToolTipText("Action 1 tooltip");
        openProjectDirAction.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages().getImageDescriptor(
                        ISharedImages.IMG_OBJS_INFO_TSK));

        reloadAction = new Action() {

            public void run() {
                TableTreeItem[] selection = tableTreeViewer.getTableTree()
                        .getSelection();
                if (selection != null && selection.length > 0) {
                    if (selection[0].getData() instanceof JobResultNode) {
                        JobResultNode node = (JobResultNode) selection[0]
                                .getData();
                        AnalysisJob job = node.getAnalysisJob();
                        
                        ParameterInfo[] params = job.getJobInfo()
                                .getParameterInfoArray();
                        Map paramName2ValueMap = new HashMap();
                        for (int i = 0, length = params.length; i < length; i++) {
                            ParameterInfo param = params[i];
                            if (param.isOutputFile()) {
                                continue;
                            }
                            paramName2ValueMap.put(param.getName(), param
                                    .getValue());
                            if (param.isInputFile()) { // input file is local
                                // file
                                paramName2ValueMap.put(
                                        param.getName(),
                                        param.getAttributes().get(GPConstants.PARAM_INFO_CLIENT_FILENAME[0]));
                            }
                        }
                       
                        IViewSite viewSite = getViewSite();
                        IWorkbenchWindow wbw = viewSite.getWorkbenchWindow();
                        IWorkbenchPage page = wbw.getActivePage();
                        IViewPart[] views = page.getViews();
                        for(int i = 0; i < views.length; i++) {
                            if(views[i] instanceof ModuleFormView) {
                                String serverId = job.getServer().replace(':','-');
                            	
                                try {
                                    
                            	    page.showView(ModuleFormView.ID_VIEW,serverId,IWorkbenchPage.VIEW_ACTIVATE);
                            	} catch (Exception e){e.printStackTrace();}
                            	
                                if(page.isPartVisible(views[i])) {
                                    ModuleFormView view = (ModuleFormView) views[i];
                                    view.showModule(job.getTaskName(), paramName2ValueMap);
                            
                                }
                            }
 
                        }
                    }
                }
            }
        };
        reloadAction.setText("Reload");
        reloadAction.setToolTipText("Reload a job");
        
        saveAsAction = new Action() {
            FileDialog dialog;
            
            public void run() {
                TableTreeItem[] selection = tableTreeViewer.getTableTree()
                .getSelection();
                if (selection != null && selection.length > 0) {
                    if (selection[0].getData() instanceof JobResultFileNode) {
                        final JobResultFileNode node = (JobResultFileNode) selection[0].getData();
                        if (dialog == null) {
                            dialog = new FileDialog(tableTreeViewer.getControl()
                                    .getShell(), SWT.SAVE);
                        }
                        final String file = dialog.open();
                        if(file!=null) {
                                new Thread() {
                                    public void run() {
                                        try {
                                            node.downloadFile(new File(file));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                        }
                    }
                }
                 
            }
        };
        saveAsAction.setText("Save As...");
        saveAsAction.setToolTipText("Saves the file to the file system.");
        
        refreshAction = new Action() {
            public void run() {
                TableTreeItem[] selection = tableTreeViewer.getTableTree()
                .getSelection();
                if (selection != null && selection.length > 0) {
                    if (selection[0].getData() instanceof JobResultNode) {
                        // FIXME should we still display deleted files, just distinguish them somehow
                        ((JobResultNode) (selection[0].getData())).removeDeletedOutputFiles();
                        refreshViewer((TreeNode) selection[0].getData());
                    } else if (selection[0].getData() instanceof ProjectDirNode) {
                        refreshViewer((TreeNode) selection[0].getData());
                    }
                
            }
            }
        };
        refreshAction.setText("Refresh");
      

    }    
        private void showMessage(String message) {
        MessageDialog.openInformation(tableTreeViewer.getControl().getShell(),
                "Sample View", message);
    }

    /**
     * Passing the focus request to the tableTreeViewer's control.
     */
    public void setFocus() {
        tableTreeViewer.getControl().setFocus();
    }

    
    
}