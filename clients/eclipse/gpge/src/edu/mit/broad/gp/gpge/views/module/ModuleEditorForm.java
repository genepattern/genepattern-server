/*
 * Created on Aug 5, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.RcpWorkbenchAdvisor;
import edu.mit.genome.gp.ui.analysis.AnalysisService;
import edu.mit.wi.omnigene.framework.analysis.ParameterInfo;
import edu.mit.wi.omnigene.framework.analysis.TaskInfo;
import edu.mit.wi.omnigene.framework.webservice.WebServiceErrorMessageException;
import edu.mit.wi.omnigene.framework.webservice.WebServiceException;
import edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask;
import edu.mit.genome.util.GPConstants;

public class ModuleEditorForm extends ScrolledForm {
    private Map controlMap = new HashMap();
    private ServiceManager serviceManager;
    private FileDialog fileOpenDialog;
    private List addedFiles = new ArrayList();
    private Combo supportFilesCombo;
    private Composite parameterComposite;
    private ArrayList parameters = new ArrayList();
    private TaskInfo taskInfo;
    private String taskName;
    protected ArrayList deletedFiles = new ArrayList();
    
    public ModuleEditorForm(Composite parent, String taskName,
            final ServiceManager serviceManager) {
        super(parent);
        this.taskName = taskName;
        if(taskName!=null) {
            this.taskInfo = serviceManager.getService(taskName).getTaskInfo();
        } else {
            taskInfo = new TaskInfo();
            taskInfo.setTaskInfoAttributes(new HashMap());
        }
        fileOpenDialog = new FileDialog(parent.getShell(), SWT.OPEN |SWT.MULTI);
        this.serviceManager = serviceManager;
        this
                .setBackground(Display.getDefault().getSystemColor(
                        SWT.COLOR_WHITE));

        FormToolkit toolkit = new FormToolkit(parent.getDisplay());
        this.setText("Module Editor");
        if ("macosx".equals(System.getProperty("osgi.os"))) {
            Color black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
            this.setForeground(black);
        }
        GridLayout layout = new GridLayout();

        this.getBody().setLayout(layout);
        setVisible(true);
        createTaskDefinitionSection(toolkit, this.getBody());
        createParameterSection(toolkit, this.getBody());
        Composite buttonComposite = toolkit.createComposite(this.getBody());
        GridLayout buttonLayout = new GridLayout();
        buttonLayout.numColumns = 2;
        buttonComposite.setLayout(buttonLayout);
        Button saveButton = toolkit.createButton(buttonComposite, "Save",
                SWT.BORDER);
        saveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                save();
            }
        });

    }

     void save() {
        String privacyString = getValue("Privacy:");
        int accessId = GPConstants.ACCESS_PRIVATE;
        if("public".equals(privacyString)) {
            accessId = GPConstants.ACCESS_PUBLIC;
        }
            
        String taskName = getValue("Name:");
        String taskDescription = getValue("Description:");
        Map taskAttributes = new HashMap();
        String commandLine = getValue("Command Line:");
        if(commandLine==null) {
            ; //showError("Command line is not defined.");
        }
        taskAttributes.put(GPConstants.COMMAND_LINE,
                commandLine);
        taskAttributes.put(GPConstants.TASK_TYPE,
                getValue("Task type:"));
        taskAttributes.put(GPConstants.CLASSNAME,
                "edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask");
        taskAttributes.put(GPConstants.CPU_TYPE,
                getValue("CPU type:"));
        taskAttributes.put(GPConstants.OS,
                getValue("Operating System:"));
     //   taskAttributes.put(GPConstants.JVM_LEVEL,
           //     getValue("JVM Level:"));
        taskAttributes.put(GPConstants.LANGUAGE,
                getValue("Language:"));
        taskAttributes.put(GPConstants.VERSION,
                getValue("Version:"));
        
        taskAttributes.put(GPConstants.AUTHOR,
                getValue("Author:"));
         
        taskAttributes.put(GPConstants.USERID,
                getValue("Owner:"));
        taskAttributes.put(GPConstants.PRIVACY,
                getValue("Privacy:"));
        taskAttributes.put(GPConstants.QUALITY,
                getValue("Quality Level:"));
       // taskAttributes.put(GPConstants.PIPELINE_SCRIPT,
         //       getValue("Quality Level:"));
        // taskAttributes.put(GPConstants.LSID,
        //       getValue("Quality Level:"));
        // taskAttributes.put(GPConstants.SERIALIZED_MODEL,
        //       getValue("Quality Level:"));
        
       
        

        File[] supportFiles = (File[]) addedFiles.toArray(new File[0]);
        List parameterInfoList = new ArrayList();
        
        for(int i = 0, size = parameters.size(); i < size; i++) {
            ParameterInput p = (ParameterInput) parameters.get(i);
            p.validate();
            
            if(!p.isEmpty()) {
                if(!p.isValid()) {
                   System.out.println("Missing required information for parameters");
                   continue; // FIXME
                }
             
                String name = p.getValue("name");
                String desc = p.getValue("description");
                String choices = p.getValue("choices");
                String defaultValue = p.getValue("default");
                String optional = p.getValue("optional");
                if(optional.equals("yes")) {
                    optional = "on";
                } else {
                    optional = "";
                }
                String type = p.getValue("types");
                
                HashMap attr = new HashMap();
                ParameterInfo param = new ParameterInfo(name, choices, desc);
                  
                attr.put(GPConstants.PARAM_INFO_OPTIONAL[0], optional);
                attr.put(GPConstants.PARAM_INFO_DEFAULT_VALUE[0], defaultValue);
                

                if(type.equals("input file")) {
                    type = "java.io.File";
                    param.setAsInputFile();
                } else if(type.equals("text")) {
                    type = "java.lang.String";
                } else if(type.equals("floating point")) {
                    type = "java.lang.Float";
                }  else if(type.equals("integer")) {
                    type = "java.lang.Integer";
                }
                attr.put(GPConstants.PARAM_INFO_TYPE[0], type);
                param.setAttributes(attr);
                parameterInfoList.add(param);
                
            }
        }
        
        ParameterInfo[] parameterInfoArray = (ParameterInfo[]) parameterInfoList.toArray(new ParameterInfo[0]);
              try {
                serviceManager.getTaskIntegratorProxy().modifyTask(
                        accessId, taskName, taskDescription,
                        parameterInfoArray, taskAttributes, supportFiles);
                
                if(deletedFiles.size() >0) {
                    serviceManager.getTaskIntegratorProxy().deleteFiles(taskName, (String[])deletedFiles.toArray(new String[0]));
                }
                deletedFiles.clear();
                addedFiles.clear();
//              FIXME show message in status bar
            } catch (WebServiceException e) {
                e.printStackTrace();
                Throwable rc = e.getRootCause();
                if(rc instanceof WebServiceErrorMessageException) {
                    System.out.println(((WebServiceErrorMessageException)rc).getErrors());
                }
            }
        
        
    }


    private void deleteFile(String path) {
        deletedFiles.add(path);
        supportFilesCombo.remove(path);
        this.reflow(true);
       
    }
    
    private void addFile(String path) {
        File f = new File(path);
        addedFiles.add(f);
        supportFilesCombo.add(f.getName());
        this.reflow(true);
    }
    
    private void createTaskDefinitionSection(FormToolkit toolkit,
            Composite parent) {
        Section section = toolkit.createSection(parent, Section.DESCRIPTION
                | Section.TWISTIE | Section.EXPANDED);
        section.setText("Module Definition");

        Composite sectionClient = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        sectionClient.setLayout(layout);
        section.setClient(sectionClient);
      
        createTextInputPair(toolkit, sectionClient, "Name:", taskInfo.getName());
        createTextInputPair(toolkit, sectionClient, "Description:", taskInfo.getDescription());
        createTextInputPair(toolkit, sectionClient, "Author:", (String) taskInfo.getTaskInfoAttributes().get(GPConstants.AUTHOR));
        createTextInputPair(toolkit, sectionClient, "Owner:", (String) taskInfo.getTaskInfoAttributes().get(GPConstants.USERID));
        
        
        createComboInputPair(toolkit, sectionClient, "Privacy:", new String[] {
                "public", "private" }, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.PRIVACY));
        createComboInputPair(toolkit, sectionClient, "Quality Level:",
                new String[] { "development", "preproduction", "production" },
                (String) taskInfo.getTaskInfoAttributes().get(GPConstants.QUALITY));
        
 
        GridLayout cl = new GridLayout();
        cl.numColumns = 2;
     
        Label l = toolkit.createLabel(sectionClient, "Command Line:", SWT.WRAP);
        GridData gridData = new GridData();
        gridData.verticalSpan = 4;
        l.setLayoutData(gridData);
        
        Text text = toolkit.createText(sectionClient, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.COMMAND_LINE), SWT.BORDER | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL);
        gridData.verticalSpan = 4;
        gridData.heightHint = 100;
        gridData.widthHint = 400;
        gridData.grabExcessVerticalSpace = true;
        text.setLayoutData(gridData);
        controlMap.put("Command Line:", text);
 
        createComboInputPair(toolkit, sectionClient, "Task type:",
                (String[]) serviceManager.getCategories().toArray(new String[0]), (String) taskInfo.getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
        createComboInputPair(toolkit, sectionClient, "CPU type:", new String[] {
                "Any", "Alpha", "Intel", "PowerPC", "Sparc" }, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.CPU_TYPE));

        createComboInputPair(toolkit, sectionClient, "Operating System:",
                new String[] { "Any", "Linux", "Mac", "Solaris", "Tru64",
                        "Windows" }, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.OS));

        createComboInputPair(toolkit, sectionClient, "Language:", new String[] {
                "Any", "C", "C++", "Java", "Perl", "Python", "R" }, (String) taskInfo.getTaskInfoAttributes().get(GPConstants.LANGUAGE));

        String defaultVersion = (String) taskInfo.getTaskInfoAttributes().get(GPConstants.VERSION);
        if(defaultVersion==null) {
            defaultVersion = "1.0";
        }
        createTextInputPair(toolkit, sectionClient, "Version:", defaultVersion);

        toolkit.createLabel(sectionClient, "Support Files:", SWT.WRAP);
        Button chooseFileButton = toolkit.createButton(sectionClient,
                "Add File(s)...", SWT.BORDER);
        chooseFileButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                String path = fileOpenDialog.open();
                if(path!=null) {
                    addFile(path); 
                }
            }
        });
        this.setVisible(true);

        String[] supportFiles = new String[0];
        if(taskName!=null) {
            try {
                supportFiles = serviceManager.getTaskIntegratorProxy().getSupportFileNames(taskName);
            } catch (WebServiceException e1) {
                e1.printStackTrace();
            }
        }
        
        supportFilesCombo = new Combo(sectionClient, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        supportFilesCombo.setItems(supportFiles);
        Hyperlink deleteSupportFileLink = toolkit.createHyperlink(
                sectionClient, "Delete", SWT.WRAP);
        
       	deleteSupportFileLink.addHyperlinkListener(new HyperlinkAdapter() {
       	 public void linkActivated(HyperlinkEvent e) {
       	     int index = supportFilesCombo.getSelectionIndex();
       	     if(index!=-1) {
       	         
       	             String fileName = supportFilesCombo.getItem(index);	
       	             deleteFile(fileName);
                
              
       	     }
       	     
       	 }
       	});
       	
       	Hyperlink downloadSupportFileLink = toolkit.createHyperlink(
                sectionClient, "View", SWT.WRAP);
        
       	downloadSupportFileLink.addHyperlinkListener(new HyperlinkAdapter() {
       	 public void linkActivated(HyperlinkEvent e) {
       	     int index = supportFilesCombo.getSelectionIndex();
       	     if(index!=-1) {
       	         try {
       	           String fileName = supportFilesCombo.getItem(index);
       	           File destFile = new File(System.getProperty("java.io.tmpdir"), fileName);
                    serviceManager.getTaskIntegratorProxy().getSupportFile(taskName, fileName, destFile);
                    Program.launch(destFile.getCanonicalPath());
                } catch (WebServiceException e1) {
                    e1.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
       	     }
       	     
       	 }
       	});

    }

    public String getValue(String key) {
        Control c = (Control) controlMap.get(key);
        if (c instanceof Combo) {
            Combo combo = (Combo) c;
            int selectionIndex = combo.getSelectionIndex();
            if (selectionIndex != -1) {
                return combo.getItem(selectionIndex);
            }
            return null;
        }
        return ((Text) c).getText();
    }

    private void createComboInputPair(FormToolkit toolkit, Composite parent,
            String label, String[] choices, String defaultValue) {
        toolkit.createLabel(parent, label, SWT.WRAP);
        Combo c = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        c.setItems(choices);
        int index = -1;
        if(defaultValue!=null) {
            index = c.indexOf(defaultValue);
        }
        
        index = Math.max(index, 0);
        c.select(index);
        controlMap.put(label, c);
    }

    private void createTextInputPair(FormToolkit toolkit, Composite parent,
            String label, String defaultValue) {
        toolkit.createLabel(parent, label, SWT.WRAP);
        Text text = toolkit.createText(parent, defaultValue, SWT.SINGLE|SWT.BORDER);
        GridData gridData = new GridData();
        gridData.widthHint = 400;
        text.setLayoutData(gridData);
        
        text.setTextLimit(Text.LIMIT);
        controlMap.put(label, text);
    }

    private void createParameterSection(final FormToolkit toolkit,
            final Composite parent) {
        Section section = toolkit.createSection(parent, Section.DESCRIPTION
                | Section.TWISTIE | Section.EXPANDED);
        section.setText("Parameters");
        //section.setDescription("The names of these parameters will be
        // available for the command line (above) in the form <name>.\n
        // Parameters with \"filename\" in their name will be treated as input
        // filenames.");
        Composite sectionClient = toolkit.createComposite(section);
        sectionClient.setLayout(new GridLayout());

        parameterComposite = toolkit.createComposite(sectionClient);
        GridLayout layout = new GridLayout();
        layout.numColumns = 6;
        parameterComposite.setLayout(layout);
        section.setClient(sectionClient);

        toolkit.createLabel(parameterComposite, "Name", SWT.WRAP);
        toolkit.createLabel(parameterComposite, "Description", SWT.WRAP);
        toolkit.createLabel(parameterComposite, "Choices", SWT.WRAP);
        toolkit.createLabel(parameterComposite, "Default Value", SWT.WRAP);
        toolkit.createLabel(parameterComposite, "Optional", SWT.WRAP);
        toolkit.createLabel(parameterComposite, "Type", SWT.WRAP);

        if(taskName!=null && taskInfo.getParameterInfoArray()!=null) {
            ParameterInfo[] parameterInfo = taskInfo.getParameterInfoArray();
            for(int i = 0; i < parameterInfo.length; i++) {
                addParameterRow(toolkit, parameterInfo[i]);
            }
        } else {
            addParameterRow(toolkit);
        }
        Hyperlink addParameterLink = toolkit.createHyperlink(
                sectionClient, "Add Parameter", SWT.WRAP);
        addParameterLink.addHyperlinkListener(new HyperlinkAdapter() {
       	 public void linkActivated(HyperlinkEvent e) {  
	       	  addParameterRow(toolkit);
       	 }
       	});

        
    }
    
    static class ParameterInput {
        Map controlMap = new HashMap(6);
        List problems = new ArrayList();
        boolean empty = true;
        static Set requiredParams = new HashSet();
        static Set textInputs = new HashSet();
        static {
            requiredParams.add("name");
            requiredParams.add("types");
            textInputs.add("name");
            textInputs.add("description");
            textInputs.add("choices");
            textInputs.add("default");
        }
       

        /**
         * @param name
         * @param description
         * @param choices
         * @param defaultValue
         * @param optionalCombo
         * @param typesCombo
         */
        public ParameterInput(Control name, Control description, Control choices, Control defaultValue, Control optionalCombo, Control typesCombo) {
            controlMap.put("name", name);
            controlMap.put("description", description);
            controlMap.put("choices", choices);
            controlMap.put("default", defaultValue);
            controlMap.put("optional", optionalCombo);
            controlMap.put("types", typesCombo);
        }
        
        boolean isRequired(String key) {
            return requiredParams.contains(key);
        }
        
        public String getValue(String key) {
            Control c = (Control) controlMap.get(key);
            if(c instanceof Combo) {
                Combo combo = (Combo) c;
                int i = combo.getSelectionIndex();
                if(i!=-1) {
                    return combo.getItem(i);
                    
                }
                return null;
            } else {
                return ((Text)c).getText();
            }
        }
        
        public boolean isValid() {
            return problems.size()==0;
        }
        
        public List getMissingParams() {
            return problems;
        }
        
        public boolean isEmpty() {
            return empty;
        }
        
        public void validate() {
            problems.clear();
            empty = true;
            for(Iterator keys = textInputs.iterator(); keys.hasNext(); ) {
                String key = (String) keys.next();
                String value = getValue(key);
                if(value==null || value.trim().equals("")) {
                    if(isRequired(key)) {
                        problems.add(key); 
                     }
                } else {
                    empty = false;
                }
            }
        
       
        }
       
        
    }
    
    private void addRow(FormToolkit toolkit, String name, String description, String choices, String defaultValue, int optionalIndex, String defaultType) {
        
        Control nameControl = toolkit.createText(parameterComposite, name, SWT.BORDER);
        GridData gridData = new GridData();
        gridData.widthHint = 100;
        nameControl.setLayoutData(gridData);
        
        Control descriptionControl = toolkit.createText(parameterComposite, description, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 320;
        descriptionControl.setLayoutData(gridData);
        
        Control choicesControl = toolkit.createText(parameterComposite, choices, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 140;
        choicesControl.setLayoutData(gridData);
        
        Control defaultValueText = toolkit.createText(parameterComposite, defaultValue, SWT.BORDER);
        gridData = new GridData();
        gridData.widthHint = 90;
        defaultValueText.setLayoutData(gridData);
        
        Combo optionalCombo = new Combo(parameterComposite, SWT.READ_ONLY);
        optionalCombo.setItems(new String[] { "no", "yes" });
        optionalCombo.select(optionalIndex);
       
        Combo typesCombo = new Combo(parameterComposite, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        typesCombo.setItems(new String[] { "floating point", "input file",
                "integer", "text" });
        if(defaultType!=null) {
            typesCombo.select(typesCombo.indexOf(defaultType));
        } else {
            typesCombo.select(3);
        }
        this.reflow(true);
        parameters.add(new ParameterInput(nameControl, descriptionControl, choicesControl, defaultValueText, optionalCombo, typesCombo));
        
    }
    private void addParameterRow(FormToolkit toolkit, ParameterInfo param) {
        String optional = (String) param.getAttributes().get(GPConstants.PARAM_INFO_OPTIONAL[0]);
        String defaultValue = (String) param.getAttributes().get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
        String type = (String) param.getAttributes().get(GPConstants.PARAM_INFO_TYPE[0]);
         
        if(type.equals("java.io.File")) {
            type = "input file";
        } else if(type.equals("java.lang.String")) {
            type = "text";
        } else if(type.equals("java.lang.Float")) {
            type = "floating point";
        }  else if(type.equals("java.lang.Integer")) {
            type = "integer";
        }
        int optionalIndex = 0;
        if("on".equals(optional)) {
            optionalIndex = 1;
        } 
        addRow(toolkit, param.getName(), param.getDescription(), param.getValue(), defaultValue, optionalIndex, type);
            
    }
    
    
    private void addParameterRow(FormToolkit toolkit) {
        addRow(toolkit, null, null, null, null, 0, null);
        
    }

}