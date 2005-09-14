/*
 * Created on Jun 27, 2005
 *
 */
package org.genepattern.gpge.ui.tasks;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.PreferenceChangeMessage;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.project.ProjectDirModel;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Displays an array of <tt>ParameterInfo</tt> objects
 * 
 * @author Joshua Gould
 *  
 */
public class ParameterInfoPanel extends JPanel {

	private Map parameterName2ComponentMap;

	private List inputFileParameters;

	private List inputFileTypes;

	
	private List parameterDescriptions;

	private FormLayout formLayout;

	private int maxLabelWidth = 0;

	private boolean viewOnly;

	private DescriptionListener descriptionListener;

	
	final static int PARAMETER_LABEL_COLUMN = 1;

	final static int PARAMETER_INPUT_FIELD_COLUMN = 3;

	private static final int ROWS_PER_PARAMETER = 4;

	final static int PARAMETER_ROW_OFFSET = 0;

	final static int USE_OUTPUT_ROW_OFFSET = 1;

	final static int DESCRIPTION_ROW_OFFSET = 2;

	final static int SPACE_ROW_OFFSET = 3;

	
	public void setLabelWidth(int labelWidth) {
		formLayout.setColumnSpec(PARAMETER_LABEL_COLUMN, new ColumnSpec(
				labelWidth + "px"));
		formLayout.invalidateLayout(this);
		formLayout.layoutContainer(this);
	}

	public int getLabelWidth() {
		return maxLabelWidth;
	}

	/**
	 * Gets the component that is used to display the given parameter
	 * 
	 * @param parameterName
	 *            the parameter name
	 * @return the component
	 */
	public Component getComponent(String parameterName) {
		return (Component) parameterName2ComponentMap.get(parameterName);
	}

	
	public Iterator getInputFileParameters() {
		return inputFileParameters.iterator();
	}
	
	public Iterator getInputFileTypes() {
		return inputFileTypes.iterator();
	}

	private final static String getValue(ParameterInfo info,
			ObjectTextField field) throws java.io.IOException {
		String text = field.getText().trim();
		if(new File(text).exists()) {
			info.setAsInputFile();
			return new File(text).getCanonicalPath();
		} else if(text.startsWith("job #")) { // e.g. job #21, out.txt
			info.getAttributes().put(ParameterInfo.TYPE,
					ParameterInfo.FILE_TYPE);
			info.getAttributes().put(ParameterInfo.MODE,
					ParameterInfo.CACHED_INPUT_MODE);
			String jobNumber = text.substring(text.indexOf("#")+1, text.indexOf(",")).trim();
			String fileName = text.substring(text.indexOf(",")+1, text.length()).trim();
			return jobNumber + "/" + fileName;
		} else if(text.equals("")){
			return null;
		} else {
			return text;
		}
	}

	/**
	 * Gets the parameter info array for the current values of parameters
	 */
	public ParameterInfo[] getParameterInfoArray() {
		List actualParameters = new ArrayList();

		if (parameterName2ComponentMap.size() > 0) {
			for (Iterator it = parameterName2ComponentMap.keySet().iterator(); it
					.hasNext();) {
				String parameterName = (String) it.next();
				Component c = this.getComponent(parameterName);
				String value = null;
				ParameterInfo actualParameter = new ParameterInfo(
						parameterName, "", "");
				actualParameter.setAttributes(new HashMap(2));

				if (c instanceof ObjectTextField) {
					try {
						value = getValue(actualParameter, (ObjectTextField) c);
						actualParameter.getAttributes().put(
								GPConstants.PARAM_INFO_CLIENT_FILENAME[0],
								value);
					} catch (java.io.IOException ioe) {
						ioe.printStackTrace();
					}
				} else if (c instanceof JComboBox) {
					ParameterChoice ci = (ParameterChoice) ((JComboBox) c)
							.getSelectedItem();
					value = ci.getValue();
				} else if (c instanceof JTextField) {
					value = ((JTextField) c).getText();
					if (value != null) {
						value = value.trim();
					}
					if ("".equals(value)) {
						value = null;
					}
				}

				actualParameter.setValue(value);
				actualParameters.add(actualParameter);
			}
		}
		final ParameterInfo[] actualParameterArray = (ParameterInfo[]) actualParameters
				.toArray(new ParameterInfo[0]);
		return actualParameterArray;
	}

	private int getRowIndex(int i) {
		return i * ROWS_PER_PARAMETER + 1;
	}

	/**
	 * Sets the vertical spacing between the description and the next input field
	 * @param size
	 */
	public void setVerticalSpacing(int size) {
		if (parameterName2ComponentMap.size() > 0) {
			for (int i = 0; i < parameterName2ComponentMap.size(); i++) {
				int row = getRowIndex(i) + SPACE_ROW_OFFSET;
				formLayout.setRowSpec(row, new RowSpec(size + "px"));
			}
			formLayout.invalidateLayout(this);
			formLayout.layoutContainer(this);
		}
	}

	public void removeNotify() {
		super.removeNotify();
		MessageManager.removeGPGEMessageListener(descriptionListener);
	}

	public ParameterInfoPanel(String taskName, ParameterInfo[] params) {
		this(taskName, params, false);
	}

	public ParameterInfoPanel(String taskName, ParameterInfo[] params,
			boolean viewOnly) {
		this.viewOnly = viewOnly;
		descriptionListener = new DescriptionListener();
		MessageManager.addGPGEMessageListener(descriptionListener);
		boolean showDescriptions = PropertyManager
				.getBooleanProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS);

		int numParams = params != null ? params.length : 0;
		parameterName2ComponentMap = new HashMap(numParams);
		parameterDescriptions = new ArrayList(numParams);
		inputFileParameters = new ArrayList();
		inputFileTypes = new ArrayList();
		
		this.setBackground(Color.white);
		this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		if (params == null || params.length == 0) {
			this.add(new JLabel(taskName + " has no input parameters"));
		} else {
			StringBuffer rowSpec = new StringBuffer();
			for (int i = 0; i < params.length; i++) {
				if (i > 0) {
					rowSpec.append(", ");
				}
				rowSpec.append("pref, pref, pref, 12px");
				// input row
				// use output from row
				// description row
				// space
			}
			// input label, space, input field + browse button
			//                     description

			formLayout = new FormLayout( // 
					"right:pref:none, 6px, left:default:none", rowSpec
							.toString());

			this.setLayout(formLayout);

			CellConstraints cc = new CellConstraints();

			for (int i = 0; i < params.length; i++) {
				cc.gridWidth = 1;
				final ParameterInfo info = params[i];

				Component input = createComponent(info);

				int row = getRowIndex(i);
				Component inputLabel = new JLabel(AnalysisServiceDisplay
						.getDisplayString(info)
						+ ":");
				maxLabelWidth = Math.max(maxLabelWidth, inputLabel.getWidth());
				if (!isOptional(params[i])) {
					inputLabel.setFont(inputLabel.getFont().deriveFont(
							java.awt.Font.BOLD));
				}

				this.add(inputLabel, cc.xy(PARAMETER_LABEL_COLUMN, row));
				JTextArea description = GUIUtil.createWrappedLabel(info.getDescription());
				description.setColumns(50);
				//JLabel description = new JLabel(info.getDescription());

				if (!viewOnly && info.isInputFile()) {
					JPanel p = new JPanel();
					p.setBackground(getBackground());
					FormLayout f = new FormLayout(
							"left:pref:none, left:pref:none, left:default:none",
							"pref");
					p.setLayout(f);

					JButton btn = new JButton("Browse...");
					btn.setBackground(getBackground());
					btn.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							File f = GUIUtil.showOpenDialog();
							if (f != null) {
								setValue(info.getName(), f);
							}
						}
					});

					p.add(input, cc.xy(1, 1));
					p.add(btn, cc.xy(2, 1));
					this.add(p, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row
							+ PARAMETER_ROW_OFFSET));
				} else {
					this.add(input, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row
							+ PARAMETER_ROW_OFFSET));
				}
				cc.hAlign = CellConstraints.FILL;
				description.setVisible(showDescriptions);
				parameterDescriptions.add(description);
				this.add(description, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row
						+ DESCRIPTION_ROW_OFFSET));
			}
		}
	}

	private void setDescriptionsVisible(boolean b) {
		for (int i = 0; i < parameterDescriptions.size(); i++) {
			Component c = (Component) parameterDescriptions.get(i);
			c.setVisible(b);
		}
	}

	/**
	 * Sets the value of the given parameter
	 * @param parameterName the unencoded parameter name as returned by ParameterInfo.getName()
	 * @param parameterValue the parameter value. If the parameter contains a choice list, the value can be
	 *            either the UI value of the command line value
	 */
	public void setValue(String parameterName, Object value) {
		if (value == null) {
			value = "";
		}
		Component c = getComponent(parameterName);
		if (c != null) {
			if (c instanceof ObjectTextField) {
				ObjectTextField tf = (ObjectTextField) c;
				tf.setObject(value);
			} else if (c instanceof JTextField) {
				((JTextField) c).setText(value.toString());
			} else if (c instanceof JComboBox) {
				JComboBox cb = (JComboBox) c;
				String stringValue = value.toString();
				for (int i = 0, size = cb.getItemCount(); i < size; i++) {
					ParameterChoice ci = (ParameterChoice) cb.getItemAt(i);
					if (ci.equalsCmdLineOrUIValue(stringValue)) {
						cb.setSelectedIndex(i);
						break;
					}
				}
			} else if (c instanceof JLabel) {

				((JLabel) c).setText(value.toString());
			}
		}
	}

	protected final Component createComponent(final ParameterInfo info) {
		String value = info.getValue();
		Component field = null;
		String name = AnalysisServiceDisplay.getDisplayString(info);
		if(viewOnly) {
			field = new JLabel(value);
		} else {
			if (info.isInputFile()) {
				inputFileParameters.add(name);
				String fileFormatsString = (String) info.getAttributes().get(
						GPConstants.FILE_FORMAT);
				if(fileFormatsString!=null) {
					inputFileTypes.add(fileFormatsString
						.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER));
				} else {
					inputFileTypes.add(new String[0]);
				}
				field = createInputFileField(info);
			} else if(value.split(";").length > 1) {
				field = createComboBox(info);
			} else {
				field = createTextInput(info);
			}
		}
		parameterName2ComponentMap.put(info.getName(), field);
		return field;
	}

	/**
	 * creates a JTextField
	 * 
	 * @param info
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private Component createTextInput(ParameterInfo info) {
		final JTextField field = new JTextField(20);
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
			ex.printStackTrace();
		}
		field.setFont(getFont());
		return field;
	}


	private Component createInputFileField(ParameterInfo info) {
		final ObjectTextField field = new ObjectTextField(null, 20);
		if (!GPGE.RUNNING_ON_MAC) {
			field.setBackground(Color.white);
		}
		field.setFont(getFont());

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
					System.err.println("Error setting default value.");
				}
			} else {// for reload of server file
				field.setObject(defaultValue);
			}

		}
		return field;
	}

	/**
	 * Parses the String and returns a ParameterChoice
	 * 
	 * @param string
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private static ParameterChoice createChoiceItem(final String string) {
		final int index = string.indexOf('=');
		ParameterChoice choice = null;
		if (index < 0) {
			choice = new ParameterChoice(string, string);
		} else {
			choice = new ParameterChoice(string.substring(index + 1), string
					.substring(0, index));
		}
		return choice;
	}

	/**
	 * creates the default <CODE>ChoiceItem</CODE> for the combo box
	 * 
	 * @param default_val
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	private static ParameterChoice createDefaultChoice(final String default_val) {
		if (default_val != null && default_val.length() > 0) {
			return createChoiceItem(default_val);
		}
		return null;
	}

	protected final boolean isOptional(final ParameterInfo info) {
		final Object optional = info.getAttributes().get("optional");
		return (optional != null && "on".equalsIgnoreCase(optional.toString()));
	}

	private final class DescriptionListener implements GPGEMessageListener {
		public void receiveMessage(GPGEMessage message) {
			if (message instanceof PreferenceChangeMessage) {
				PreferenceChangeMessage pcm = (PreferenceChangeMessage) message;
				if (pcm.getType() == PreferenceChangeMessage.SHOW_PARAMETER_DESCRIPTIONS) {
					setDescriptionsVisible(PropertyManager
							.getBooleanProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS));

				}
			}
		}
	}
	
	/**
	 * Creates a combo box for parameter that have a choice list
	 * @param info the parameter info
	 * @return a combo box containing ParameterChoice objects
	 */
	private static JComboBox createComboBox(ParameterInfo info) {
		// get default
		Map attrs = info.getAttributes();
		String default_val = null;
		if(attrs!=null) {
			default_val = (String) attrs.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
		}
		final StringTokenizer tokenizer = new StringTokenizer(info.getValue(),
				";");
		final JComboBox list = new JComboBox();
		list.setBackground(Color.white);
		for (int i = 0; tokenizer.hasMoreTokens(); i++) {
			final String token = tokenizer.nextToken();
			final ParameterChoice item = createChoiceItem(token);
			list.addItem(item);
			if(item.equalsCmdLineOrUIValue(default_val)) {
				list.setSelectedIndex(i);
			}
		}
		return list;
	}

	

}
