/*
 * Created on Aug 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.module;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import edu.mit.broad.gp.core.ServiceManager;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import edu.mit.genome.gp.ui.analysis.AnalysisService;
import edu.mit.wi.omnigene.framework.analysis.ParameterFormatConverter;
import edu.mit.wi.omnigene.framework.analysis.ParameterInfo;
import edu.mit.wi.omnigene.framework.analysis.TaskInfo;
import edu.mit.wi.omnigene.framework.webservice.WebServiceException;
import edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask;
import edu.mit.wi.omnigene.util.OmnigeneException;
import edu.mit.genome.util.GPConstants;

/**
 * @author jgould
 *  
 */
public class PipelineEditorForm extends ScrolledForm {

    private PipelineModel pipelineModel;

    private ServiceManager serviceManager;

    private FormToolkit toolkit;

    List tasks = new ArrayList();

    public PipelineEditorForm(Composite parent, PipelineModel pipelineModel,
            ServiceManager serviceManager) {
        super(parent);
        this.pipelineModel = pipelineModel;
        this.serviceManager = serviceManager;

        this
                .setBackground(Display.getDefault().getSystemColor(
                        SWT.COLOR_WHITE));

        toolkit = new FormToolkit(parent.getDisplay());
        GridLayout layout = new GridLayout();

        Composite buttonComposite = toolkit.createComposite(getBody());
        buttonComposite.setLayout(new GridLayout());
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        buttonComposite.setLayoutData(gd);
        Button saveButton = toolkit.createButton(buttonComposite, "Save", SWT.BORDER);
        saveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                save();
            }
        });
        
        this.getBody().setLayout(layout);

        if (pipelineModel != null) {
            List tasks = pipelineModel.getTasks();

            for (int i = 0; i < tasks.size(); i++) {
                JobSubmission js = (JobSubmission) tasks.get(i);
                String taskName = js.getName();
                ParameterInfo[] formalParams = serviceManager.getService(
                        taskName).getTaskInfo().getParameterInfoArray();

                List parameters = js.getParameters();
                boolean[] runtimePrompt = js.getRuntimePrompt();
                displayTask(i, taskName, formalParams, parameters,
                        runtimePrompt);
            }

        }

    }

    void displayTask(int taskNumber, String taskName,
            ParameterInfo[] formalParams, List actualParams,
            boolean[] runtimePrompt) {
        final Section section = toolkit.createSection(this.getBody(),
                Section.DESCRIPTION | Section.TWISTIE | Section.EXPANDED);
        String sectionText = (taskNumber + 1) + ". " + taskName;
        section.setText(sectionText);

        final Composite sectionClient = toolkit.createComposite(section);
        GridLayout gl = new GridLayout();
        gl.numColumns = 3;
        sectionClient.setLayout(gl);
        section.setClient(sectionClient);
        Object taskInput = null;
        List inheritComposites = new ArrayList();
        List taskInputList = new ArrayList();
        
        for (int p = 0; p < actualParams.size(); p++) {
            ParameterInfo actualParam = (ParameterInfo) actualParams.get(p);
            toolkit.createLabel(sectionClient, ModuleForm
                    .getParameterText(actualParam), SWT.BORDER);

            Map actualAttributes = actualParam.getAttributes();
            String inheritedFilename = null;
            int inheritedTaskNumber = -1;
            if (actualAttributes != null) {
                inheritedFilename = (String) actualAttributes
                        .get(PipelineModel.INHERIT_FILENAME);
               String inheritTaskName = (String) actualAttributes
                .get(PipelineModel.INHERIT_TASKNAME);
               if(inheritTaskName!=null) {
                inheritedTaskNumber = Integer
                        .parseInt(inheritTaskName);
               }
               String runtimePromptString = (String) actualAttributes.get(PipelineModel.RUNTIME_PARAM);
            }
            ParameterInfo formalParam = formalParams[p];
            Map formalAttributes = formalParam.getAttributes();
            String sOptional = (String) formalAttributes
                    .get(GPConstants.PARAM_INFO_OPTIONAL[0]);
            boolean optional = (sOptional != null && sOptional.length() > 0);

            String value = actualParam.getValue();

            boolean dropDown = formalParam.getValue() != null
                    && formalParam.getValue().length() != 0;
            if (dropDown) {
                Combo values = new Combo(sectionClient, SWT.DROP_DOWN
                        | SWT.READ_ONLY);
                taskInput = values;
                StringTokenizer strtok = new StringTokenizer(formalParam
                        .getValue(), ";");
                List items = new ArrayList();
                while (strtok.hasMoreTokens()) {
                    String token = strtok.nextToken();
                    StringTokenizer strtok2 = new StringTokenizer(token, "=");
                    String val = null;
                    if (strtok2.hasMoreTokens()) {
                        val = strtok2.nextToken();
                    } else {
                        val = "";
                    }
                    if (strtok2.hasMoreTokens()) {
                        String lab = strtok2.nextToken();
                        items.add(lab);
                    } else {
                        items.add(val);
                    }
                }
                values.setItems((String[]) items.toArray(new String[items
                        .size()]));

                int index = values.indexOf(value);
                if (index != -1) {
                    values.select(index);
                } else {
                    values.select(0); // FIXME
                }
            } else if (formalParam.isInputFile()) {
                Composite c = toolkit.createComposite(sectionClient);
                c.setLayout(new GridLayout());

                final Text text = toolkit.createText(c, value, SWT.BORDER);
                GridData textGridData = new GridData();
                textGridData.widthHint = 300;
                text.setLayoutData(textGridData);
                int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
                Transfer[] types = new Transfer[] { TextTransfer.getInstance(),
                        org.eclipse.swt.dnd.FileTransfer.getInstance() };
                DropTarget target = new DropTarget(text, operations);
                target.setTransfer(types);
                target.addDropListener(new DropTargetAdapter() {
                    public void drop(DropTargetEvent event) {
                        if (event.data == null) {
                            event.detail = DND.DROP_NONE;
                            return;
                        }
                        if (event.data instanceof String[]) { // drag from
                            // finder
                            text.setText(((String[]) event.data)[0]);
                        } else {
                            text.setText(event.data.toString());
                        }
                    }
                });

                Composite c2 = toolkit.createComposite(c);
                inheritComposites.add(c2);

                if (taskNumber == 0) {
                    c2.setVisible(false);
                }
                GridData gridData = new GridData();
                gridData.horizontalSpan = 3;
                c2.setLayoutData(gridData);
                GridLayout c2l = new GridLayout();
                c2l.numColumns = 3;
                c2.setLayout(c2l);
                toolkit.createLabel(c2, "or use output from");
                Combo previousTasksCombo = new Combo(c2, SWT.DROP_DOWN
                        | SWT.READ_ONLY);
                String[] previousTasks = new String[taskNumber + 1];
                previousTasks[0] = "Choose Task";
                for (int i = 1; i < previousTasks.length; i++) {
                    previousTasks[i] = "Task " + String.valueOf(i);
                }
                previousTasksCombo.setItems(previousTasks);
                if (inheritedTaskNumber != -1) {
                    previousTasksCombo.select(inheritedTaskNumber + 1);
                }

                Combo outputfileCombo = new Combo(c2, SWT.DROP_DOWN
                        | SWT.READ_ONLY);
                outputfileCombo.setItems(new String[] { "1st output",
                        "2nd output", "3rd output", "4th output", "5th output",
                        "stdout", "stderr" });
                taskInput = new Object[] { text, previousTasksCombo,
                        outputfileCombo };
                try {
                    int num = Integer.parseInt(inheritedFilename);
                    outputfileCombo.select(num - 1);
                } catch (NumberFormatException nfe) {
                    if (inheritedFilename != null) {
                        outputfileCombo.select(outputfileCombo
                                .indexOf(inheritedFilename));
                    }
                }

            } else {
                Text text = toolkit
                        .createText(sectionClient, value, SWT.BORDER);
                GridData gd = new GridData();
                gd.widthHint = 300;
                text.setLayoutData(gd);
                taskInput = text;
            }

            toolkit.createLabel(sectionClient, formalParam.getDescription(),
                    SWT.NONE);

            taskInputList.add(taskInput);
            /*
             * if(!optional) { Label star = toolkit.createLabel(c, "*");
             * star.setForeground(red); } name.setLayoutData(gridData);
             */

        }
        final MyTask task = new MyTask(section, taskName, formalParams,
                taskInputList.toArray(), (Composite[]) inheritComposites.toArray(new Composite[0]));
        tasks.add(task);
        Composite buttonComposite = toolkit.createComposite(sectionClient);
        GridLayout buttonL = new GridLayout();
        buttonL.numColumns = 2;
        buttonComposite.setLayout(buttonL);
        Button add = toolkit.createButton(buttonComposite, "Add Task",
                SWT.BORDER);
        add.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                ;
            }
        });
        Button delete = toolkit.createButton(buttonComposite, "Delete",
                SWT.BORDER);
        final int _taskNumber = taskNumber;
        delete.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                int index = tasks.indexOf(task);
                tasks.remove(index);
                section.dispose();
                if (index == 0) {
                    MyTask task = (MyTask) tasks.get(0);
                    task.setShowInheritFields(false);
                }
                // go through subsequent tasks, rename section text, and see
                // which ones inherit from this task
                for (int i = index; i < tasks.size(); i++) {
                    // FIXME check to see if task has a paremeter which
                    // inherited from deleted task
                    // FIXME select different task number for tasks that inherit
                    MyTask task = (MyTask) tasks.get(i);
                    task.section.setText((i + 1) + ". " + task.taskName);
                }
                PipelineEditorForm.this.reflow(true);

            }
        });
    }

    static class MyTask {
        Section section;

        String taskName;

        Map controlMap = new HashMap();

        ParameterInfo[] formalParams;

        private Composite[] inheritComposites;


        /**
         * @param b
         */
        public void setShowInheritFields(boolean b) {
            for(int i = 0; i < inheritComposites.length; i++) {
                inheritComposites[i].setVisible(b);
            }
        }

        public MyTask(Section section, String taskName, ParameterInfo[] params,
                Object[] values, Composite[] inheritComposites) {
            this.section = section;
            this.taskName = taskName;
            this.formalParams = params;
            this.inheritComposites = inheritComposites;
            for (int i = 0; i < params.length; i++) {
                controlMap.put(params[i], values[i]);
            }
        }

        public ParameterInfo[] getFormaParameters() {
            return formalParams;
        }

        public ParameterInfo getParameter(ParameterInfo formalParam) {
            ParameterInfo actualParam = new ParameterInfo();
            actualParam.setName(formalParam.getName());
            boolean runTimePrompt = false; // FIXME
            String value = null;
            if (formalParam.isInputFile()) {
                actualParam.setAsInputFile();
                Object[] c = (Object[]) controlMap.get(formalParam);
                Text t = (Text) c[0];
                Combo taskCombo = (Combo) c[1];
                Combo fileNameCombo = (Combo) c[2];

                boolean inherited = !t.getText().trim().equals("")
                        && taskCombo.getSelectionIndex() > 0
                        && fileNameCombo.getSelectionIndex() >= 0;
                if (inherited) {
                    String inheritedTaskNum = taskCombo.getItem(taskCombo
                            .getSelectionIndex());
                    String inheritedFilename = fileNameCombo
                            .getItem(fileNameCombo.getSelectionIndex());
                    actualParam.getAttributes().put(PipelineModel.INHERIT_FILENAME,
                            inheritedFilename);
                    actualParam.getAttributes().put(PipelineModel.INHERIT_TASKNAME,
                            inheritedTaskNum);
                    actualParam.setValue("");
                } else {
                    actualParam.setValue(t.getText());
                }
                return actualParam;
            } else if (runTimePrompt) {
                actualParam.getAttributes().put(PipelineModel.RUNTIME_PARAM, "1");
                actualParam.setValue("");
                return actualParam;
            }
            Object c =  controlMap.get(formalParam);
           
            if (c instanceof Text) {
                actualParam.setValue(((Text) c).getText());
            } else {
                Combo combo = (Combo) c;
                actualParam.setValue(combo.getItem(combo.getSelectionIndex()));
            }
            return actualParam;
        }
    }

    void save() {
        try {
         String pipelineName = "test.pipeline";
        String commandLine = "<java> -cp ..<file.separator>..<file.separator>common<file.separator>lib<file.separator>activation.jar<path.separator>..<file.separator>..<file.separator>common<file.separator>lib<file.separator>xerces.jar<path.separator>..<file.separator>..<file.separator>common<file.separator>lib<file.separator>saaj.jar<path.separator>..<file.separator>..<file.separator>common<file.separator>lib<file.separator>jaxrpc.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>analysis.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>gpclient.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>log4j-1.2.4.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>axis.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>commons-logging.jar<path.separator>..<file.separator>..<file.separator>webapps<file.separator>gp<file.separator>WEB-INF<file.separator>lib<file.separator>commons-discovery.jar<path.separator> -Ddecorator=edu.mit.genome.gp.ui.analysis.RunPipelineNullDecorator -Domnigene.conf=..<file.separator>..<file.separator>..<file.separator>resources<file.separator> -Dgenepattern.properties=..<file.separator>..<file.separator>..<file.separator>resources<file.separator> edu.mit.genome.gp.ui.analysis.RunPipeline <GenePatternURL>getPipelineModel.jsp?name=" + pipelineName + "&userid=<userid> <userid>";
        PipelineModel model = new PipelineModel();
        
        model.setName(pipelineName);
        model.setDescription("");
        model.setAuthor("GenePattern");
        String version = "1.0";
        model.setVersion(version);
        model.setUserID("GenePattern");
        model.setPrivacy(false); 

        for (int i = 0; i < tasks.size(); i++) {
            MyTask task = (MyTask) tasks.get(i);
            String taskName = task.taskName;
            int taskNumber = i;
            
            ParameterInfo[] formalParams = task.getFormaParameters();
            boolean[] runtimePrompt = new boolean[formalParams.length];
            
            JobSubmission js = new JobSubmission();
            js.setName(taskName);
            AnalysisService analysisService = serviceManager.getService(taskName);
            js.setDescription(analysisService.getTaskInfo().getDescription());
            js.setVisualizer(SubmitAnalysisAction.isVisualizer(analysisService));
            
            for (int j = 0; j < formalParams.length; j++) {
                String paramName = formalParams[j].getName();
                ParameterInfo actualParam = task.getParameter(formalParams[j]);
                String newName = taskName + (taskNumber + 1) + "."
                + paramName;
                
                model.addInputParameter(newName, actualParam);
                Map attrs = actualParam.getAttributes();
                if(attrs!=null && actualParam.getAttributes().containsKey(PipelineModel.RUNTIME_PARAM)){
                    runtimePrompt[j] = true;
                }
                js.addParameter(actualParam);
            }
            // FIXME all parameters are prompt when run
            js.setRuntimePrompt(runtimePrompt);
            model.addTask(js);

        }
        Map taskAttributes = new HashMap();
        taskAttributes.put(GPConstants.COMMAND_LINE, commandLine);
        taskAttributes.put(GPConstants.OS, "any");
        taskAttributes.put(GPConstants.USERID, "GenePattern");
        taskAttributes.put(GPConstants.AUTHOR, "GenePattern");
        taskAttributes.put(GPConstants.LANGUAGE, "any");
        taskAttributes.put(GPConstants.VERSION, "1.0");
        taskAttributes.put(GPConstants.CLASSNAME, "edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask");
        taskAttributes.put(GPConstants.QUALITY, "production");
        taskAttributes.put(GPConstants.DESCRIPTION, "");
        taskAttributes.put(GPConstants.CPU_TYPE, "any");
        taskAttributes.put(GPConstants.TASK_TYPE, "pipeline");
        taskAttributes.put(GPConstants.SERIALIZED_MODEL, model.toXML());
      
        String pipelineDescription = "";
        File[] files = null; // FIXME
        try {
            serviceManager.getTaskIntegratorProxy().modifyTask(GPConstants.ACCESS_PUBLIC, pipelineName, pipelineDescription, null, taskAttributes, files);
        } catch (WebServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        } catch(Throwable t) {
            t.printStackTrace();
        }

    }
}