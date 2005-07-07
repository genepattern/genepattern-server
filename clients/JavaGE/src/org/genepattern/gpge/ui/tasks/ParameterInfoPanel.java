/*
 * Created on Jun 27, 2005
 *
 */
package org.genepattern.gpge.ui.tasks;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
import javax.swing.JTextField;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.ui.graphics.draggable.ObjectTextField;
import org.genepattern.gpge.ui.maindisplay.MainFrame;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.project.ProjectDirModel;
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

    private List parameterDescriptions;

	private FormLayout formLayout;
	private int maxLabelWidth = 0;
	final static int PARAMETER_LABEL_COLUMN = 1;
	final static int PARAMETER_INPUT_FIELD_COLUMN = 3;
	
	private static final int ROWS_PER_PARAMETER = 4;
	
	final static int PARAMETER_ROW_OFFSET = 0;
	final static int USE_OUTPUT_ROW_OFFSET = 1;
	final static int DESCRIPTION_ROW_OFFSET = 2;
	final static int SPACE_ROW_OFFSET = 3;
	
	public void setLabelWidth(int labelWidth) {
		formLayout.setColumnSpec(PARAMETER_LABEL_COLUMN, new ColumnSpec(labelWidth + "px"));
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

    /**
     * Gets an iterator of the input file parameter names
     * 
     * @return the input file parameter names
     */
    public Iterator getInputFileParameters() {
        return inputFileParameters.iterator();
    }

    private final static String getValue(ParameterInfo info,
            ObjectTextField field) throws java.io.IOException {
        final Object obj = field.getObject();
        if (obj == null) {
            return null;
        }

        if (obj instanceof String) {// reloaded job where input file is
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
            final File drop_file = (File) obj;
            return drop_file.getCanonicalPath();
        } else if (obj instanceof java.net.URL) {
            return obj.toString();
        } else {
            throw new IllegalStateException();
        }
    }

   
    /**
     * Gets the parameter info array for the current values of parameters
     */
    public ParameterInfo[] getParameterInfoArray() {
        List actualParameters = new ArrayList();
        
        if (parameterName2ComponentMap.size() > 0) {
            for (Iterator it = parameterName2ComponentMap.keySet().iterator(); it.hasNext(); ) {
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
                    ParameterInfoPanel.ChoiceItem ci = (ParameterInfoPanel.ChoiceItem) ((JComboBox) c)
                            .getSelectedItem();
                    value = ci.getValue();
                } else if (c instanceof JTextField) {
                    value = ((JTextField) c).getText();
                }
                if (value != null) {
                    value = value.trim();
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
    		return  i * ROWS_PER_PARAMETER + 1;
    }

    /**
     * Sets the vertical spacing between the description and the next input field
     * @param size
     */
    public void setVerticalSpacing(int size) {
    	 if (parameterName2ComponentMap.size() > 0) {
    		 for(int i = 0; i < parameterName2ComponentMap.size(); i++) {
    			 int row = getRowIndex(i) + SPACE_ROW_OFFSET;
    			 formLayout.setRowSpec(row, new RowSpec(size + "px"));
    		 }
    		 formLayout.invalidateLayout(this);
    		 formLayout.layoutContainer(this);
    	 }
    }
    
    /**
     * 
     * @param parameterIndex the index of the parameter, starting at 0
     * @param previousTaskNames, the array of task names to display
     * @param choices the array of output type choices to display
     */
	public void setUseInputFromPreviousTask(int parameterIndex, String[] previousTaskNames, String[] choices) {
		int row = getRowIndex(parameterIndex) + USE_OUTPUT_ROW_OFFSET;
		JLabel tasksLabel = new JLabel("or use output from");
		JComboBox tasksComboBox = new JComboBox(previousTaskNames);
		tasksComboBox.setBackground(getBackground());
		
		JLabel outputFilesLabel = new JLabel("output file");
		JComboBox outputFilesComboBox = new JComboBox(choices);
		outputFilesComboBox.setBackground(getBackground());
		
		JPanel p = new JPanel();
		p.setBackground(getBackground());
		p.add(tasksLabel);
		p.add(tasksComboBox);
		p.add(outputFilesLabel);
		p.add(outputFilesComboBox);
		CellConstraints cc = new CellConstraints();
		this.add(p, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row));
	}
    
     public ParameterInfoPanel(String taskName, ParameterInfo[] params) {
        int numParams = params!=null?params.length:0;
        parameterName2ComponentMap = new HashMap(numParams);
        parameterDescriptions = new ArrayList(numParams);
        inputFileParameters = new ArrayList();

        this.setBackground(Color.white);
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        boolean showDescriptions = Boolean
                .valueOf(
                        PropertyManager
                                .getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS))
                .booleanValue();

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
                final String value = info.getValue();
                Component input = null;
                if (value == null || value.equals("")) {
                    if (info.isInputFile()) {
                        input = createInputFileField(info);
                    } else {
                        input = createTextInput(info);
                    }
                } else {
                    input = createComboBox(info);
                }
                int row = getRowIndex(i);
                Component inputLabel = new JLabel(AnalysisServiceDisplay
                        .getDisplayString(info) + ":");
                maxLabelWidth = Math.max(maxLabelWidth, inputLabel.getWidth());
                if (!isOptional(params[i])) {
                    inputLabel.setFont(inputLabel.getFont().deriveFont(
                            java.awt.Font.BOLD));
                }
                
                this.add(inputLabel, cc.xy(PARAMETER_LABEL_COLUMN, row));
                JLabel description = new JLabel(info.getDescription());
               
                if (info.isInputFile()) {
                	JPanel p = new JPanel();
              
                	p.setBackground(getBackground());
                	FormLayout f = new FormLayout("left:pref:none, left:pref:none, left:default:none","pref");
                	p.setLayout(f);

                	JButton btn = new JButton("Browse...");
                	btn.addActionListener(new ActionListener() {
                		public void actionPerformed(ActionEvent e) {
                			FileDialog fc = new FileDialog(GenePattern.getDialogParent());
                			fc.setModal(true);
                			fc.show();
                			String f = fc.getFile();
                			if(f!=null) {
                				setValue(info.getName(), new File(f));
                			}
                		}
                	});
                	btn.setBackground(getBackground());
               // 	Dimension d = btn.getPreferredSize();
                //	d.height = size.height - 2;
               // 	btn.setSize(d);
                	p.add(input, cc.xy(1, 1));
                	p.add(btn, cc.xy(2, 1));
                	this.add(p, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row + PARAMETER_ROW_OFFSET));
                } else {
                	 this.add(input, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row + PARAMETER_ROW_OFFSET));
                }
                cc.hAlign = CellConstraints.FILL;
                description.setVisible(showDescriptions);
                parameterDescriptions.add(description);
                this.add(description, cc.xy(PARAMETER_INPUT_FIELD_COLUMN, row + DESCRIPTION_ROW_OFFSET));
                parameterName2ComponentMap.put(info.getName(), input);
            }
        }
    }

    public void setDescriptionsVisible(boolean b) {
        for (int i = 0; i < parameterDescriptions.size(); i++) {
            Component c = (Component) parameterDescriptions.get(i);
            c.setVisible(b);
        }
    }

    public void setValue(String parameterName, Object value) {
    		Component c = getComponent(parameterName);
    	    if(c!=null) {
    	    		if(c instanceof ObjectTextField) {
	    			ObjectTextField tf = (ObjectTextField)  c;
    	    			tf.setObject(value);
	    		} else if(c instanceof JTextField) {
    	    			((JTextField)c).setText(value.toString());
    	    		} else if(c instanceof JComboBox) {
    	    			JComboBox cb = (JComboBox) c;
    	    			cb.setSelectedItem(new ChoiceItem(value.toString(), value.toString()));
    	    		} 
    	    }
    }
    
    protected final JTextField createProperTextField(final ParameterInfo info) {
        final int num_cols = 20;
        JTextField field = null;
        final Object value = info.getAttributes().get("type");
        if (value == null || value.equals("java.lang.String")) {
            field = new JTextField(num_cols);
        } else if (value.equals("java.lang.Integer")) {
            field = new JTextField(num_cols);
        } else if (value.equals("java.lang.Float")) {
            field = new JTextField(num_cols);
        } else {
            field = new JTextField(num_cols);
            System.err.println("Unknown type");
        }
        return field;
    }

    private Component createComboBox(ParameterInfo info) {
        // get default
        final String default_val = ((String) info.getAttributes().get(
                GPConstants.PARAM_INFO_DEFAULT_VALUE[0])).trim();
        final ChoiceItem default_item = createDefaultChoice(default_val);
        final StringTokenizer tokenizer = new StringTokenizer(info.getValue(),
                ";");
        final JComboBox list = new JComboBox();
        list.setBackground(Color.white);
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
        } else {
            list.setSelectedIndex(0);
        }
        return list;
    }

    private ObjectTextField createObjectTextField() {
        final ObjectTextField field = new ObjectTextField(null, 20);
        if (!MainFrame.RUNNING_ON_MAC) {
            field.setBackground(Color.white);
        }
        field.setFont(getFont());
        return field;
    }

    private Component createInputFileField(ParameterInfo info) {
        ObjectTextField field = createObjectTextField();
        inputFileParameters.add(info);
        parameterName2ComponentMap.put(info.getName(), field);
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
        field.setFont(getFont());
        return field;
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
     * @param info
     *            Description of the Parameter
     * @return Description of the Return Value
     */
    private Component createTextInput(ParameterInfo info) {

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
            ex.printStackTrace();
        }
        field.setFont(getFont());
        return field;
    }

    protected final boolean isOptional(final ParameterInfo info) {
        final Object optional = info.getAttributes().get("optional");
        return (optional != null && "on".equalsIgnoreCase(optional.toString()));
    }

    static class ChoiceItem {
        
        /** the text that is displayed to the users */
        private final String text;

        /** the command line value */
        private final String value;

        ChoiceItem(final String text, final String value) {
            this.text = text.trim();
            this.value = value;

        }

      
        public final String toString() {
            return text;
        }

        public boolean equals(Object obj) {
        		if(obj instanceof ChoiceItem) {
        			ChoiceItem other = (ChoiceItem) obj;
        			return other.text.equals(this.text);
        		}
        		return false;
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

        /**
         * returns the value (which is not displayed)
         * 
         * @return The value
         */
        public final String getValue() {
            return value;
        }
    }

}
