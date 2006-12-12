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

package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.genepattern.codegenerator.JavaPipelineCodeGenerator;
import org.genepattern.codegenerator.MATLABPipelineCodeGenerator;
import org.genepattern.codegenerator.RPipelineCodeGenerator;
import org.genepattern.codegenerator.TaskCodeGenerator;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.CLThread;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.gpge.ui.tasks.pipeline.ExportPipeline;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Displays an <tt>AnalysisService</tt>
 * 
 * @author Joshua Gould
 */
public class AnalysisServiceDisplay extends JPanel implements TaskDisplay {
    /** the currently displayed <tt>AnalysisService</tt> */
    private AnalysisService selectedService;

    private boolean advancedGroupExpanded = false;

    private TogglePanel togglePanel;

    private ParameterInfoPanel parameterInfoPanel;

    private JComboBox runToComboBox;

    public AnalysisServiceDisplay() {
        this.setBackground(Color.white);
        showGettingStarted();
    }

    public void showGettingStarted() {
        java.net.URL url = AnalysisServiceDisplay.class
                .getResource("/org/genepattern/gpge/resources/getting_started.html");
        final JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent evt) {
                if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    URL url = evt.getURL();
                    try {
                        BrowserLauncher.openURL(url.toString());
                    } catch (Exception e) {
                    }
                } else if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    pane.setCursor(Cursor
                            .getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    pane.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        try {
            pane.setPage(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pane.setMargin(new Insets(5, 5, 5, 5));
        pane.setEditable(false);
        pane.setBackground(Color.white);
        removeAll();
        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
        invalidate();
        validate();
        selectedService = null;
    }

    /**
     * Displays the given analysis service
     * 
     * @param selectedService
     *            Description of the Parameter
     */
    public void loadTask(AnalysisService _selectedService) {
        if (_selectedService == null) {
            throw new NullPointerException();
        }
        this.selectedService = _selectedService;
        JPanel bottomPanel = new JPanel(new BorderLayout());
        if (togglePanel != null) {
            advancedGroupExpanded = togglePanel.isExpanded();
        }

        TaskInfo taskInfo = selectedService.getTaskInfo();
        String taskName = taskInfo.getName();

        JPanel topPanel = new TaskNamePanel(taskInfo,
                ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST);

        ParameterInfo[] params = taskInfo.getParameterInfoArray();

        parameterInfoPanel = new ParameterInfoPanel(taskName, params);
        removeAll();

        boolean isPipeline = TaskLauncher.isPipeline(selectedService);
        setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        if (isPipeline) {

            runToComboBox = new JComboBox();
            buttonPanel.add(new JLabel("Run to "));
            buttonPanel.add(runToComboBox);
        }

        JButton submitButton = new JButton("Run");
        submitButton.addActionListener(new SubmitActionListener());
        buttonPanel.add(submitButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ResetActionListener());
        buttonPanel.add(resetButton);
        JButton helpButton = new JButton("Help");

        TaskHelpActionListener tsl = new TaskHelpActionListener();
        tsl.setTaskInfo(selectedService.getTaskInfo());
        helpButton.addActionListener(tsl);

        buttonPanel.add(helpButton);

        if (isPipeline) {

            // PipelineEditor pipelineEditor = new PipelineEditor();
            // pipelineEditor.setShowButtonPanel(false);
            // pipelineEditor.setShowHeaderPanel(false);

            try {
                PipelineModel model = PipelineModel
                        .toPipelineModel(((String) selectedService
                                .getTaskInfo().getTaskInfoAttributes().get(
                                        GPConstants.SERIALIZED_MODEL)));

                List<JobSubmission> tasks = model.getTasks();

                if (tasks != null) {
                    for (int i = 0; i < tasks.size(); i++) {
                        runToComboBox.addItem((i + 1) + ". "
                                + tasks.get(i).getName());
                    }
                }
                runToComboBox
                        .setSelectedIndex(runToComboBox.getItemCount() - 1);
                // pipelineEditor.view(selectedService, model);
                //
                // JDialog d = new JDialog();
                // d.getContentPane().add(pipelineEditor);
                // d.pack();
                // d.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
            }

            JButton editButton = new JButton("Edit");
            editButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MessageManager
                            .notifyListeners(new ChangeViewMessageRequest(
                                    this,
                                    ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST,
                                    selectedService));
                }
            });
            buttonPanel.add(editButton);

            JButton viewButton = new JButton("View");
            viewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MessageManager
                            .notifyListeners(new ChangeViewMessageRequest(
                                    this,
                                    ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
                                    selectedService));
                }
            });
            buttonPanel.add(viewButton);

            JButton exportButton = new JButton("Export");
            exportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    new ExportPipeline(selectedService);
                }
            });
            buttonPanel.add(exportButton);
        }

        JPanel viewCodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        final JComboBox viewCodeComboBox = new JComboBox(new String[] {
                "- Language -", "Java", "MATLAB", "R" });
        
        JButton viewCodeButton = new JButton("View Code");
        viewCodePanel.add(viewCodeComboBox);
        viewCodePanel.add(viewCodeButton);

        togglePanel = new TogglePanel("Generate Code", viewCodePanel);
        togglePanel.setExpanded(advancedGroupExpanded);
        /*
         * JTaskPaneGroup group = new JTaskPaneGroup(); group.setText("Advanced
         * Options"); JTaskPane tp = new JTaskPane(); group.add(viewCodePanel);
         * tp.add(group);
         */

        bottomPanel.setBorder(GUIUtil.createBorder(UIManager
                .getLookAndFeelDefaults().getBorder("ScrollPane.border"), 0, 0,
                0, 2));

        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(togglePanel, BorderLayout.SOUTH);

        viewCodeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (viewCodeComboBox.getSelectedIndex() == 0) {
                    return;
                }
                String language = (String) viewCodeComboBox.getSelectedItem();
                TaskCodeGenerator codeGenerator = null;
                if ("Java".equals(language)) {
                    codeGenerator = new JavaPipelineCodeGenerator();
                } else if ("MATLAB".equals(language)) {
                    codeGenerator = new MATLABPipelineCodeGenerator();
                } else if ("R".equals(language)) {
                    codeGenerator = new RPipelineCodeGenerator();
                } else {
                    throw new IllegalArgumentException("Unknown language");
                }
                String lsid = (String) selectedService.getTaskInfo()
                        .getTaskInfoAttributes().get(GPConstants.LSID);

                JobInfo jobInfo = new JobInfo(-1, -1, null, null, null,
                        parameterInfoPanel.getParameterInfoArray(),
                        AnalysisServiceManager.getInstance().getUsername(),
                        lsid, selectedService.getTaskInfo().getName());
                AnalysisJob job = new AnalysisJob(selectedService.getServer(),
                        jobInfo, TaskLauncher.isVisualizer(selectedService));
                org.genepattern.gpge.ui.code.Util.viewCode(codeGenerator, job,
                        language);
            }
        });

        add(topPanel, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(parameterInfoPanel);
        final Border b = sp.getBorder();
        sp.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
        add(sp, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setMinimumSize(new java.awt.Dimension(100, 100));
        revalidate();
        doLayout();
    }

    /**
     * Sets the value of the given parameter
     * 
     * @param parameterName
     *            the unencoded parameter name as returned by
     *            ParameterInfo.getName()
     * @param parameterValue
     *            the parameter value. If the parameter contains a choice list,
     *            the value can be either the UI value of the command line value
     */
    public void setValue(String parameterName, String parameterValue) {
        parameterInfoPanel.setValue(parameterName, parameterValue);
    }

    public void sendTo(String parameterName, Sendable sendable) {
        if (selectedService != null) {
            ObjectTextField tf = (ObjectTextField) parameterInfoPanel
                    .getComponent(displayToActualParameterString(parameterName));
            if (tf != null) {
                tf.setObject(sendable);
            }
        }
    }

    public static String getDisplayString(ParameterInfo p) {
        return getDisplayString(p.getName());
    }

    public static String getDisplayString(String name) {
        return name.replace('.', ' ');
    }

    public static String displayToActualParameterString(String name) {
        return name.replace(' ', '.');
    }

    /**
     * Returns <tt>true</tt> of this panel is showing an <tt>AnalysisService
     *  </tt>
     * 
     * @return whether this panel is showing an <tt>AnalysisService</tt>
     */
    public boolean isShowingAnalysisService() {
        return selectedService != null;
    }

    public java.util.Iterator getInputFileParameters() {
        return parameterInfoPanel.getInputFileParameters();
    }

    private class ResetActionListener implements ActionListener {
        public final void actionPerformed(ActionEvent ae) {
            loadTask(selectedService);
        }
    }

    public static void doSubmit(JButton source,
            final ParameterInfo[] actualParameterArray,
            final AnalysisService selectedService) {
        try {
            source.setEnabled(false);

            new CLThread() {
                public void run() {
                    RunTask rt = new RunTask(selectedService,
                            actualParameterArray, AnalysisServiceManager
                                    .getInstance().getUsername(),
                            AnalysisServiceManager.getInstance().getPassword());
                    rt.exec();
                }
            }.start();
        } finally {
            if (TaskLauncher.isVisualizer(selectedService)) {
                try {
                    CLThread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            source.setEnabled(true);
        }
    }

    private class SubmitActionListener implements ActionListener {

        public final void actionPerformed(ActionEvent ae) {
            JButton source = (JButton) ae.getSource();
            ParameterInfo[] actualParameterArray = parameterInfoPanel
                    .getParameterInfoArray();
            if (runToComboBox != null) {
                int taskNumber = runToComboBox.getSelectedIndex();
                ParameterInfo p = new ParameterInfo(
                        "PIPELINE_ARG.StopAfterTask", "" + taskNumber, "");
                ParameterInfo[] temp = new ParameterInfo[actualParameterArray.length + 1];
                System.arraycopy(actualParameterArray, 0, temp, 0,
                        actualParameterArray.length);
                temp[temp.length - 1] = p;
                actualParameterArray = temp;

            }
            AnalysisService _selectedService = selectedService;
            doSubmit(source, actualParameterArray, _selectedService);
        }
    }

    public Iterator getInputFileTypes() {
        return parameterInfoPanel.getInputFileTypes();
    }

}
