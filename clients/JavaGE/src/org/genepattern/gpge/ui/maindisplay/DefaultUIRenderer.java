/*
 DefaultUIRenderer.java
 Created on March 27, 2003, 5:35 PM
 */
package org.genepattern.gpge.ui.maindisplay;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.io.AbstractDataSource;
import org.genepattern.gpge.io.DataObjectProxy;
import org.genepattern.gpge.io.DataSource;
import org.genepattern.gpge.io.ServerFileDataSource;
import org.genepattern.gpge.ui.tasks.ParamRetrievor;
import org.genepattern.gpge.ui.tasks.UIRenderer;
import org.genepattern.modules.ui.graphics.FloatField;
import org.genepattern.modules.ui.graphics.IntegerField;
import org.genepattern.util.ExceptionHandler;
import org.genepattern.util.GPConstants;
import org.genepattern.util.PropertyFactory;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.gpge.ui.graphics.draggable.TransferableTreePath;
import org.genepattern.gpge.ui.maindisplay.VisualizerTaskSubmitter;
import org.genepattern.gpge.ui.maindisplay.LSIDUtil;
import org.genepattern.gpge.ui.project.ProjectDirModel;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;

/**
 * @author kohm
 * @created February 9, 2004
 */
public class DefaultUIRenderer implements UIRenderer {

	// fields
	private GridBagConstraints gbc1 = new GridBagConstraints();

	private GridBagConstraints gbc2 = new GridBagConstraints();

	private GridBagConstraints remainder = new GridBagConstraints();

	/** listener for help button presses */
	private ActionListener help_listener;

	private JPanel pane;

	private int _gridy = 0;

	/** the min size of the text scroll panes */
	private final static java.awt.Dimension MINIMUM_SCROLL_SIZE = new java.awt.Dimension(
			200, 50);

	/** the top inset value for a JScrollPane */
	private int scroll_top_inset = -5;

	/**
	 * the insets value for the bottom value so that the regular conponents are
	 * aligned to the top of the Description box
	 */
	private int fill_height;

	/** the actual or estimated height of a JLabel given the current font */
	private int label_height;

	/** the font to create all components with */
	private java.awt.Font font;

	/** the bold font to create some components with */
	private java.awt.Font bold_font;


	/** maps parameter names to the text field for a parameter */
	private java.util.Map inputFileParameterNameToTextFieldMap = new java.util.LinkedHashMap();
   
   private AnalysisService service;
   private java.util.List params_list;
   private Map name_retriever;
  
	public java.util.Iterator getInputFileParameterNames() {
		return inputFileParameterNameToTextFieldMap.keySet().iterator();
	}

	public void setInputFile(String parameterName,
			javax.swing.tree.TreeNode node) {
		ObjectTextField tf = (ObjectTextField) inputFileParameterNameToTextFieldMap
				.get(parameterName);
		tf.setObject(node);
	}

	/**
	 * renders as many parameters as it can on the supplied JComponent The input
	 * List of ParameterInfo objects will contain those that were not processed.
	 * The specified Map will map parameter names to ParamRetrievor instances.
	 * 
	 * @param pane
	 *            Description of the Parameter
	 * @param service
	 *            Description of the Parameter
	 * @param params_list
	 *            Description of the Parameter
	 * @param name_retriever
	 *            Description of the Parameter
	 */
	public void render(final JComponent pane, final AnalysisService service,
			final java.util.List params_list, final Map name_retriever) {
		inputFileParameterNameToTextFieldMap.clear();
		this.pane = (JPanel) pane;
		this.service = service;
		this.params_list = params_list;
		this.name_retriever = name_retriever;
		addParameters(service, params_list, name_retriever);
	}

	private void addParameters(final AnalysisService service,
			final java.util.List params_list, final Map name_retriever) {
		setFont(pane.getFont()); 

		final int fill_height = calcFillHeight(pane, getFont());
		final int rel = GridBagConstraints.RELATIVE;
		final int rem = GridBagConstraints.REMAINDER;
		setGridBagConstraints(gbc1, rel, rel, 1, 1, 100, 1);
		gbc1.anchor = gbc2.anchor = GridBagConstraints.WEST;
		gbc1.insets = new Insets(0, 10, fill_height, 0);
      
		setGridBagConstraints(gbc2, rel, rel, 1, 1, 100, 1);
		setGridBagConstraints(remainder, rel, rel, rem, 1, 100, 1);
		gbc2.insets = new Insets(0, 0, fill_height, 0);
		_gridy = 0;
		gbc1.anchor = gbc2.anchor = GridBagConstraints.WEST;
		final TaskInfo task = service.getTaskInfo();
		if (task != null) {
			final ParameterInfo[] params = (ParameterInfo[]) params_list
					.toArray(new ParameterInfo[params_list.size()]);
			//loop through the parameters to create UI
			int i;
			final int length = (params != null) ? params.length : 0;
			_gridy++;
			for (i = 0; i < length; i++) {
				ParameterInfo info = params[i];
				createParamPanel(task, info, name_retriever);
			}
		}
		
		params_list.clear();
	}

	private JPanel createParamPanel(final TaskInfo task,
			final ParameterInfo info, final Map name_retriever) {
		final String param_name = info.getName();

		final String param_desc = info.getDescription();
		
		if (param_name.equals("className")) {
			task.getTaskInfoAttributes().put(param_name, param_desc);
			return null;
		}
		final String param_label = info.getLabel();
		final String param_value = info.getValue();
		
		//check the parameter value to decide which kind of JComponent to
		// create
		if (param_value == null || param_value.equals("")) {
			if (info.isInputFile()) {
				createInputFileField(param_name, param_label, param_desc, info,
						name_retriever);
			} else {
				createTextInput(param_name, param_label, param_desc, info,
						name_retriever);
			}
		} else {
			createComboBox(param_name, param_label, param_desc, param_value,
					info, name_retriever);
		}

		return null; //panel;
	}

	private JComponent createComboBox(final String param_name,
			final String label, final String descr, final String param_value,
			final ParameterInfo info, final Map name_retriever) {
		// get default
		final String default_val = ((String) info.getAttributes().get(
				GPConstants.PARAM_INFO_DEFAULT_VALUE[0])).trim();
		final ChoiceItem default_item = createDefaultChoice(default_val);

		pane.add(createLabel(param_name, info), gbc1);
		final StringTokenizer tokenizer = new StringTokenizer(param_value, ";");
		final JComboBox list = new JComboBox();
		list.setFont(getFont());
		int selectIndex = -1;
		for (int i = 0; tokenizer.hasMoreTokens(); i++) {
			final String token = tokenizer.nextToken();
			final ChoiceItem item = createChoiceItem(token);
			list.addItem(item);
			if (selectIndex < 0 && item.hasToken(default_item)) {
				selectIndex = i;
			}
		}

		if (selectIndex >= 0) {
			list.setSelectedIndex(selectIndex);
		} else if (default_val != null && default_val.length() > 0) {
			GenePattern.showWarning(null, "Default \"" + default_val
					+ "\" does not match any values in "
					+ "the drop-down menu for parameter " + param_name);
		}
		pane.add(list, gbc2);
		addDescription(pane, descr);

		name_retriever.put(param_name, new NoFileParamRetrievor(info) {
			/**
			 * returns the value
			 * 
			 * @return Description of the Return Value
			 */
			public final String internalGetValue() {
				return (String) ((ChoiceItem) list.getSelectedItem())
						.getValue();
			}
		});
		return null; //panel;
	}

	/**
	 * Parses the String and returns a ChoiceItem
	 * 
	 * @param string
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private ChoiceItem createChoiceItem(final String string) {
		final int index = string.indexOf('=');
		ChoiceItem choice = null;
		if (index < 0) {
			choice = new ChoiceItem(string, string);
		} else {
			choice = new ChoiceItem(string.substring(index + 1), string
					.substring(0, index));
		}
		//choice.setFont(getFont());
		return choice;
	}

	/**
	 * creates the default <CODE>ChoiceItem</CODE> for the combo box
	 * 
	 * @param default_val
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private ChoiceItem createDefaultChoice(final String default_val) {
		if (default_val != null && default_val.length() > 0) {
			return createChoiceItem(default_val);
		}
		return null;
	}

	/**
	 * creates a JTextField
	 * 
	 * @param param_name
	 *            Description of the Parameter
	 * @param label
	 *            Description of the Parameter
	 * @param descr
	 *            Description of the Parameter
	 * @param info
	 *            Description of the Parameter
	 * @param name_retriever
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private JComponent createTextInput(final String param_name,
			final String label, final String descr, final ParameterInfo info,
			final Map name_retriever) {
		//final JPanel panel = new JPanel();// FlowLayout
		pane.add(createLabel(param_name, info), gbc1);
		//final JTextField field = new JTextField(15);
		final JTextField field = createProperTextField(info);
		// set default
		final String default_val = (String) info.getAttributes().get(
				GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

		try {
			if (default_val != null && default_val.trim().length() > 0) {
				field.setText(default_val);
			} else {
				// if optional value and no default, clear the field
				if (isOptional(info)) {
					field.setText(null);
				}
			}
		} catch (NumberFormatException ex) {
			GenePattern.getReporter().showWarning(
					"Cannot set default value for parameter " + param_name
							+ ":", ex);
		}
		field.setFont(getFont());
		pane.add(field, gbc2);
		addDescription(pane, descr);
		name_retriever.put(param_name, new NoFileParamRetrievor(info) {
			/**
			 * returns the value
			 * 
			 * @return Description of the Return Value
			 */
			public final String internalGetValue() {
				return field.getText();
			}
		});
		return null; //panel;
	}

	/**
	 * determines if the parameter is optional
	 * 
	 * @param info
	 *            Description of the Parameter
	 * @return The optional
	 */
	protected final boolean isOptional(final ParameterInfo info) {
		final Object optional = info.getAttributes().get("optional");
		return (optional != null && "on".equalsIgnoreCase(optional.toString()));
	}

	/**
	 * gets the proper input field object
	 * 
	 * @param info
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	protected final JTextField createProperTextField(final ParameterInfo info) {
		final int num_cols = 15;
		JTextField field = null;
		final Object value = info.getAttributes().get("type");
		if (value == null || value.equals("java.lang.String")) {
			field = new JTextField(num_cols);
			field.setToolTipText("Type in text");
		} else if (value.equals("java.lang.Integer")) {
			field = new IntegerField(num_cols);
			field.setToolTipText("Type in integer values");
		} else if (value.equals("java.lang.Float")) {
			field = new FloatField(num_cols);
			field.setToolTipText("Type in floating point values");
		} else {
			org.genepattern.util.AbstractReporter.getInstance().showWarning(
					"Don't have input field associated with " + value);
			field = new JTextField(num_cols);
			field.setToolTipText("Type in text");
		}
		return field;
	}

	/**
	 * determines if the label needs a ':' and a space
	 * 
	 * @param desc
	 *            Description of the Parameter
	 * @param info
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private JLabel createLabel(String desc, final ParameterInfo info) {
		final String trimmed = desc.trim();
		JLabel nameLabel = null;
		if (trimmed.length() == 0) {
			nameLabel = new JLabel("Unlabeled:  ");
			return fixLabel(nameLabel, info);
		}
		// replace the dot "." with space i.e hi.there => hi there
		desc = desc.replace('.', ' ');
		desc = desc.replace('_', ' ');
		desc = org.genepattern.util.StringUtils.capitalize(desc);
		final int colon_pos = desc.lastIndexOf(':');

		if (colon_pos < 0) {
			nameLabel = createLabel(trimmed + ":  ", info);
		} else {
			if (colon_pos == desc.length() - 1) { // needs padding
				nameLabel = new JLabel(desc + "  ");
			} else {
				nameLabel = new JLabel(desc);
			}
		}
		return fixLabel(nameLabel, info);
	}

	/**
	 * fixes the Label with tooltip and bolded if a required parameter
	 * 
	 * @param label
	 *            Description of the Parameter
	 * @param info
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private JLabel fixLabel(final JLabel label, final ParameterInfo info) {
		final boolean optional = isOptional(info);
		if (optional) {
			label.setToolTipText("Optional parameter can be left blank");
			label.setFont(getFont());
		} else {
			label.setToolTipText("Required parameter must be filled in");
			final Font font = getFont();
			label.setFont(getBoldFont());
		}
		return label;
	}

	private JComponent createInputFileField(final String param_name,
			final String label, final String descr, final ParameterInfo info,
			final Map name_retriever) {
		pane.add(createLabel(param_name, info), gbc1);
      
		final JScrollPane scroll = createObjectTextField(label);
		final ObjectTextField field = (ObjectTextField) scroll.getViewport()
				.getView();
		inputFileParameterNameToTextFieldMap.put(param_name, field);
		String defaultValue = (String) info.getAttributes().get(
				GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

		if (defaultValue != null && !defaultValue.trim().equals("")) {
			File f = new File(defaultValue);
			if (f.exists()) {
				field.setObject(f);
			} else if (defaultValue.startsWith("http://")
					|| defaultValue.startsWith("https://")
					|| defaultValue.startsWith("ftp:")) {
				try {
					field.setObject(new URL(defaultValue));
				} catch (MalformedURLException mue) {
					System.err.println(mue + " while creating input field "
							+ param_name + " for " + defaultValue);
				}
			} else { // jgould added for reload of server file
				field.setObject(defaultValue);
			}

		}
		field.setFont(getFont());
		pane.add(scroll, gbc2);
		addDescription(pane, descr);

		name_retriever.put(param_name, new AbstractParamRetrievor(info) {
			/**
			 * returns the value
			 * 
			 * @return Description of the Return Value
			 * @exception IOException
			 *                Description of the Exception
			 */
			protected final String internalGetValue() throws IOException {
				final Object obj = field.getObject();
				if (obj == null) {
					return null;
				}
				InputStream in = null;
				if (obj instanceof DataObjectProxy) {
					final DataObjectProxy proxy = (DataObjectProxy) obj;
					final DataSource source = proxy.getDataSource();
					//FIXME this could be very inefficient
					// if the source is pointing to files already on the server!
					in = source.getRawInputStream(proxy);
				} else if (obj instanceof java.io.File) {
					final File file = (File) obj;
					in = new FileInputStream(file);
				} else if (obj instanceof java.net.URL) {
					// even though it may not be a URL from the server this is
					// needed
					//info.getAttributes().put(ParameterInfo.MODE,
					// ParameterInfo.URL_INPUT_MODE);
					//info.getAttributes().put(ParameterInfo.MODE,
					// ParameterInfo.INPUT_MODE);
					info.setAttributes(new HashMap(3));
					// Note Exiting the method here!
					return obj.toString();
				} else {
					throw new IllegalStateException(
							"Unknown object from ObjectTextField (name="
									+ param_name + ", label=" + label + "): "
									+ obj);
				}
				final BufferedReader reader = new BufferedReader(
						new InputStreamReader(in));
				final StringBuffer buff = new StringBuffer();
				try {
					final char NL = '\n';
					for (String tmp_line = reader.readLine(); tmp_line != null; tmp_line = reader
							.readLine()) {
						buff.append(tmp_line);
						buff.append(NL);
					}
				} catch (EOFException ex) {
				}
				return buff.toString();
			}

			protected final boolean internalIsFile() {
				return (!(field.getObject() instanceof java.net.URL));
			}

			/**
			 * @return Description of the Return Value
			 */
			protected final boolean internalIsServerFile() {
				final Object obj = field.getObject();
				if (obj == null) {
					return false;
				}
				return (obj instanceof org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode);
			}

			protected final String internalGetSourceName()
					throws java.io.IOException {
				final Object obj = field.getObject();
				if (obj == null) {
					return null;
				}

				if (obj instanceof String) { // reloaded job where input file is
											 // on server
					info.getAttributes().put(ParameterInfo.TYPE,
							ParameterInfo.FILE_TYPE);

					info.getAttributes().put(ParameterInfo.MODE,
							ParameterInfo.CACHED_INPUT_MODE);
					return obj.toString();
				}
				if (obj instanceof org.genepattern.gpge.ui.project.ProjectDirModel.FileNode) {
					info.setAsInputFile();
					ProjectDirModel.FileNode node = (ProjectDirModel.FileNode) obj;
					return node.file.getCanonicalPath();
				} else if (obj instanceof org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode) {
					info.getAttributes().put(ParameterInfo.TYPE,
							ParameterInfo.FILE_TYPE);

					info.getAttributes().put(ParameterInfo.MODE,
							ParameterInfo.CACHED_INPUT_MODE);
					org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode node = (org.genepattern.gpge.ui.tasks.JobModel.ServerFileNode) obj;
					return node.getParameterValue();

				} else if (obj instanceof java.io.File) {
					info.setAsInputFile();
					//	info.getAttributes().put(ParameterInfo.MODE,
					// ParameterInfo.INPUT_MODE);
					final File drop_file = (File) obj;
					return drop_file.getCanonicalPath();
				} else if (obj instanceof java.net.URL) {
					// even though it may not be a URL from the server this is
					// needed
					//info.getAttributes().put(ParameterInfo.MODE,
					// ParameterInfo.URL_INPUT_MODE);
					return obj.toString();
				} else {
					throw new IllegalStateException(
							"Unknown object from ObjectTextField (name="
									+ param_name + ", label=" + label + "): "
									+ obj);
				}
			}
		});

		return null; //panel;
	}

	/**
	 * @param name
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	protected final static String fixName(final String name) {
		String better_name = name;
		final int index = name.lastIndexOf('\\');
		if (index > 0) {
			better_name = name.substring(index + 1);
		}

		if (better_name.startsWith("Axis")) {
			better_name = better_name.substring(better_name.indexOf('_') + 1);
		}

		final String seperator = org.genepattern.io.StorageUtils.TEMP_FILE_MARKER;
		final int sep_ind = name.indexOf(seperator);
		if (sep_ind < 0) {
			better_name = better_name.substring(sep_ind + seperator.length()
					+ 2);
		}
		return better_name;
	}

	protected static void setGridBagConstraints(final GridBagConstraints gbc,
			final int gridx, final int gridy, final int gridw, final int gridh,
			final int weightx, final int weighty) {
		gbc.gridx = gridx;
		gbc.gridy = gridy;
		gbc.gridwidth = gridw;
		gbc.gridheight = gridh;
		gbc.weightx = weightx;
		gbc.weighty = weighty;
	}

	/**
	 * creates an ObjectTextField in a consistent way
	 * 
	 * @param type
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private JScrollPane createObjectTextField(final String type) {
		final ObjectTextField field = new ObjectTextField(null, 20);
		field.setDropType(type);
		final JScrollPane scroll = new JScrollPane(field);
		scroll.setFont(getFont());
		field.setFont(getFont());
		return scroll;
	}

	/**
	 * conditionaly creates the description
	 * 
	 * @param panel
	 *            The feature to be added to the Description attribute
	 * @param description
	 *            The feature to be added to the Description attribute
	 */
	private void addDescription(final JPanel panel, final String description) {
		String desc = "";
		if (description != null) {
			desc = description.trim();
		}

		if (desc.length() > 0) {

			final JEditorPane pane = new JEditorPane("text/html", desc) {
				/**
				 * keep this from getting the focus
				 * 
				 * @return The focusTraversable
				 */
				public boolean isFocusTraversable() {
					return false;
				}
			};
			pane.setEditable(false);
			pane.setCaretPosition(0);
			pane.setBackground(panel.getBackground());
			pane.setFont(getFont());
			final JScrollPane scroll = new JScrollPane(pane);

			scroll.setToolTipText("Description");
			pane.setToolTipText("Description");
			scroll.setPreferredSize(MINIMUM_SCROLL_SIZE);
			scroll.setMinimumSize(MINIMUM_SCROLL_SIZE);
			scroll.setFont(getFont());
			panel.add(scroll, remainder);
			//return ;
		} else {
			final JLabel label = new JLabel();
			panel.add(label, remainder);

		}

	}

	/**
	 * figures the height of "regular" Swing components (JLabel JComboBox,
	 * JCheckBox, etc)
	 * 
	 * @param component
	 *            Description of the Parameter
	 * @param fnt
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	protected final int calcFillHeight(final JComponent component,
			final java.awt.Font fnt) {
		if (fill_height <= 0) {
			if (label_height <= 0) { // negative values are approximations and
									 // need redoing
				final JLabel label = new JLabel();
				label.setFont(fnt);
				label_height = label.getUI().getPreferredSize(label).height;
			}
			if (label_height == 0) { // approximation
				java.awt.FontMetrics metrics = component.getFontMetrics(fnt);
				label_height = -(metrics.getHeight() + 5); // negative height
														   // value
			}
			if (scroll_top_inset < 0) {
				final JScrollPane scroll = new JScrollPane();
				final int top = scroll.getInsets().top;
				if (top > 0) {
					scroll_top_inset = top;
				}
			}
			final int height = MINIMUM_SCROLL_SIZE.height
					- ((label_height > 0) ? label_height : -(label_height))
					- ((scroll_top_inset > 0) ? scroll_top_inset
							: -(scroll_top_inset)) - 2;
			fill_height = height;
		}
		return fill_height;
	}

	// UI like methods

	/**
	 * sets the current Font for all swing objects produced
	 * 
	 * @param new_font
	 *            The new font value
	 */
	public void setFont(final Font new_font) {
		if (new_font.equals(font)) {
			return;
		} // do nothing
		//        if( new_font.getCharacterHeight() != old_character_height) {
		this.fill_height = 0;
		this.label_height = 0;
		this.scroll_top_inset = -5;
		//FIXME need to do something about the Preferred Size of the
		// Description JScrollPane
		// the height should be adjusted to represent the new font height
		// MINIMUM_SCROLL_SIZE.height = some_calculation
		//        }
		this.font = new_font;
		this.bold_font = new Font(font.getName(), Font.BOLD, font.getSize());
	}

	/**
	 * returns the <CODE>Font</CODE> used when making swing components
	 * 
	 * @return The font
	 */
	public Font getFont() {
		return this.font;
	}

	/**
	 * returns the bolded <CODE>Font</CODE> used when making swing components
	 * 
	 * @return The boldFont
	 */
	public Font getBoldFont() {
		return this.bold_font;
	}

	public JComponent createSubmitPanel(final AnalysisService service,
			final java.awt.event.ActionListener submitListener) {
		return createSubmitPanel(service, submitListener, null);
	}

	/**
	 * creates a component that is or contains another component that the user
	 * can press (a JButton) to submit the job
	 * 
	 * @param service
	 *            Description of the Parameter
	 * @param listener
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	public JComponent createSubmitPanel(final AnalysisService service,
			final java.awt.event.ActionListener submitListener,
			java.awt.event.ActionListener resetListener) {
		final JPanel panel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = gbc.weighty = 1.0;
		final String label = (VisualizerTaskSubmitter.isVisualizer(service)) ? "Show"
				: "Submit";
		final JButton submit = new JButton(label);
		submit.addActionListener(submitListener);
		panel.add(submit, gbc);

		if (resetListener != null) {
			JButton reset = new JButton("Reset");
			reset.addActionListener(resetListener);
			panel.add(reset, gbc);
		}

		 help_listener = new ActionListener() {
          public final void actionPerformed(java.awt.event.ActionEvent ae) {
				try {
					String server =  service.getServer();
					String docURL = server + "/gp/getTaskDoc.jsp?name="
							+ LSIDUtil.getTaskId(service.getTaskInfo()) + "&"
							+ GPConstants.USERID + "=GenePattern";
					org.genepattern.util.BrowserLauncher.openURL(docURL);
				} catch (java.io.IOException ex) {
					ExceptionHandler.handleException(ex);
				}
			}
       };
      final JButton help = new JButton("Help");
      help.addActionListener(help_listener);
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(help, gbc);
		
		return panel;
	}

	/**
	 * creates a component that is the label for the analysis service.
	 * 
	 * @param service
	 *            the analysis service that defines a task
	 * @return JComponent the label or name of this task
	 */
	public JComponent createTaskLabel(AnalysisService service) {
		final String name = service.getTaskInfo().getDescription();
		//FIXME should be a TextComponent that handles HTML
		return new JLabel(name);
	}

	// I N N E R C L A S S E S

	

	/**
	 * Abstact implemtation for those non file ParamRetrievor implementations
	 * 
	 * @author jgould
	 * @created February 9, 2004
	 */
	private abstract static class AbstractParamRetrievor implements ParamRetrievor {

		/** which method to call */
		private String method;

		private Object value;

		private boolean result;

		private final ParameterInfo paraminfo;

		/**
		 * constructor
		 * 
		 * @param info
		 *            Description of the Parameter
		 */
		protected AbstractParamRetrievor(final ParameterInfo info) {
			paraminfo = info;
		}

		/**
		 * returns the value
		 * 
		 * @return Description of the Return Value
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		protected abstract String internalGetValue() throws java.io.IOException;

		/**
		 * returns the value in an Exception-safe way
		 * 
		 * @return The value
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public final String getValue() throws java.io.IOException {
			method = "getValue";
			runIt(); // calls runIt()
			return (String) value;
		}

		/**
		 * returns true if the value should be in a file
		 * 
		 * @return Description of the Return Value
		 */
		protected abstract boolean internalIsFile();

		public final boolean isFile() {
         try {
            method = "isFile";
            runIt(); // calls runIt()
            return result;
         } catch(IOException ioe) {
            return false;
         }
		}

		/**
		 * returns true if the file exists on the server
		 * 
		 * @return Description of the Return Value
		 */
		protected abstract boolean internalIsServerFile();

		public final boolean isServerFile() {
         try {
            method = "isServerFile";
            runIt(); // calls runIt()
            return result;
         } catch(IOException e) {
            return false;  
         }
		}

		/**
		 * returns the file name URI or null
		 * 
		 * @return Description of the Return Value
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		protected String internalGetSourceName() throws java.io.IOException {
			throw new UnsupportedOperationException(
					"Not implemented! Param does not represent a file...");
		}

		public final String getSourceName() throws java.io.IOException {
			method = "getSourceName";
			runIt(); // calls runIt()
			return (String) value;
		}

		/**
		 * gets the copy of the ParamInfo
		 * 
		 * @return The parameterInfo
		 */
		public final ParameterInfo getParameterInfo() {
			return paraminfo;
		}

		/**
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public final void runIt() throws java.io.IOException {
			if (method.equals("getValue")) {
				value = internalGetValue();
			} else if (method.equals("isFile")) {
				result = internalIsFile();
			} else if (method.equals("isServerFile")) {
				result = internalIsServerFile();
			} else if (method.equals("getSourceName")) {
				value = internalGetSourceName();
			}
		}
	}

	/**
	 * Abstact implemtation for those non file ParamRetrievor implementations
	 * 
	 * @author jgould
	 * @created February 9, 2004
	 */
	private abstract static class NoFileParamRetrievor implements ParamRetrievor {
		private String value;

		private final ParameterInfo paraminfo;

		protected NoFileParamRetrievor(ParameterInfo info) {
			paraminfo = info;
		}

		/**
		 * returns the value
		 * 
		 * @return Description of the Return Value
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public abstract String internalGetValue() throws java.io.IOException;

		/**
		 * returns the value in an Exception-safe way
		 * 
		 * @return The value
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public final String getValue() throws java.io.IOException {
			runIt();
			return value;
		}

		/**
		 * returns true if the value should be in a file
		 * 
		 * @return The file
		 */
		public final boolean isFile() {
			return false;
		}

		/**
		 * returns the file name URI or null
		 * 
		 * @return The sourceName
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public final String getSourceName() throws java.io.IOException {
			throw new UnsupportedOperationException(
					"Not implemented! Param does not represent a file...");
		}

		/**
		 * @exception java.io.IOException
		 *                Description of the Exception
		 */
		public void runIt() throws java.io.IOException {
			value = internalGetValue();
		}

		/**
		 * gets the cloned ParameterInfo
		 * 
		 * @return The parameterInfo
		 */
		public final ParameterInfo getParameterInfo() {
			return paraminfo;
		}
	}

	/**
	 * Helper class. Items can be displayed as one thing and return the value of
	 * another Useful for JComboBox item
	 * 
	 * @author jgould
	 * @created February 9, 2004
	 */
	private final class ChoiceItem {
		// fields
		/** the text that represents this */
		private final String text;

		/** the object that this is a wrapper for */
		private final Object value;

		ChoiceItem(final String text, final Object value) {
			this.text = text.trim();
			if (value instanceof String) {
				this.value = ((String) value).trim();
			} else {
				this.value = value;
			}
		}

		/**
		 * returns the value (which is not displayed)
		 * 
		 * @return The value
		 */
		public final Object getValue() {
			return value;
		}

		/**
		 * overrides super... returns the supplied text
		 * 
		 * @return Description of the Return Value
		 */
		public final String toString() {
			return text;
		}

		/**
		 * returns true if the <CODE>ChoiceItem</CODE>'s fields equal either
		 * the value or the text
		 * 
		 * @param item
		 *            Description of the Parameter
		 * @return Description of the Return Value
		 */
		protected boolean hasToken(final ChoiceItem item) {
			return (item != null && (text.equalsIgnoreCase(item.text) || item.value
					.toString().equalsIgnoreCase(value.toString())));
		}
	}

}