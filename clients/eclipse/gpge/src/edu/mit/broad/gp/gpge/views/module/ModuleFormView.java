package edu.mit.broad.gp.gpge.views.module;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.xml.sax.SAXException;

import edu.mit.broad.gp.core.GPGECorePlugin;
import edu.mit.broad.gp.core.InstallTask;
import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.GpgePlugin;
import edu.mit.broad.gp.gpge.JobSubmissionRegistry;
import edu.mit.broad.gp.gpge.RcpWorkbenchAdvisor;
import edu.mit.broad.gp.gpge.job.JobEventListenerAdaptor;
import edu.mit.broad.gp.gpge.util.BrowserLauncher;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceErrorMessageException;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.util.GPConstants;

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

public class ModuleFormView extends ViewPart {
    public static final String ID_VIEW = "edu.mit.broad.gp.gpge.views.module.ModuleFormView";

    private DrillDownAdapter drillDownAdapter;

    private HashMap moduleForms = new HashMap();

    private ScrolledForm visibleForm = null;

    private ScrolledForm homeForm = null;

    private Color foreground = Display.getDefault().getSystemColor(
            SWT.COLOR_BLUE);

    private SashForm sash_form;

    private Action analysisMenuAction;

    private Action homeAction;

    private Action visualizerMenuAction;

    private ServiceManager serviceManager = null;

    /*
     * The content provider class is responsible for providing objects to the
     * view. It can wrap existing objects in adapters or simply return objects
     * as-is. These objects may be sensitive to the current input of the view,
     * or ignore it and always show the same content (like Task List, for
     * example).
     */

    class TreeObject implements IAdaptable {
        private TreeParent parent;

        public void setParent(TreeParent parent) {
            this.parent = parent;
        }

        public TreeParent getParent() {
            return parent;
        }

        public Object getAdapter(Class key) {
            return null;
        }
    }

    static class DownloadAdapter extends HyperlinkAdapter {
        FileDialog dialog;

        Shell shell;

        public DownloadAdapter(FileDialog dialog, Shell shell) {
            this.dialog = dialog;
            this.shell = shell;
        }

        public void linkActivated(HyperlinkEvent e) {
            java.net.URL url = null;
            try {
                url = new java.net.URL((String) e.getHref());
                download(url, null);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }

        }

        public void download(java.net.URL url, String suggestedFileName) {
            if (suggestedFileName == null) {
                String file = url.getFile();
                int slashIndex = file.lastIndexOf("/");
                if (slashIndex != -1) {
                    suggestedFileName = file.substring(slashIndex + 1, file
                            .length());
                }
            }
            dialog.setFileName(suggestedFileName);
            final String fileName = dialog.open();

            final URL _url = url;

            new Thread() {
                public void run() {
                    if (fileName != null) {
                        downloadFile(_url, fileName, shell);
                    }

                }
            }.start();

        }

    }

    class TreeLeaf extends TreeObject {
        private Object obj;

        public TreeLeaf(Object obj) {
            this.obj = obj;
        }

        public String getName() {
            return obj.toString();
        }

        public String toString() {
            return getName();
        }

    }

    class TreeParent extends TreeObject {
        private ArrayList children;

        private String name;

        public TreeParent(String name) {
            this.name = name;
            children = new ArrayList();
        }

        public void addChild(TreeObject child) {
            children.add(child);
            child.setParent(this);
        }

        public void removeChild(TreeObject child) {
            children.remove(child);
            child.setParent(null);
        }

        public TreeObject[] getChildren() {
            return (TreeObject[]) children.toArray(new TreeObject[children
                    .size()]);
        }

        public boolean hasChildren() {
            return children.size() > 0;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return getName();
        }

    }

    class TreeViewContentProvider implements IStructuredContentProvider,
            ITreeContentProvider {
        private TreeParent invisibleRoot;

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                if (invisibleRoot == null)
                    initialize();
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }

        public Object getParent(Object child) {
            if (child instanceof TreeObject) {
                return ((TreeObject) child).getParent();
            }
            return null;
        }

        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeParent) {
                return ((TreeParent) parent).getChildren();
            }
            return new Object[0];
        }

        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeParent)
                return ((TreeParent) parent).hasChildren();
            return false;
        }

        /*
         * We will set up a dummy model to initialize tree heararchy. In a real
         * code, you will connect to a real model and expose its hierarchy.
         */
        private void initialize() {
            File root = new File("e:/gp2/workshop/datasets/all_aml");
            TreeParent p1 = new TreeParent(root.getName());
            HashMap categories = new HashMap();

            File[] children = root.listFiles();

            for (int i = 0; i < children.length; i++) {
                File kid = children[i];
                if (kid.isDirectory()) {
                    TreeParent aFolder = new TreeParent(kid.getName());
                    p1.addChild(aFolder);
                } else {
                    TreeObject aFile = new TreeLeaf(kid);
                    p1.addChild(aFile);
                }
            }

            invisibleRoot = new TreeParent("");
            invisibleRoot.addChild(p1);
        }
    }

    class TreeViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {

            return obj.toString();
        }

        public Image getImage(Object obj) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (obj instanceof TreeParent)
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            return PlatformUI.getWorkbench().getSharedImages().getImage(
                    imageKey);
        }
    }

    class NameSorter extends ViewerSorter {
    }

    class TableViewLabelProvider extends LabelProvider implements
            ITableLabelProvider, IColorProvider {
        public String getColumnText(Object obj, int index) {
            // modify to retrieve appropriate label for this column
            return obj.toString();
        }

        public Image getColumnImage(Object obj, int index) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(
                    ISharedImages.IMG_TOOL_FORWARD);
        }

        public Color getForeground(Object element) {
            if (element.toString().endsWith("res"))
                return Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
            return null; // Always use default.
        }

        /** @see IColorProvider#getBackground(Object) */
        public Color getBackground(Object element) {
            if (element.toString().endsWith("res"))
                return (foreground);
            else
                return null; // default
        }
    }

    class TableViewContentProvider implements IStructuredContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            System.out.println("Input changed " + oldInput + " to " + newInput);

        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            if (parent.getClass() != TreeLeaf.class)
                return new String[0];
            TreeLeaf leaf = (TreeLeaf) parent;

            String[] params = new String[1];
            params[0] = leaf.getName();

            return params;
        }
    }

    /**
     * The constructor.
     */
    public ModuleFormView() {
    }

    public static void downloadFile(URL url, String fileName, Shell shell) {
        java.io.FileOutputStream fos = null;
        java.io.InputStream is = null;
        try {
            java.io.File f = new java.io.File(fileName);
            /*
             * int counter = 1; while(f.exists()) { int dotIndex =
             * fileName.lastIndexOf("."); if(dotIndex > 0) { String baseName =
             * fileName.substring(0, dotIndex); String extension =
             * fileName.substring(dotIndex, fileName.length()); f = new
             * java.io.File(baseName + "-" + counter + extension); } else { f =
             * new java.io.File(fileName + counter); } }
             */

            java.io.File tempFile = new java.io.File(f.getName() + ".part");
            fos = new java.io.FileOutputStream(tempFile);

            java.net.URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            byte[] b = new byte[10000];
            int bytesRead = -1;
            while ((bytesRead = is.read(b, 0, b.length)) != -1) {
                fos.write(b, 0, bytesRead);
            }
            fos.close();
            tempFile.renameTo(f);
        } catch (java.io.IOException e1) {
            MessageDialog
                    .openError(shell, "GenePattern",
                            "An error occurred while downloading the file, "
                                    + fileName);
            e1.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }

        }

    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    private SashForm secondSash = null;

    private FileDialog fileSaveDialog;

    private FileDialog fileOpenDialog;

    private void createModulesSection(FormToolkit toolkit) {
        
        Section modulesSection = toolkit.createSection(homeForm.getBody(),
                Section.DESCRIPTION | Section.TWISTIE | Section.EXPANDED);
        modulesSection.setText("Modules");
 
        Composite exportCompositeClient = toolkit.createComposite(modulesSection);
        modulesSection.setClient(exportCompositeClient);
        GridLayout exportLayout = new GridLayout();
        exportLayout.numColumns = 2;
        exportCompositeClient.setLayout(exportLayout);
        final Combo tasksCombo = new Combo(exportCompositeClient, SWT.DROP_DOWN | SWT.READ_ONLY);
        java.util.List tasks = new ArrayList();
      
        for (Iterator iter = serviceManager.getServiceMap().keySet().iterator(); iter.hasNext();) {
            tasks.add(iter.next());
        }
        java.util.Collections.sort(tasks, String.CASE_INSENSITIVE_ORDER);
        tasksCombo.setItems((String[])tasks.toArray(new String[0])); 
        tasksCombo.select(0);
        
        
        Composite c = toolkit.createComposite(exportCompositeClient);
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        c.setLayout(gl);
        Hyperlink exportLink = toolkit.createHyperlink(c, "Export",
                SWT.WRAP);
        HyperlinkAdapter exportAdapter = new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                
                    int selection = tasksCombo
                    .getSelectionIndex();
                    if(selection!=-1) {
                        final String task = tasksCombo.getItem(selection);
                        fileSaveDialog.setFileName(task + ".zip");
                        final String s = fileSaveDialog.open();
                        new Thread() {
                            public void run() {                
                                if(s!=null) {
                                    try {
                                        File f = new File(s);
                                        serviceManager.getTaskIntegratorProxy().exportToZip(task, f);  
                                        showStatus("Downloaded " + task + " to " + f.getName());
                                    } catch (WebServiceException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                                    
                            }
                        }.start();
                    }
                

            }
        };
        exportLink.addHyperlinkListener(exportAdapter);
        
        Hyperlink editLink = toolkit.createHyperlink(c, "Edit",
                SWT.WRAP);
        HyperlinkAdapter editAdapter = new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                
                    int selection = tasksCombo
                    .getSelectionIndex();
                    if(selection!=-1) {
                        final String task = tasksCombo.getItem(selection);
                        String serializedModel = (String) serviceManager.getService(task).getTaskInfo().getTaskInfoAttributes().get(GPConstants.SERIALIZED_MODEL);
                       
                        if(serializedModel!=null && !serializedModel.trim().equals("")) {
                            try {
                                PipelineModel pipeline = PipelineModel.toPipelineModel(serializedModel);
                                displayForm(new PipelineEditorForm(sash_form, pipeline, serviceManager));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                            
                        } else {
                            displayForm(new ModuleEditorForm(sash_form, task, serviceManager));
                        }
                           
                    }
                

            }
        };
        editLink.addHyperlinkListener(editAdapter);
        
        
       
        
        Hyperlink createTaskLink = toolkit.createHyperlink(exportCompositeClient, "Create Task",
                SWT.WRAP);
        HyperlinkAdapter createTaskAdapter = new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                displayForm(new ModuleEditorForm(sash_form, null, serviceManager));
            }
        };
        createTaskLink.addHyperlinkListener(createTaskAdapter);
        
        Hyperlink createPipelineLink = toolkit.createHyperlink(exportCompositeClient, "Create Pipeline",
                SWT.WRAP);
        HyperlinkAdapter createPipelineAdapter = new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                displayForm(new PipelineEditorForm(sash_form, null, serviceManager));
                  
            }
        };
        createPipelineLink.addHyperlinkListener(createPipelineAdapter);
        
        Hyperlink importLink = toolkit.createHyperlink(exportCompositeClient, "Import",
                SWT.WRAP);
        HyperlinkAdapter importAdapter = new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                    int selection = tasksCombo
                    .getSelectionIndex();
                    if(selection!=-1) {
                        final String task = tasksCombo.getItem(selection);
                        final String s = fileOpenDialog.open();
                        new Thread() {
                            public void run() {                
                                if(s!=null) {
                                    try {
                                        serviceManager.getTaskIntegratorProxy().importZip(new File(s), GPConstants.ACCESS_PUBLIC);     
                                        showStatus("Imported " + task);
                                    } catch(WebServiceException e) {
                                        showError(null, e.getErrors());
                                    } catch (WebServiceException e1) {
                                        showError(null, e1.getMessage());
                                    }
                                }
                                    
                            }
                        }.start();
                    }
                

            }
        };
        importLink.addHyperlinkListener(importAdapter);
        
        
        
        
        Hyperlink reposLink = toolkit.createHyperlink(exportCompositeClient, "Broad Module Repository",
                SWT.WRAP);
        reposLink.addHyperlinkListener(new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                
            		
                try {
                    InstallTask[] tasks;
                    System.out.println("Available tasks");
                    tasks = edu.mit.broad.gp.core.ModuleRepository.getModules(serviceManager);
                    for(int i = 0; i < tasks.length; i++) {
                        System.out.println(tasks[i].getName());
                    }
                } catch (Throwable e1) {
                    e1.printStackTrace();
                }
               
            }
            
        });
        
    }
    
    void displayForm(ScrolledForm form) {
        if (visibleForm != null) {
            visibleForm.setVisible(false);
            if(visibleForm instanceof ModuleEditorForm || visibleForm instanceof PipelineEditorForm) {
                visibleForm.dispose();
            }
        }
        visibleForm = form;
        visibleForm.setVisible(true);
        sash_form.layout(true);
    }
    
    void showError(final Shell shell, final String s) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                MessageDialog.openError(shell, "Error", s);
            }

        });
    }
    
    void showError(final Shell shell, final java.util.List errors) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                StringBuffer buf = new StringBuffer();
                for(int i = 0; i < errors.size(); i++) {
                    buf.append(errors.get(i));
                }
                MessageDialog.openError(shell, "Error", buf.toString());
            }

        });
    }
    
    private void showStatus(final String s) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                getViewSite().getActionBars()
                        .getStatusLineManager().setMessage(s);
            }

        });
        
    }
    public void createPartControl(final Composite parent) {
        String serverId = this.getViewSite().getSecondaryId();
        fileSaveDialog = new FileDialog(parent.getShell(), SWT.SAVE);
        fileSaveDialog.setText("Save As");
        
        fileOpenDialog = new FileDialog(parent.getShell(), SWT.OPEN);
        fileOpenDialog.setText("Open");
        // XXX Must tie this into the preferences
        if (serverId == null)
            serverId = "elm:8080";
        serviceManager = new ServiceManager(serverId, "");
        this.setPartName(serviceManager.getServerName());

        final String serverName = serviceManager.getServerName();
        sash_form = new SashForm(parent, SWT.HORIZONTAL | SWT.NULL);

        FormToolkit toolkit = new FormToolkit(sash_form.getDisplay());
        homeForm = toolkit.createScrolledForm(sash_form);
        homeForm.setText("GenePattern Server: " + serverName);
        visibleForm = homeForm;
        homeForm.setVisible(true);
        homeForm.getBody().setLayout(new GridLayout());

        FormText txt = toolkit.createFormText(homeForm.getBody(), true);
        txt
                .setText(
                        "Select a data analysis module or visualizer from the menus to start.",
                        false, true);

        
       
		
		    
		Hyperlink refreshLink = toolkit.createHyperlink(homeForm.getBody(), "Refresh Tasks",
	                SWT.WRAP);
	    
		refreshLink.addHyperlinkListener(new HyperlinkAdapter() {
	      public void linkActivated(HyperlinkEvent e) {
	          refresh = true;
	          RcpWorkbenchAdvisor.getInstance().refreshServer(ModuleFormView.this, serverName);
	      }
		});
		
		Hyperlink viewTomcatLogLink = toolkit.createHyperlink(homeForm.getBody(), "Tomcat Log",
                SWT.WRAP);
		viewTomcatLogLink.addHyperlinkListener(new HyperlinkAdapter() {
		    public void linkActivated(HyperlinkEvent e) {
     
		        try {
		            File tomcatFile = new File(System.getProperty("java.io.tmpdir"), "tomcat_log.txt");
		            serviceManager.getTaskIntegratorProxy().getTomcatLog(tomcatFile);  
		            Program.launch(tomcatFile.getCanonicalPath());
		        } catch (WebServiceException e1) {
		            e1.printStackTrace();
		        } catch (IOException e2) {
                    e2.printStackTrace();
                }
		    }
		});
		
		Hyperlink viewGenePatternLogLink = toolkit.createHyperlink(homeForm.getBody(), "GenePattern Log",
                SWT.WRAP);
		viewGenePatternLogLink.addHyperlinkListener(new HyperlinkAdapter() {
		    public void linkActivated(HyperlinkEvent e) {
		        try {
		            File gpFile = new File(System.getProperty("java.io.tmpdir"), "GenePattern_log.txt");
		            serviceManager.getTaskIntegratorProxy().getGenePatternLog(gpFile);  
		            Program.launch(gpFile.getCanonicalPath());
		        } catch (WebServiceException e1) {
		            e1.printStackTrace();
		        } catch (IOException e2) {
                    e2.printStackTrace();
                }
		    }
		});
	
		
		createModulesSection(toolkit);
        Section docSection = toolkit.createSection(homeForm.getBody(),
                Section.DESCRIPTION | Section.TWISTIE | Section.EXPANDED);
        docSection.setText("Documentation");

        Composite docSectionClient = toolkit.createComposite(docSection);
        docSectionClient.setLayout(new GridLayout());
        docSection.setClient(docSectionClient);

        if ("macosx".equals(System.getProperty("osgi.os"))) {
            Color black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
            homeForm.setForeground(black);
        }

        ImageHyperlink linkPub = toolkit.createImageHyperlink(docSectionClient,
                SWT.WRAP);
        linkPub.setImage(GpgePlugin.getDefault().getImageRegistry().get(
                GpgePlugin.IMG_OBJ_DOC));
        linkPub.setText("Task Documentation for GenePattern server "
                + serverName);
        linkPub.setHref("http://" + serverName + "/gp/getTaskDoc.jsp");
        linkPub.addHyperlinkListener(new HyperlinkAdapter() {
            public void linkActivated(HyperlinkEvent e) {
                try {
                    String url = (String) e.getHref();
                    BrowserLauncher.openURL(url);
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        });

        try {
            Section programmingSection = toolkit.createSection(homeForm
                    .getBody(), Section.DESCRIPTION | Section.TWISTIE
                    | Section.EXPANDED);
            programmingSection.setText("Programming");

            Composite programmingSectionClient = toolkit
                    .createComposite(programmingSection);
            GridLayout gl = new GridLayout();
            gl.numColumns = 4;
            programmingSectionClient.setLayout(gl);
            programmingSection.setClient(programmingSectionClient);

            Label l = toolkit.createLabel(programmingSectionClient,
                    "GenePattern Library", SWT.WRAP);
            GridData gridData = new GridData();
            gridData.horizontalSpan = 4;
            l.setLayoutData(gridData);
            toolkit.createLabel(programmingSectionClient, "Java", SWT.WRAP);

            Hyperlink javaZip = toolkit.createHyperlink(
                    programmingSectionClient, "zip", SWT.WRAP);
            javaZip.setHref("http://" + serverName
                    + "/gp/downloads/GenePattern.zip");

            
            DownloadAdapter downloadAdapter = new DownloadAdapter(fileSaveDialog,
                    parent.getShell());

            javaZip.addHyperlinkListener(downloadAdapter);
            gridData = new GridData();
            gridData.horizontalSpan = 3;
            javaZip.setLayoutData(gridData);

            toolkit.createLabel(programmingSectionClient, "R", SWT.WRAP);

            Hyperlink rZip = toolkit.createHyperlink(programmingSectionClient,
                    "zip", SWT.WRAP);
            rZip.setHref("http://" + serverName
                    + "/gp/downloads/GenePattern_0.1-0.zip");
            rZip.addHyperlinkListener(downloadAdapter);

            Hyperlink rGzip = toolkit.createHyperlink(programmingSectionClient,
                    "tar.gz", SWT.WRAP);
            rGzip.setHref("http://" + serverName
                    + "/gp/downloads/GenePattern_0.1-0.tar.gz");
            rGzip.addHyperlinkListener(downloadAdapter);

            Hyperlink rSource = toolkit.createHyperlink(
                    programmingSectionClient, "source", SWT.WRAP);
            rSource.setHref("http://" + serverName + "/gp/GenePattern.R");
            rSource.addHyperlinkListener(downloadAdapter);

            //PipelineCodeGenerator generator =
            // PipelineCodeGeneratorFactory.createGenerator(language);
            //java.io.PrintStream ps = new java.io.PrintStream(new
            // java.io.FileOutputStream(null));
            // generator.getCode(pipelineModel, server, "GenePattern", ps); //
            // XXX username is incorrect

            l = toolkit.createLabel(programmingSectionClient, "Pipeline code",
                    SWT.WRAP);
            gridData = new GridData();
            gridData.horizontalSpan = 4;
            l.setLayoutData(gridData);

            final Combo modulesCombo = new Combo(programmingSectionClient,
                    SWT.DROP_DOWN | SWT.READ_ONLY);
            java.util.List values = new ArrayList();
            Map services = serviceManager.getServiceMap();
            for (Iterator iter = services.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                AnalysisService service = (AnalysisService) services.get(key);
                TaskInfo tinfo = service.getTaskInfo();
                Map tia = tinfo.getTaskInfoAttributes();
                String serializedModel = (String) tia
                        .get(GPConstants.SERIALIZED_MODEL);

                if ((serializedModel != null)
                        && (serializedModel.trim().length() > 0)) { // pipeline
                    values.add(key);
                }
            }
            java.util.Collections.sort(values, String.CASE_INSENSITIVE_ORDER);
            modulesCombo.setItems((String[]) values.toArray(new String[values
                    .size()]));
            modulesCombo.select(0);

            final Hyperlink javaPipelineCodeLink = toolkit.createHyperlink(
                    programmingSectionClient, "Java", SWT.WRAP);
            javaPipelineCodeLink.setHref("http://" + serverName
                    + "/gp/getPipelineCode.jsp?language=Java&download=1&name=");

            final Hyperlink rPipelineCodeLink = toolkit.createHyperlink(
                    programmingSectionClient, "R", SWT.WRAP);
            rPipelineCodeLink.setHref("http://" + serverName
                    + "/gp/getPipelineCode.jsp?language=R&download=1&name=");

            DownloadAdapter pipelineAdapter = new DownloadAdapter(fileSaveDialog,
                    parent.getShell()) {
                public void linkActivated(HyperlinkEvent e) {
                    java.net.URL url = null;
                    try {
                        int selection = modulesCombo
                        .getSelectionIndex();
                        if(selection!=-1) {
                        String fileName = modulesCombo.getItem(selection);
                        url = new java.net.URL((String) e.getHref() + fileName);
                        if (e.getSource() == rPipelineCodeLink) {
                            fileName += ".R";
                        } else {
                            fileName = fileName.replace('.', '_');
                            fileName += ".java";
                        }
                        download(url, fileName);
                        }
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    }

                }
            };
            javaPipelineCodeLink.addHyperlinkListener(pipelineAdapter);
            rPipelineCodeLink.addHyperlinkListener(pipelineAdapter);

            createModuleForms(parent);
            makeActions();
            contributeToActionBars();
            
            JobSubmissionRegistry.getDefault().addJobEventListener(
                    new JobEventListenerAdaptor() {
                        // XXX This is the code that is updating the status line
                        // with the running job status
                        // 
                        public void jobStatusChange(final JobInfo ji) {
                            Display.getDefault().asyncExec(new Runnable() {
                                public void run() {
                                    getViewSite().getActionBars()
                                            .getStatusLineManager().setMessage(
                                                    "Job " + ji.getJobNumber()
                                                            + " "
                                                            + ji.getStatus());
                                }

                            });

                        }
                    });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void makeActions() {

        homeAction = new Action() {
            public void run() {
                if (visibleForm != null)
                    visibleForm.setVisible(false);
                visibleForm = homeForm;
                visibleForm.setVisible(true);
                sash_form.layout(true);
            }
        };
        homeAction.setText("Home");
        homeAction
                .setToolTipText("Return the the welcome page for this server");

        analysisMenuAction = new Action() {
            public void run() {
                this.getMenuCreator().getMenu(getSite().getShell()).setVisible(
                        true);
            }
        };
        analysisMenuAction.setText("Data Analysis");
        analysisMenuAction.setToolTipText("Data Analysis menu");

        analysisMenuAction.setMenuCreator(new IMenuCreator() {
            public void dispose() {
                // TODO Auto-generated method stub
            }

            public Menu getMenu(Control parent) {
                // TODO Auto-generated method stub
                return createAnalysisModuleMenu(parent);
            }

            public Menu getMenu(Menu parent) {
                // TODO Auto-generated method stub
                return null;
            }
        });
        visualizerMenuAction = new Action() {
            public void run() {

                this.getMenuCreator().getMenu(getSite().getShell()).setVisible(
                        true);
            }
        };
        visualizerMenuAction.setText("Visualizers");
        visualizerMenuAction.setToolTipText("Visualizer Menu");

        visualizerMenuAction.setMenuCreator(new IMenuCreator() {
            public void dispose() {
                // TODO Auto-generated method stub
            }

            public Menu getMenu(Control parent) {
                // TODO Auto-generated method stub
                return createVisualizerModuleMenu(parent);
            }

            public Menu getMenu(Menu parent) {
                // TODO Auto-generated method stub
                return null;
            }
        });

    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        //fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(homeAction);
        manager.add(new Separator());
        manager.add(analysisMenuAction);
        manager.add(visualizerMenuAction);
        manager.add(new Separator());
        //drillDownAdapter.addNavigationActions(manager);
    }

    public void createModuleForms(Composite parent) {
        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        Map services = serviceManager.getServiceMap();
        for (Iterator iter = services.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            AnalysisService service = (AnalysisService) services.get(key);
            //System.out.println("Form for:" + service.getURL());
            ModuleForm form = new ModuleForm(sash_form, serviceManager, service);
            form.createPartsForModule(toolkit, homeForm);

            moduleForms.put(key, form);
        }
    }

    public void showModule(String name, Map reloadParams) {
        showModule(name);
        java.util.HashMap controlMap = (java.util.HashMap) visibleForm
                .getData("controlMap");
        for (Iterator iter = controlMap.keySet().iterator(); iter.hasNext();) {
            String pname = (String) iter.next();
            Control control = (Control) controlMap.get(pname);
            String reloadValue = (String) reloadParams.remove(pname);
            if (reloadValue != null) {
                if (control.getClass() == Text.class) {
                    ((Text) control).setText(reloadValue);
                } else {
                    Combo cmb = (Combo) control;
                    int index = cmb.indexOf(reloadValue);
                    if (index != -1) {
                        cmb.select(index);
                    }
                }
            }
        }
        // what's left in reloadParams are parameters that have been removed
        if (reloadParams.size() != 0) {
            StringBuffer message = new StringBuffer();
            message.append("Ignoring the following unused parameters: ");
            for (Iterator iter = reloadParams.keySet().iterator(); iter
                    .hasNext();) {
                String param = (String) iter.next();
                message.append(param);
                message.append(" ");
            }
            MessageDialog.openInformation(null, "Reload " + name, message
                    .toString()); // FIXME don't use null here
        }

    }

  
    public void showModule(String name) {
        displayForm((ScrolledForm) moduleForms.get(name));
    }

    private void fillContextMenu(IMenuManager manager) {

        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void showMessage(String message) {
        //swingMessage(message);
        MessageDialog.openInformation(getViewSite().getShell(), "Two View",
                message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        sash_form.setFocus();
        //RcpWorkbenchAdvisor.setTreeViewer(this);
        this.setPartName(serviceManager.getServerName());

    }

    public Action getModuleSelectionAction(AnalysisService service) {
        Action action = new Action() {
            public void run() {
                showModule(this.getId());
            }
        };
        TaskInfo ti = service.getTaskInfo();
        action.setText(ti.getName());
        action.setId(ti.getName());
        action.setToolTipText("Action 2 tooltip");
        return action;
    }

    MenuManager daMenu;

    private Menu createAnalysisModuleMenu(Object parent) {
        //	if (parent instanceof Menu){
        //		MenuItem [] menuItems = ((Menu)parent).getItems ();
        //		for (int i=0; i<menuItems.length; i++) {
        //			menuItems [i].dispose ();
        //		}
        //	}

        daMenu = new MenuManager("Data Analysis",
                IWorkbenchActionConstants.M_WINDOW);
        Map catMap = serviceManager.getCategoryMap();

        for (Iterator iter = catMap.keySet().iterator(); iter.hasNext();) {
            String catname = (String) iter.next();
            if (!("Visualizer".equalsIgnoreCase(catname))) {
                //menu.add(new Separator());
                MenuManager catMenu = new MenuManager(catname, catname); //$NON-NLS-1$ $NON-NLS-2$
                daMenu.add(catMenu);

                Vector services = (Vector) catMap.get(catname);

                for (Iterator sIter = services.iterator(); sIter.hasNext();) {
                    AnalysisService svc = (AnalysisService) sIter.next();
                    //System.out.println("Menu for:" + svc.getURL());

                    Action action = this.getModuleSelectionAction(svc);
                    catMenu.add(action);
                }
            }
        }

        daMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        if (parent instanceof Menu) {
            daMenu.fill((Menu) parent, 0);
        } else if (parent instanceof Control) {
            daMenu.createContextMenu((Control) parent);
        }

        return daMenu.getMenu();

    }

    MenuManager visualizerMenu;

    /** Whether a refresh is currently in progress */
    private boolean refresh = false;

    private Menu createVisualizerModuleMenu(Object parent) {

        MenuManager visualizerMenu = new MenuManager("Visualizer",
                IWorkbenchActionConstants.M_WINDOW);
        Map catMap = serviceManager.getCategoryMap();
        Vector services = serviceManager.getServicesInCategory("Visualizer");

        for (Iterator sIter = services.iterator(); sIter.hasNext();) {
            AnalysisService svc = (AnalysisService) sIter.next();
            Action action = this.getModuleSelectionAction(svc);
            visualizerMenu.add(action);
        }

        visualizerMenu
                .add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        if (parent instanceof Menu) {
            visualizerMenu.fill((Menu) parent, 0);
        } else if (parent instanceof Control) {
            visualizerMenu.createContextMenu((Control) parent);
        }

        return visualizerMenu.getMenu();

    }

    public void dispose() {
        super.dispose();
        if (!refresh && !GpgePlugin.isShutdownInProgress()) {
            boolean remove = MessageDialog.openQuestion(this.getViewSite()
                    .getShell(), "Please Confirm", "Do you want to remove "
                    + this.getPartName() + " from your server preferences?");

            if (remove) {
                GPGECorePlugin.getDefault().removeFromPreferenceArray(
                        GPGECorePlugin.SERVERS_PREFERENCE, this.getPartName());
            }
        } 
        refresh = false; 
    }

}