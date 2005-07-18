package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.genepattern.codegenerator.JavaPipelineCodeGenerator;
import org.genepattern.codegenerator.MATLABPipelineCodeGenerator;
import org.genepattern.codegenerator.RPipelineCodeGenerator;
import org.genepattern.codegenerator.TaskCodeGenerator;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.LSIDUtil;
import org.genepattern.gpge.ui.maindisplay.TogglePanel;
import org.genepattern.util.BrowserLauncher;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskIntegratorProxy;
import org.genepattern.webservice.WebServiceException;

/**
 *  Displays an <tt>AnalysisService</tt>
 *
 * @author    Joshua Gould
 */
public class AnalysisServiceDisplay extends JPanel {
	/** the currently displayed <tt>AnalysisService</tt> */
	private AnalysisService selectedService;

	/** whether the <tt>selectedService</tt> has documentation */
	private volatile boolean hasDocumentation;

	private boolean advancedGroupExpanded = false;

	private TogglePanel togglePanel;

	private ParameterInfoPanel parameterInfoPanel;

	public AnalysisServiceDisplay() {
		this.setBackground(Color.white);
		showGettingStarted();
	}

	public void showGettingStarted() {
		java.net.URL url = ClassLoader
				.getSystemResource("org/genepattern/gpge/resources/getting_started.html");

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

	private Border createBorder(final Border b, final int left, final int top,
			final int right, final int bottom) {
		return new javax.swing.border.Border() {
			public Insets getBorderInsets(java.awt.Component c) {
				Insets i = b.getBorderInsets(c);
				if (left >= 0) {
					i.left = left;
				}
				if (top >= 0) {
					i.top = top;
				}
				if (right >= 0) {
					i.right = right;
				}
				if (bottom >= 0) {
					i.bottom = bottom;
				}

				return i;
			}

			public boolean isBorderOpaque() {
				return b.isBorderOpaque();
			}

			public void paintBorder(Component c, Graphics g, int x, int y,
					int width, int height) {
				b.paintBorder(c, g, x, y, width, height);
			}

		};
	}

	/**
	 *  Displays the given analysis service
	 *
	 * @param  selectedService  Description of the Parameter
	 */
	public void loadTask(AnalysisService _selectedService) {
		this.selectedService = _selectedService;
		hasDocumentation = true;
		if (togglePanel != null) {
			advancedGroupExpanded = togglePanel.isExpanded();
		}
		if (selectedService != null) {
			new Thread() {
				public void run() {
					try {

						String username = AnalysisServiceManager.getInstance()
								.getUsername();
						String server = selectedService.getServer();
						String lsid = LSIDUtil.getTaskId(selectedService
								.getTaskInfo());
						String[] supportFileNames = new TaskIntegratorProxy(
								server, username).getSupportFileNames(lsid);
						hasDocumentation = supportFileNames != null
								&& supportFileNames.length > 0;
					} catch (WebServiceException wse) {
						wse.printStackTrace();
					}
				}
			}.start();
		}

		TaskInfo taskInfo = selectedService.getTaskInfo();
		String taskName = taskInfo.getName();

		JPanel topPanel = new TaskNamePanel(taskInfo,
				ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST);

		ParameterInfo[] params = taskInfo.getParameterInfoArray();

		parameterInfoPanel = new ParameterInfoPanel(taskName, params);
		removeAll();

		setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JButton submitButton = new JButton("Run");
		submitButton.addActionListener(new SubmitActionListener());
		buttonPanel.add(submitButton);

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ResetActionListener());
		buttonPanel.add(resetButton);
		JButton helpButton = new JButton("Help");
		helpButton.addActionListener(new HelpActionListener());
		buttonPanel.add(helpButton);

		JButton editButton = new JButton("Edit");
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_EDIT_PIPELINE_REQUEST, selectedService));
			}
		});
		buttonPanel.add(editButton);
		
		JButton viewButton = new JButton("View");
		viewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				MessageManager.notifyListeners(new ChangeViewMessageRequest(this, ChangeViewMessageRequest.SHOW_VIEW_PIPELINE_REQUEST, selectedService));
			}
		});
		buttonPanel.add(viewButton);
		
		JPanel viewCodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JLabel viewCodeLabel = new JLabel("View Code:");
		final JComboBox viewCodeComboBox = new JComboBox(new Object[] { "Java",
				"MATLAB", "R" });

		viewCodePanel.add(viewCodeLabel);
		viewCodePanel.add(viewCodeComboBox);

		togglePanel = new TogglePanel("Advanced", viewCodePanel);
		togglePanel.setExpanded(advancedGroupExpanded);
		/*JTaskPaneGroup group = new JTaskPaneGroup();
		 group.setText("Advanced Options");
		 JTaskPane tp = new JTaskPane();
		 group.add(viewCodePanel);
		 tp.add(group);*/

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBorder(createBorder(UIManager.getLookAndFeelDefaults()
				.getBorder("ScrollPane.border"), 0, 0, 0, 2));

		bottomPanel.add(buttonPanel, BorderLayout.CENTER);
		bottomPanel.add(togglePanel, BorderLayout.SOUTH);

		viewCodeComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
		final javax.swing.border.Border b = sp.getBorder();
		sp.setBorder(createBorder(b, 0, -1, -1, -1));
		add(sp, BorderLayout.CENTER);

		add(bottomPanel, BorderLayout.SOUTH);
		setMinimumSize(new java.awt.Dimension(100, 100));
		revalidate();
		doLayout();
	}

	static JTextArea createWrappedLabel(String s) {
		JTextArea jTextArea = new JTextArea();
		// Set JTextArea to look like JLabel
		jTextArea.setWrapStyleWord(true);
		jTextArea.setLineWrap(true);
		jTextArea.setEnabled(false);
		jTextArea.setEditable(false);
		jTextArea.setOpaque(false);
		jTextArea.setFont(javax.swing.UIManager.getFont("Label.font"));
		jTextArea.setBackground(UIManager.getColor("Label.background"));
		jTextArea.setDisabledTextColor(UIManager.getColor("Label.foreground"));
		jTextArea.setText(s);
		return jTextArea;
	}

	/**
	 *  Sets the value of the given parameter to the given node
	 *
	 * @param  parameterName  the parmeter name
	 * @param  node           a tree node
	 */
	public void setInputFile(String parameterName,
			javax.swing.tree.TreeNode node) {
		if (selectedService != null) {
			ObjectTextField tf = (ObjectTextField) parameterInfoPanel
					.getComponent(parameterName);
			if (tf != null) {
				tf.setObject(node);
			}
		}
	}

	public static String getDisplayString(ParameterInfo p) {
		return getDisplayString(p.getName());
	}

	public static String getDisplayString(String name) {
		return name.replace('.', ' ');
	}

	/**
	 *  Returns <tt>true</tt> of this panel is showing an <tt>AnalysisService
	 *  </tt>
	 *
	 * @return    whether this panel is showing an <tt>AnalysisService</tt>
	 */
	public boolean isShowingAnalysisService() {
		return selectedService != null;
	}

	/**
	 *  Gets a collection of input file parameters
	 *
	 * @return    the input file names
	 */
	public java.util.Iterator getInputFileParameters() {
		return parameterInfoPanel.getInputFileParameters();
	}

	private class ResetActionListener implements ActionListener {
		public final void actionPerformed(ActionEvent ae) {
			loadTask(selectedService);
		}
	}

	private class HelpActionListener implements ActionListener {
		public final void actionPerformed(java.awt.event.ActionEvent ae) {
			try {
				String username = AnalysisServiceManager.getInstance()
						.getUsername();
				String server = selectedService.getServer();
				String lsid = LSIDUtil.getTaskId(selectedService.getTaskInfo());

				if (hasDocumentation) {
					String docURL = server + "/gp/getTaskDoc.jsp?name=" + lsid
							+ "&" + GPConstants.USERID + "="
							+ java.net.URLEncoder.encode(username, "UTF-8");
					org.genepattern.util.BrowserLauncher.openURL(docURL);
				} else {
					GenePattern.showMessageDialog(selectedService.getTaskInfo()
							.getName()
							+ "has no documentation");
				}
			} catch (java.io.IOException ex) {
				System.err.println(ex);
			}
		}
	}

	private class SubmitActionListener implements ActionListener {
		public final void actionPerformed(ActionEvent ae) {
			final JButton source = (JButton) ae.getSource();
			try {
				source.setEnabled(false);
				final ParameterInfo[] actualParameterArray = parameterInfoPanel
						.getParameterInfoArray();
				final AnalysisService _selectedService = selectedService;
				final String username = AnalysisServiceManager.getInstance()
						.getUsername();

				new Thread() {
					public void run() {
						RunTask rt = new RunTask(_selectedService,
								actualParameterArray, username);
						rt.exec();
					}
				}.start();
			} finally {
				if (TaskLauncher.isVisualizer(selectedService)) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				source.setEnabled(true);
			}
		}
	}

}
