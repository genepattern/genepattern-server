package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class PipelineComponent extends JPanel {
	private TaskInfoAttributes pipelineTaskInfoAttributes;
	private PipelineModel pipelineModel;
	private List jobSubmissions;
	private String userID;

	public static void test(TaskInfo taskInfo) {
		PipelineComponent c = new PipelineComponent();
		c.setTaskInfo(taskInfo);
		JFrame f = new JFrame();
		f.getContentPane().add(new JScrollPane(c), BorderLayout.CENTER);
		f.pack();
		f.show();

	}

	public PipelineComponent() {
		setBackground(Color.white);
	}
	
	public void setTaskInfo(TaskInfo pipelineTaskInfo) {
		pipelineTaskInfoAttributes = pipelineTaskInfo.giveTaskInfoAttributes();
		try {
			pipelineModel = PipelineModel
					.toPipelineModel((String) pipelineTaskInfoAttributes
							.get(GPConstants.SERIALIZED_MODEL));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		jobSubmissions = pipelineModel.getTasks();
		userID = pipelineTaskInfo.getUserId();
		String lsidStr = pipelineTaskInfoAttributes.get("LSID");

		try {
			LSID pipeLSID = new LSID(lsidStr);
		} catch (MalformedURLException e2) {
			e2.printStackTrace();
		}
		String displayName = pipelineModel.getName();
		if (displayName.endsWith(".pipeline")) {
			displayName = displayName.substring(0, displayName.length()
					- ".pipeline".length());
		}

		// show edit link when task has local authority and either belongs to
		// current user or is public
		String lsid = (String) pipelineTaskInfoAttributes.get(GPConstants.LSID);
		String description = pipelineModel.getDescription();
		String owner = pipelineTaskInfo.getUserId();
		List tasks = pipelineModel.getTasks();
		StringBuffer rowSpec = new StringBuffer();
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) {
                rowSpec.append(", ");
            }
            rowSpec.append("pref");// input, description space

        }
        
		FormLayout formLayout = new FormLayout( // 
                "left:pref:none", rowSpec
                        .toString());
		
		setLayout(formLayout);
		CellConstraints cc = new CellConstraints();
		for(int i = 0; i < tasks.size(); i++) {
			add(new PipelineTask(i, (JobSubmission)tasks.get(i)), cc.xy(1, (i+1)));
		}
	}

	protected boolean taskExists(String lsid, String userID) {
		return true;
	}

	protected TaskInfo getTaskInfo(String lsid, String userID) {
		AnalysisService svc = AnalysisServiceManager.getInstance().getAnalysisService(lsid);
		if(svc==null) {
			return null;
		}
		return svc.getTaskInfo();
	}

	private class PipelineTask extends JPanel {

		public PipelineTask(int displayNumber, JobSubmission js) {
			//          what is the difference between JobSubmission getName and getLSID

			setBackground(Color.white);
			TaskInfo formalTaskInfo = getTaskInfo(js.getLSID(), userID);
			ParameterInfo[] formalParams = formalTaskInfo != null ? formalTaskInfo
					.getParameterInfoArray()
					: null;
			if (formalParams == null) {
				formalParams = new ParameterInfo[0];
			}
			int maxLabelWidth = 0;
			ParameterInfoPanel parameterInfoPanel = new ParameterInfoPanel(js.getName(),
					formalParams);
			parameterInfoPanel.setUseInputFromPreviousTask(0, new String[]{"a"}, new String[]{"b"});
			maxLabelWidth = Math.max(maxLabelWidth, parameterInfoPanel.getLabelWidth());
			JTextField description = new JTextField(js.getDescription(), 80);
			
			String lsid = js.getLSID();
			String lsidNoVersion = null;
			String currentVersion = null;
			try {
				LSID _lsid = new LSID(lsid);
				lsidNoVersion = _lsid.toStringNoVersion();
				currentVersion = _lsid.getVersion();
			} catch(Exception e) {
				
			}
			Vector versionsCopy = new Vector();
			if(lsidNoVersion!=null) {
				List versions = (List) AnalysisServiceManager.getInstance().getLSIDToVersionsMap().get(lsidNoVersion);
				versionsCopy.addAll(versions);
				Collections.sort(versionsCopy, String.CASE_INSENSITIVE_ORDER);
	        		versionsCopy.add("Always Use Latest");
			} 
			JComboBox versionComboBox = new JComboBox(versionsCopy);
			versionComboBox.setBackground(getBackground());
			versionComboBox.setSelectedItem(currentVersion);
			JPanel p = new JPanel(new BorderLayout());
			p.setBackground(getBackground());
			
			JButton docBtn = new JButton("Documentation");
			docBtn.setBackground(getBackground());
			
			JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			topPanel.setBackground(getBackground());
			JLabel versionLabel = new JLabel("Version:");
			topPanel.add(versionLabel);
			topPanel.add(versionComboBox);
			topPanel.add(docBtn);
			
			p.add(topPanel, BorderLayout.NORTH);
			p.add(parameterInfoPanel);
			TogglePanel togglePanel = new TogglePanel((displayNumber+1) + ". " + formalTaskInfo.getName(), description, p);
			togglePanel.setBackground(parameterInfoPanel.getBackground());
			togglePanel.setExpanded(true);
		     
			setValues(js, formalParams, parameterInfoPanel);
			setLayout(new BorderLayout());
			add(togglePanel, BorderLayout.CENTER);
			
			JButton addButton = new JButton("Add Task After");
			addButton.setBackground(getBackground());
			JButton addBeforeButton = new JButton("Add Task Before");
			addBeforeButton.setBackground(getBackground());
			JButton deleteButton = new JButton("Delete");
			deleteButton.setBackground(getBackground());
			JButton moveUpButton = new JButton("Move Up");
			moveUpButton.setBackground(getBackground());
			JButton moveDownButton = new JButton("Move Down");
			moveDownButton.setBackground(getBackground());
			JPanel bottomPanel = new JPanel();
			bottomPanel.setBackground(getBackground());
			bottomPanel.add(addButton);
			bottomPanel.add(addBeforeButton);
			bottomPanel.add(deleteButton);
			bottomPanel.add(moveUpButton);
			bottomPanel.add(moveDownButton);
			add(bottomPanel, BorderLayout.SOUTH);
		}

	}

	private void setValues(JobSubmission js, ParameterInfo[] formalParams,
			ParameterInfoPanel parameterInfoPanel) {

		TaskInfo actualTaskInfo = js.getTaskInfo();
		ParameterInfo[] actualParameters = js.giveParameterInfoArray();

		boolean[] runtimePrompt = js.getRuntimePrompt();

		Map paramName2ActualParamIndexMap = new HashMap();
		for (int j = 0; j < actualParameters.length; j++) {
			paramName2ActualParamIndexMap.put(actualParameters[j].getName(),
					new Integer(j));
		}

		for (int j = 0; j < formalParams.length; j++) {
			String paramName = formalParams[j].getName();

			ParameterInfo formalParam = formalParams[j];
			Integer index = (Integer) paramName2ActualParamIndexMap
					.get(paramName);
			ParameterInfo actualParam = null;
			if (index != null) {
				actualParam = actualParameters[index.intValue()];
			}

			String value = null;
			if (formalParam.isInputFile()) {
				java.util.Map pipelineAttributes = actualParam.getAttributes();

				String taskNumber = null;
				if (pipelineAttributes != null) {
					taskNumber = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_TASKNAME);
				}
				int k = index.intValue();
				if ((k < runtimePrompt.length) && (runtimePrompt[k])) {
					value = "Prompt when run";
				} else if (taskNumber != null) {
					String outputFileNumber = (String) pipelineAttributes
							.get(PipelineModel.INHERIT_FILENAME);
					int taskNumberInt = Integer.parseInt(taskNumber.trim());
					String inheritedOutputFileName = null;
					if (outputFileNumber.equals("1")) {
						inheritedOutputFileName = "1st output";
					} else if (outputFileNumber.equals("2")) {
						inheritedOutputFileName = "2nd output";
					} else if (outputFileNumber.equals("3")) {
						inheritedOutputFileName = "3rd output";
					} else if (outputFileNumber.equals("stdout")) {
						inheritedOutputFileName = "standard output";
					} else if (outputFileNumber.equals("stderr")) {
						inheritedOutputFileName = "standard error";
					}
					JobSubmission inheritedTask = (JobSubmission) jobSubmissions
							.get(taskNumberInt);
					int displayTaskNumber = taskNumberInt + 1;

					value = "Use " + inheritedOutputFileName + "from "
							+ displayTaskNumber + ". "
							+ inheritedTask.getName();
				} else {
					value = actualParam.getValue();

					try {
						new java.net.URL(value);// see if parameter is a URL
					} catch (java.net.MalformedURLException x) {

					}
				}

			} else {
				String[] choices = formalParam.getValue().split(
						GPConstants.PARAM_INFO_CHOICE_DELIMITER);
				String[] eachValue;
				value = actualParam.getValue();
				for (int v = 0; v < choices.length; v++) {
					eachValue = choices[v]
							.split(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
					if (value.equals(eachValue[0])) {
						if (eachValue.length == 2) {
							value = eachValue[1];
						}
						break;
					}
				}
			}
			parameterInfoPanel.setValue(paramName, value);
		}
	}
}
