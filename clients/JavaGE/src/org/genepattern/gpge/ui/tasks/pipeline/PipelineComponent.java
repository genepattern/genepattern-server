package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;

import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.GroupPanel;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.gpge.ui.tasks.AnalysisServiceDisplay;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.ParameterChoice;
import org.genepattern.gpge.ui.tasks.Sendable;
import org.genepattern.gpge.ui.tasks.TaskDisplay;
import org.genepattern.gpge.ui.tasks.TaskHelpActionListener;
import org.genepattern.gpge.ui.tasks.TaskNamePanel;
import org.genepattern.gpge.ui.tasks.VersionComboBox;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

// inherited task numbers start at 0, output files start at one, names for params start at one
public class PipelineComponent extends JPanel implements TaskDisplay,
		PipelineListener {
	private static final boolean DEBUG = false;

	private static final int INPUT_FIELD_COLUMN = 5;

	private static final int INPUT_LABEL_COLUMN = 3;

	private static final int PROMPT_WHEN_RUN_COLUMN = 1;

	private PipelineEditorModel model;

	private JPanel tasksPanel;

	private FormLayout tasksLayout;

	private ArrayList taskDisplayList = new ArrayList();

	private JPanel buttonPanel;

	private HeaderPanel headerPanel;

	private List inputFileParameters = new ArrayList();

	private JComboBox tasksInPipelineComboBox;

	private JScrollPane scrollPane;

	private TaskHelpActionListener taskHelpActionListener;

	private JButton addAfterButton;

	private JButton addBeforeButton;

	private JButton deleteButton;

	private JButton moveUpButton;

	private JButton moveDownButton;

	private Map parameterName2ComponentMap = new HashMap();

	private ArrayList inputFileTypes = new ArrayList();

	
	private void enableButtons() {
		deleteButton.setEnabled(model.getTaskCount() > 0);
		if(model.getTaskCount()==0) {
			addAfterButton.setText("Add Task");
		} else {
			addAfterButton.setText("Add Task After");
		}
		int index = tasksInPipelineComboBox.getSelectedIndex();
		addBeforeButton.setEnabled(index > 0);
		moveDownButton.setEnabled((index + 1) != model.getTaskCount());
		moveUpButton.setEnabled(index > 0);

	}

	/**
	 * Currently only one instance should be created by the ViewManager
	 * 
	 */
	public PipelineComponent() {
		setBackground(Color.white);
		setMinimumSize(new java.awt.Dimension(100, 100));
		setLayout(new BorderLayout());

		scrollPane = new JScrollPane();
		Border b = scrollPane.getBorder();
		scrollPane.setBorder(GUIUtil.createBorder(b, 0, -1, -1, -1));
		add(scrollPane, BorderLayout.CENTER);

		tasksInPipelineComboBox = new JComboBox();

		addAfterButton = new JButton("Add Task After");

		addBeforeButton = new JButton("Add Task Before");

		deleteButton = new JButton("Delete");

		moveUpButton = new JButton("Move Up");

		moveDownButton = new JButton("Move Down");

		tasksInPipelineComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				enableButtons();

			}
		});

		ActionListener taskBtnListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = tasksInPipelineComboBox.getSelectedIndex();
				if (e.getSource() == addAfterButton) {
					showAddTask(index, true);
				} else if (e.getSource() == addBeforeButton) {
					showAddTask(index, false);
				} else if (e.getSource() == deleteButton) {
					model.remove(index);
				} else if (e.getSource() == moveUpButton) {
					model.move(index, index - 1);
				} else if (e.getSource() == moveDownButton) {
					model.move(index, index + 1);
				}
				enableButtons();
			}
		};

		addAfterButton.addActionListener(taskBtnListener);
		addBeforeButton.addActionListener(taskBtnListener);
		deleteButton.addActionListener(taskBtnListener);
		moveUpButton.addActionListener(taskBtnListener);
		moveDownButton.addActionListener(taskBtnListener);

		buttonPanel = new JPanel(new BorderLayout());
		JPanel topPanel = new JPanel();
		topPanel.add(tasksInPipelineComboBox);
		topPanel.add(addAfterButton);
		topPanel.add(addBeforeButton);
		topPanel.add(deleteButton);
		topPanel.add(moveUpButton);
		topPanel.add(moveDownButton);

		JPanel bottomPanel = new JPanel();
		final JButton expandAllButton = new JButton("Expand All");
		final JButton collapseAllButton = new JButton("Collapse All");
		bottomPanel.add(collapseAllButton);
		bottomPanel.add(expandAllButton);
		buttonPanel.add(topPanel, BorderLayout.CENTER);
		buttonPanel.add(bottomPanel, BorderLayout.SOUTH);
		buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));
		
		ActionListener expandListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == expandAllButton) {
					for (int i = 0; i < taskDisplayList.size(); i++) {
						MyTask p = (MyTask) taskDisplayList.get(i);
						p.setExpanded(true);
					}
				} else if (e.getSource() == collapseAllButton) {
					for (int i = 0; i < taskDisplayList.size(); i++) {
						MyTask p = (MyTask) taskDisplayList.get(i);
						p.setExpanded(false);
					}
				}
			}

		};

		expandAllButton.addActionListener(expandListener);
		collapseAllButton.addActionListener(expandListener);

		JPanel bottomBtnPanel = new JPanel();
		final JButton saveButton = new JButton("Save");
		final JButton runButton = new JButton("Run");
		final JButton viewButton = new JButton("View");
		final JButton helpButton = new JButton("Help");
		taskHelpActionListener = new TaskHelpActionListener();
		helpButton.addActionListener(taskHelpActionListener);

		ActionListener btnListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == saveButton) {
					save();
				} else if (e.getSource() == runButton) {
					// FIXME
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST,
									null));
				} else if (e.getSource() == viewButton) {
					MessageManager
							.notifyListeners(new ChangeViewMessageRequest(
									this,
									ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST,
									null));
					// FIXME
				}
			}

		};
		saveButton.addActionListener(btnListener);
		runButton.addActionListener(btnListener);
		viewButton.addActionListener(btnListener);
		bottomBtnPanel.add(saveButton);
		bottomBtnPanel.add(runButton);
		bottomBtnPanel.add(viewButton);
		bottomBtnPanel.add(helpButton);
		add(bottomBtnPanel, BorderLayout.SOUTH);
	}

	protected void save() {
		StringBuffer errors = headerPanel.save();
		List localInputFiles = new ArrayList();
		List existingFileNames = new ArrayList();
		for (int i = 0; i < taskDisplayList.size(); i++) {
			MyTask td = (MyTask) taskDisplayList.get(i);
			model.setTaskDescription(i, td.getTaskDescription());
			for (int j = 0; j < model.getParameterCount(i); j++) {
				ParameterDisplay pd = td.parameters[j];
				int inheritedTaskIndex = pd.getInheritedTaskIndex();
				if (inheritedTaskIndex != -1) {
					String inheritedFileName = pd.getInheritedFileName();
					if (inheritedFileName == null) {
						errors.append("Missing value for " + (i + 1) + ". "
								+ model.getTaskName(i) + " "
								+ model.getParameterName(i, j) + "\n");
					}
					model.setInheritedFile(i, j, inheritedTaskIndex,
							inheritedFileName);
				} else if (pd.isPromptWhenRun()) {
					model.setPromptWhenRun(i, j);
				} else {
					String value = pd.getValue();
					if (model.isRequired(i, j)) {
						if (value == null || value.trim().equals("")) {
							errors.append("Missing value for " + (i + 1) + ". "
									+ model.getTaskName(i) + " "
									+ model.getParameterName(i, j) + "\n");
						}
					}
					
					if (model.isInputFile(i, j)) {
						File file = new File(value);
						if (file.exists()) {
							localInputFiles.add(file);
						} else if(value.startsWith("<GenePatternURL>getFile.jsp?task=<LSID>&file=")){
							String fileName = value.substring("<GenePatternURL>getFile.jsp?task=<LSID>&file=".length(), value.length());
							existingFileNames.add(fileName);
						}
					}
					
					model.setValue(i, j, value);
				}
			}
		}
		if (errors.length() > 0) {
			GenePattern.showErrorDialog(errors.toString());
			return;
		}
		TaskInfo ti = model.toTaskInfo();
		try {
			List taskFiles = new ArrayList();
			taskFiles.addAll(localInputFiles);
			taskFiles.addAll(model.getLocalDocFiles());
			
			existingFileNames.addAll(model.getServerDocFiles());
			
			
			String lsid = new TaskIntegratorProxy(AnalysisServiceManager.getInstance()
					.getServer(), AnalysisServiceManager.getInstance()
					.getUsername(), false).modifyTask(
					GPConstants.ACCESS_PUBLIC, ti.getName(), ti
							.getDescription(), ti.getParameterInfoArray(),
					(HashMap) ti.getTaskInfoAttributes(), (File[]) taskFiles.toArray(new File[0]), (String[]) existingFileNames.toArray(new String[0]));
			
			model.setLSID(lsid);
		} catch (WebServiceException e1) {
			e1.printStackTrace();
			if (!GenePattern.disconnectedFromServer(e1, AnalysisServiceManager
					.getInstance().getServer())) {
				GenePattern
						.showErrorDialog("An error occurred while saving the pipeline.");
			}
		}
	}

	void reset() {
		if (headerPanel != null) {
			remove(headerPanel);
		}
		taskDisplayList.clear();
		inputFileTypes.clear();
		parameterName2ComponentMap.clear();
		inputFileParameters.clear();
		tasksInPipelineComboBox.removeAllItems();
		tasksLayout = new FormLayout(
				"left:pref, 3dlu, right:pref, 3dlu, default:grow", "");
		tasksPanel = new JPanel(tasksLayout);
		tasksPanel.setBackground(getBackground());
		scrollPane.setViewportView(tasksPanel);
	}
	
	public Iterator getInputFileParameters() {
		return inputFileParameters.iterator();
	}
	
	public Iterator getInputFileTypes() {
		return inputFileTypes.iterator();
	}

	public void sendTo(String parameterName,
			Sendable sendable) {
		ParameterDisplay display = (ParameterDisplay) parameterName2ComponentMap  .get(parameterName);
		if (display != null) {
			 display.setValue(sendable); 
		}
	}

	public void display(AnalysisService svc) {
		if (svc == null) {
			setPipeline(null, null);
		} else {
			TaskInfo info = svc.getTaskInfo();
			try {
				PipelineModel pipelineModel = PipelineModel
						.toPipelineModel((String) info.getTaskInfoAttributes()
								.get(GPConstants.SERIALIZED_MODEL));
				setPipeline(svc, pipelineModel);

			} catch (Exception e1) {
				e1.printStackTrace();
				GenePattern
						.showErrorDialog("An error occurred while loading the pipeline");

			}
		}
	}

	private void setPipeline(AnalysisService svc, PipelineModel pipelineModel) {

		if (svc == null) {
			model = new PipelineEditorModel();
			String username = AnalysisServiceManager.getInstance()
					.getUsername();
			model.setAuthor(username);
			model.setOwner(username);
		} else {
			model = new PipelineEditorModel(svc, pipelineModel);
			if(model.getMissingJobSubmissions().size() > 0) {
				MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST, svc));
				return; // FIXME
			}
		}
		enableButtons();
		model.addPipelineListener(this);
		if (DEBUG) {
			model.print();
		}

		// show edit link when task has local authority and either belongs
		// to
		// current user or is public
		layoutTasks();

	}

	static class HeaderPanel extends JPanel {
		
		private JTextField nameField;
		private JTextField descriptionField;
		private JTextField authorField;
		private JTextField ownerField;
		private JComboBox privacyComboBox;
		private JTextField versionField;
		private PipelineEditorModel model;
		
		public StringBuffer save() {
			StringBuffer errors = new StringBuffer();
			String name = nameField.getText().trim();
			if(name.equals("")) {
				errors.append("Missing value for name\n");
			}
			model.setPipelineName(name);
			
			String author = authorField.getText().trim();
			if(author.equals("")) {
				errors.append("Missing value for author\n");
			}
			model.setAuthor(author);
			String owner = ownerField.getText().trim();
			if(owner.equals("")) {
				errors.append("Missing value for owner\n");
			}
			model.setOwner(owner);
			String privacy = (String) privacyComboBox.getSelectedItem();
			if(privacy.equals("Public")) { 
				model.setPrivacy(GPConstants.ACCESS_PUBLIC);
			} else {
				model.setPrivacy(GPConstants.ACCESS_PRIVATE);
			}
			model.setVersionComment(versionField.getText());
			model.setPipelineDescription(descriptionField.getText());
			
			return errors;
		}
		
		public HeaderPanel(final PipelineEditorModel model, JPanel buttonPanel) {
			this.model = model;
			//setBorder(BorderFactory.createLineBorder());
			setLayout(new BorderLayout());

			String name = model.getPipelineName();
			if (name != null && name.endsWith(".pipeline")) {
				name = name.substring(0, name.length() - ".pipeline".length());
			}
			JLabel nameLabel = new JLabel("Name:");
			nameField = new JTextField(name, 40);

			JLabel descriptionLabel = new JLabel("Description:");
			descriptionField = new JTextField(model
					.getPipelineDescription(), 40);
			JComboBox versionComboBox = null;
			if (name != null) {
				versionComboBox = new VersionComboBox(model.getLSID(),
						ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST);
			}
			CellConstraints cc = new CellConstraints();
			JPanel temp = new JPanel(new FormLayout(
					"right:pref:none, 3dlu, pref, pref",
					"pref, 3dlu, pref, pref"));

			temp.add(nameLabel, cc.xy(1, 1));
			temp.add(nameField, cc.xy(3, 1));

			if (versionComboBox != null) {
				temp.add(versionComboBox, cc.xy(4, 1));
			}

			temp.add(descriptionLabel, cc.xy(1, 3));
			temp.add(descriptionField, cc.xy(3, 3));

			StringBuffer rowSpec = new StringBuffer();
			for (int i = 0; i < 6; i++) {
				if (i > 0) {
					rowSpec.append(", ");
				}
				rowSpec.append("pref, ");
				rowSpec.append("3dlu");
			}
			JPanel detailsPanel = new JPanel(new FormLayout(
					"right:pref:none, 3dlu, left:pref", rowSpec.toString()));
			detailsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			JLabel authorLabel = new JLabel("Author:");
			authorField = new JTextField(model.getAuthor(), 40);
			detailsPanel.add(authorLabel, cc.xy(1, 1));
			detailsPanel.add(authorField, cc.xy(3, 1));

			JLabel ownerLabel = new JLabel("Owner:");
			ownerField = new JTextField(model.getOwner(), 40);
			detailsPanel.add(ownerLabel, cc.xy(1, 3));
			detailsPanel.add(ownerField, cc.xy(3, 3));

			JLabel privacyLabel = new JLabel("Privacy:");
			privacyComboBox = new JComboBox(new String[] { "Public",
					"Private" });
			if (model.getPrivacy() == GPConstants.ACCESS_PRIVATE) {
				privacyComboBox.setSelectedIndex(1);
			}
			detailsPanel.add(privacyLabel, cc.xy(1, 5));
			detailsPanel.add(privacyComboBox, cc.xy(3, 5));

			JLabel versionLabel = new JLabel("Version comment:");
			versionField = new JTextField(model.getVersionComment(),
					40);
			detailsPanel.add(versionLabel, cc.xy(1, 7));
			detailsPanel.add(versionField, cc.xy(3, 7));

			JLabel documentationLabel = new JLabel("Documentation:");
			final JComboBox existingDocComboBox = new JComboBox();
			if (name != null) {
				List docFiles = model.getServerDocFiles();
				for(int i = 0; i < docFiles.size(); i++) {
					existingDocComboBox.addItem(docFiles.get(i));
				}
			}
			
			JButton deleteDocBtn = new JButton("Delete");
			deleteDocBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					if(existingDocComboBox.getSelectedItem()==null) {
						return;
					}
					if(GUIUtil.showConfirmDialog("Are you sure you want to delete " + existingDocComboBox.getSelectedItem() + "?")) {
						Object obj = existingDocComboBox.getSelectedItem();
						if(obj instanceof LocalFileWrapper) {
							model.removeLocalDocFile(((LocalFileWrapper)obj).file);
						} else {
							model.removeServerDocFile((String)obj);
						}
						existingDocComboBox.removeItemAt(existingDocComboBox.getSelectedIndex());
					}
				}
			});
			
			JButton addDocBtn = new JButton("Add...");
			addDocBtn.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					File f = GUIUtil.showOpenDialog();
					if(f!=null) {
						model.addLocalDocFile(f);
						existingDocComboBox.addItem(new LocalFileWrapper(f));
					}
				}
			});
			
			detailsPanel.add(documentationLabel, cc.xy(1, 9));
			JPanel docPanel = new JPanel(new FormLayout("pref, 3dlu, pref, 3dlu, pref", "pref"));
			docPanel.add(existingDocComboBox, cc.xy(1, 1));
			docPanel.add(deleteDocBtn, cc.xy(3, 1));
			docPanel.add(addDocBtn, cc.xy(5, 1));
			detailsPanel.add(docPanel, cc.xy(3, 9));
			
			if(name!=null) {
				JLabel lsidLabel = new JLabel("LSID:");
				JLabel lsidField = new JLabel(model.getLSID());
				detailsPanel.add(lsidLabel, cc.xy(1, 11));
				detailsPanel.add(lsidField, cc.xy(3, 11));
			}
			TogglePanel detailsToggle = new TogglePanel("Details", detailsPanel);

			JPanel bottom = new JPanel(new BorderLayout());
			bottom.add(detailsToggle, BorderLayout.NORTH);
			bottom.add(buttonPanel, BorderLayout.SOUTH);
			add(temp, BorderLayout.CENTER);
			add(bottom, BorderLayout.SOUTH);
		}
	}

	private static class LocalFileWrapper {
		File file;
		
		LocalFileWrapper(File f) {
			this.file = f;
		}
		
		public String toString() {
			return file.getName();
		}
	}
	private void layoutTasks() {
		reset();
		for (int i = 0; i < model.getTaskCount(); i++) {
			layoutTask(i);
			tasksInPipelineComboBox.addItem((i + 1) + ". "
					+ model.getTaskName(i));
		}

		headerPanel = new HeaderPanel(model, buttonPanel);

		add(headerPanel, BorderLayout.NORTH);
		invalidate();
		validate();
		tasksPanel.invalidate();
		tasksPanel.validate();

	}

	private void layoutTask(final int taskIndex) {
		final JPopupMenu popupMenu = new JPopupMenu();

		final JMenuItem addTaskAfterItem = new JMenuItem("Add Task After");
		popupMenu.add(addTaskAfterItem);
		final JMenuItem addBeforeItem = new JMenuItem("Add Task Before");
		popupMenu.add(addBeforeItem);
		final JMenuItem moveUpItem = new JMenuItem("Move Up");
		popupMenu.add(moveUpItem);
		final JMenuItem moveDownItem = new JMenuItem("Move Down");
		popupMenu.add(moveDownItem);
		final JMenuItem deleteItem = new JMenuItem("Delete");
		popupMenu.add(deleteItem);

		ActionListener listener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == addTaskAfterItem) {
					showAddTask(taskIndex, true);
				} else if (e.getSource() == addBeforeItem) {
					showAddTask(taskIndex, false);
				} else if (e.getSource() == moveUpItem) {
					model.move(taskIndex, taskIndex - 1);
				} else if (e.getSource() == moveDownItem) {
					model.move(taskIndex, taskIndex + 1);
				} else if (e.getSource() == deleteItem) {
					model.remove(taskIndex);
				}
			}
		};

		addTaskAfterItem.addActionListener(listener);
		addBeforeItem.addActionListener(listener);
		moveUpItem.addActionListener(listener);
		moveDownItem.addActionListener(listener);
		deleteItem.addActionListener(listener);

		GroupPanel togglePanel = new GroupPanel((taskIndex + 1) + ". "
				+ model.getTaskName(taskIndex), new JTextField(model
				.getTaskDescription(taskIndex), 80));
		togglePanel.setBackground(getBackground());
		togglePanel.getMajorLabel().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				moveDownItem
						.setEnabled((taskIndex + 1) != model.getTaskCount());
				moveUpItem.setEnabled(taskIndex > 0);

				addBeforeItem.setEnabled(taskIndex != 0);
				addTaskAfterItem
						.setEnabled(taskIndex != model.getTaskCount() - 1
								|| model.getTaskCount() == 1);
				tasksInPipelineComboBox.setSelectedIndex(taskIndex);
				if (e.isPopupTrigger()
						|| e.getModifiers() == MouseEvent.BUTTON3_MASK) {
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		CellConstraints cc = new CellConstraints();

		tasksLayout.appendRow(new RowSpec("pref"));
		tasksPanel.add(togglePanel, cc.xywh(1, tasksLayout.getRowCount(), 2, 1,
				CellConstraints.LEFT, CellConstraints.BOTTOM));

		if(model.getParameterCount(taskIndex) > 0) {
			JLabel promptWhenRunLabel = new JLabel("Prompt when run");
			promptWhenRunLabel.setFont(promptWhenRunLabel.getFont().deriveFont(
				promptWhenRunLabel.getFont().getSize2D() - 2));
			togglePanel.addToggleComponent(promptWhenRunLabel);
			tasksLayout.appendRow(new RowSpec("pref"));
			tasksPanel.add(promptWhenRunLabel, cc.xyw(PROMPT_WHEN_RUN_COLUMN,
				tasksLayout.getRowCount(), tasksLayout.getColumnCount()));
		}
		int parameterStart = tasksLayout.getRowCount();
		addTaskParameters(taskIndex, togglePanel);
		int parameterEnd = tasksLayout.getRowCount();
	//	tasksPanel.addTask(togglePanel, parameterEnd - parameterStart);
		togglePanel.setExpanded(true);
		tasksLayout.appendRow(new RowSpec("1dlu"));
		tasksLayout.appendRow(new RowSpec("pref"));
		tasksPanel.add(new JSeparator(), cc.xyw(1, tasksLayout.getRowCount(), tasksLayout.getColumnCount()));
		tasksLayout.appendRow(new RowSpec("5dlu"));
	}

	void addTask(int index, AnalysisService svc) {
		model.add(index, svc.getTaskInfo());
	}

	private void showAddTask(int jobSubmissionIndex, boolean addAfter) {
		String title;
		int insertionIndex;

		if (addAfter) {
			if (model.getTaskCount() == 0) {
				title = "Add Task";
			} else {
				title = "Add Task After " + (jobSubmissionIndex + 1) + ". "
						+ model.getTaskName(jobSubmissionIndex);
			}
			insertionIndex = jobSubmissionIndex + 1;
		} else {
			title = "Add Task Before " + (jobSubmissionIndex + 1) + ". "
					+ model.getTaskName(jobSubmissionIndex);
			insertionIndex = jobSubmissionIndex + 1;
		}

		new TaskChooser(GenePattern.getDialogParent(), title, null,
				insertionIndex);
	}

	private void addTaskParameters(final int taskIndex, GroupPanel togglePanel) {
		MyTask taskDisplay = new MyTask(togglePanel, model
				.getParameterCount(taskIndex));
		taskDisplayList.add(taskDisplay);
		int startParameterRow = tasksLayout.getRowCount() + 1;
		for (int i = 0; i < model.getParameterCount(taskIndex); i++) {
			String paramName = model.getParameterName(taskIndex, i);
			CellConstraints cc = new CellConstraints();
			JLabel label = new JLabel(AnalysisServiceDisplay
					.getDisplayString(paramName)
					+ ":");
			togglePanel.addToggleComponent(label);
			tasksLayout.appendRow(new RowSpec("pref"));

			final JCheckBox promptWhenRunCheckBox = taskDisplay.parameters[i]
					.createPromptWhenRunCheckBox();

			promptWhenRunCheckBox.setSelected(model.isPromptWhenRun(taskIndex,
					i));

			togglePanel.addToggleComponent(promptWhenRunCheckBox);

			tasksPanel.add(promptWhenRunCheckBox, cc.xy(PROMPT_WHEN_RUN_COLUMN,
					tasksLayout.getRowCount(), CellConstraints.LEFT,
					CellConstraints.CENTER));

			tasksPanel.add(label, cc.xy(INPUT_LABEL_COLUMN, tasksLayout
					.getRowCount(), CellConstraints.RIGHT,
					CellConstraints.CENTER));

			if (model.isChoiceList(taskIndex, i)) {
				JComboBox comboBox = new JComboBox(model.getChoices(taskIndex,
						i));
				comboBox.setBackground(getBackground());
				String value = model.getValue(taskIndex, i);
				for (int j = 0; j < comboBox.getItemCount(); j++) {
					if (((ParameterChoice) comboBox.getItemAt(j))
							.equalsCmdLineOrUIValue(value)) {
						comboBox.setSelectedIndex(j);
						break;
					}
				}

				tasksPanel.add(comboBox, cc.xy(INPUT_FIELD_COLUMN, tasksLayout
						.getRowCount(), CellConstraints.LEFT,
						CellConstraints.BOTTOM));
				togglePanel.addToggleComponent(comboBox);
				taskDisplay.parameters[i].inputField = comboBox;
			} else if (model.isInputFile(taskIndex, i)) {
				String sendToString = (taskIndex + 1)
						+ ". "
						+ model.getTaskName(taskIndex)
						+ "."
						+ AnalysisServiceDisplay.getDisplayString(model
								.getParameterName(taskIndex, i));
				inputFileParameters.add(sendToString);
				inputFileTypes.add(model.getParameterInputTypes(taskIndex, i));
				
				final JTextField inputComponent = new ObjectTextField(20);
				taskDisplay.parameters[i].inputField = inputComponent;
				this.parameterName2ComponentMap.put(sendToString, taskDisplay.parameters[i]);
				JPanel inputPanel = new JPanel();
				inputPanel.setOpaque(false);
				inputPanel.setBackground(getBackground());
				FormLayout inputPanelLayout = new FormLayout(
						"left:pref:none, left:pref:none, left:pref:none, left:pref:none, left:default:none",
						"pref"); // input field, browse, task drop down,
				// output file drop down, checkbox
				inputPanel.setLayout(inputPanelLayout);

				final JButton browseBtn = new JButton("Browse...");
				taskDisplay.parameters[i].browseBtn = browseBtn;
				browseBtn.setOpaque(false);
				browseBtn.setBackground(getBackground());
				browseBtn.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						File f = GUIUtil.showOpenDialog();
						if (f != null) {
							inputComponent.setText(f.getPath());
						}
					}
				});

				final JCheckBox usePreviousOutput = taskDisplay.parameters[i]
						.createUseOutputFromPreviousTaskCheckBox();
				usePreviousOutput.setOpaque(false);
				usePreviousOutput.setBackground(getBackground());
				inputPanel.add(inputComponent, cc.xy(1, 1));
				inputPanel.add(browseBtn, cc.xy(2, 1));
				inputPanel.add(usePreviousOutput, cc.xy(5, 1));

				togglePanel.addToggleComponent(inputPanel);
				tasksPanel.add(inputPanel, cc.xy(INPUT_FIELD_COLUMN,
						tasksLayout.getRowCount(), CellConstraints.LEFT,
						CellConstraints.BOTTOM));

				Vector previousTaskNames = new Vector();
				previousTaskNames.add("Choose Task");
				for (int k = 0; k < taskIndex; k++) {
					previousTaskNames
							.add((k + 1) + ". " + model.getTaskName(k));
				}
				final JComboBox tasksComboBox = new JComboBox(previousTaskNames);
				taskDisplay.parameters[i].inheritedTaskIndex = tasksComboBox;
				tasksComboBox.setOpaque(false);
				final JComboBox outputFilesComboBox = new JComboBox();
				taskDisplay.parameters[i].inheritedFileName = outputFilesComboBox;
				outputFilesComboBox.setOpaque(false);
				inputPanel.add(tasksComboBox, cc.xy(3, 1));
				inputPanel.add(outputFilesComboBox, cc.xy(4, 1));

				int inheritedTaskIndex = model.getInheritedTaskIndex(taskIndex,
						i);
				browseBtn.setVisible(inheritedTaskIndex == -1);
				inputComponent.setVisible(inheritedTaskIndex == -1);
				tasksComboBox.setVisible(inheritedTaskIndex != -1);
				outputFilesComboBox.setVisible(inheritedTaskIndex != -1);
				usePreviousOutput.setSelected(inheritedTaskIndex != -1);

				tasksComboBox.addItemListener(new ItemListener() {

					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() != ItemEvent.SELECTED) {
							return;
						}
						String item = (String) tasksComboBox.getSelectedItem();
						if (item.equals("Choose Task")) {
							outputFilesComboBox.removeAllItems();
							outputFilesComboBox.setVisible(false);
						} else {
							outputFilesComboBox.setVisible(true);
							outputFilesComboBox.removeAllItems();

							List outputFileTypes = model
									.getOutputFileTypes(taskIndex);
							for (int i = 0; i < outputFileTypes.size(); i++) {
								outputFilesComboBox.addItem(outputFileTypes
										.get(i));
							}
							outputFilesComboBox.addItem("1st output");
							outputFilesComboBox.addItem("2nd output");
							outputFilesComboBox.addItem("3rd output");
							outputFilesComboBox.addItem("4th output");
							outputFilesComboBox.addItem("stdout");
							outputFilesComboBox.addItem("stderr");
						}
					}

				});
				tasksComboBox.setBackground(getBackground());
				outputFilesComboBox.setBackground(getBackground());

				if (inheritedTaskIndex != -1) {
					tasksComboBox.setSelectedItem((inheritedTaskIndex + 1)
							+ ". " + model.getTaskName(inheritedTaskIndex));
					outputFilesComboBox.setSelectedItem(model.getInheritedFile(
							taskIndex, i));
				} else {
					inputComponent.setText(model.getValue(taskIndex, i));
				}

			} else {
				JTextField inputComponent = new JTextField(20);
				if (!model.isPromptWhenRun(taskIndex, i)) {
					inputComponent.setText(model.getValue(taskIndex, i));
				}
				taskDisplay.parameters[i].inputField = inputComponent;
				tasksPanel.add(inputComponent, cc.xy(INPUT_FIELD_COLUMN,
						tasksLayout.getRowCount(), CellConstraints.LEFT,
						CellConstraints.BOTTOM));
				togglePanel.addToggleComponent(inputComponent);

			}
		}

		int endParameterRow = tasksLayout.getRowCount();
		int[] group = new int[endParameterRow - startParameterRow + 1];
		for (int i = startParameterRow, index = 0; i <= endParameterRow; i++, index++) {
			group[index] = i;
		}
		int[][] rowGroups = tasksLayout.getRowGroups();
		int[][] newRowGroups = new int[rowGroups.length + 1][];
		for (int i = 0; i < rowGroups.length; i++) {
			newRowGroups[i] = rowGroups[i];
		}
		newRowGroups[newRowGroups.length - 1] = group;
		tasksLayout.setRowGroups(newRowGroups);
	}

	public void pipelineChanged(PipelineEvent e) {
		model.print();
		layoutTasks();
	}

	static class MyTask {
		private GroupPanel togglePanel;

		private ParameterDisplay[] parameters;

		public MyTask(GroupPanel togglePanel, int parameterCount) {
			this.togglePanel = togglePanel;
			parameters = new ParameterDisplay[parameterCount];
			for (int i = 0; i < parameterCount; i++) {
				parameters[i] = new ParameterDisplay();
			}
		}

		public String getTaskDescription() {
			return ((JTextComponent) togglePanel.getMinorComponent()).getText().trim();
		}
		
		public void setExpanded(boolean b) {
			togglePanel.setExpanded(b);
		}

		public boolean isPromptWhenRun(int parameterIndex) {
			return parameters[parameterIndex].isPromptWhenRun();
		}

		public String getInheritedFileName(int parameterIndex) {
			return parameters[parameterIndex].getInheritedFileName();
		}

		public int getInheritedTaskIndex(int parameterIndex) {
			return parameters[parameterIndex].getInheritedTaskIndex();
		}

		public String getValue(int parameterIndex) {
			return parameters[parameterIndex].getValue();
		}
	}

	static class ParameterDisplay {
		/** a text field or combo box */
		private JComponent inputField;

		private JCheckBox promptWhenRun;

		private JCheckBox useOutputFromPreviousTask;

		private JComboBox inheritedTaskIndex;

		private JComboBox inheritedFileName;

		private JButton browseBtn;

		String getInheritedFileName() {
			return (String) inheritedFileName.getSelectedItem();
		}

		int getInheritedTaskIndex() {
			if (useOutputFromPreviousTask == null
					|| !useOutputFromPreviousTask.isVisible()
					|| !useOutputFromPreviousTask.isSelected()) {
				return -1;
			}
			String item = (String) inheritedTaskIndex.getSelectedItem();
			return Integer.parseInt(item.substring(0, item.indexOf(".")));
		}

		boolean isPromptWhenRun() {
			return promptWhenRun.isVisible() && promptWhenRun.isSelected();
		}

		JCheckBox createUseOutputFromPreviousTaskCheckBox() {
			useOutputFromPreviousTask = new JCheckBox(
					"Use output from previous task");
			useOutputFromPreviousTask.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					select();
				}
			});
			return useOutputFromPreviousTask;
		}

		JCheckBox createPromptWhenRunCheckBox() {
			promptWhenRun = new JCheckBox();
			promptWhenRun.setBackground(Color.white);
			
			promptWhenRun.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!promptWhenRun.isSelected()) {
						inputField.setVisible(true);
						if (useOutputFromPreviousTask != null) {
							browseBtn.setVisible(true);
							useOutputFromPreviousTask.setVisible(true);
						}
					} else {
						inputField.setVisible(false);
						if (useOutputFromPreviousTask != null) {
							useOutputFromPreviousTask.setSelected(false);
							useOutputFromPreviousTask.setVisible(false);
							browseBtn.setVisible(false);
							inheritedFileName.setVisible(false);
							inheritedTaskIndex.setVisible(false);
						}
					}

				}

			});
			return promptWhenRun;
		}

		private void select() {
			browseBtn.setVisible(!useOutputFromPreviousTask.isSelected());
			
			inputField.setVisible(!useOutputFromPreviousTask.isSelected());
			inheritedTaskIndex.setVisible(useOutputFromPreviousTask
					.isSelected());
			inheritedFileName.setVisible(useOutputFromPreviousTask.isSelected()
					&& inheritedFileName.getItemCount() > 0);
			promptWhenRun.setSelected(false);
		}

		void setValue(Sendable s) {
			if (inputField instanceof JTextField) {
				((ObjectTextField) inputField).setObject(s);
				useOutputFromPreviousTask.setVisible(true);
				useOutputFromPreviousTask.setSelected(false);
				select();
			}
		}
		
		String getValue() {
			if (inputField instanceof JTextField) {
				return ((JTextComponent) inputField).getText().trim();
			}
			ParameterChoice pc = (ParameterChoice) ((JComboBox) inputField).getSelectedItem();
			return pc.getValue();
		}
	}

}
