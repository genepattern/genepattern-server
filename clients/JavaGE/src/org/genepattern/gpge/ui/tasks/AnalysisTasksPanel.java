package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Category;
import org.genepattern.analysis.JobInfo;
import org.genepattern.analysis.OmnigeneException;
import org.genepattern.analysis.ParameterFormatConverter;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.client.AnalysisJob;
import org.genepattern.client.AnalysisService;
import org.genepattern.client.RequestHandler;
import org.genepattern.client.RequestHandlerFactory;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.DataObjectBrowser;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
/**
 *  <p>
 *
 *  Description: creates UI dynamically for different kind of analysis web
 *  services tasks</p>
 *
 *@author     Hui Gong
 *@created    April 9, 2004
 *@version    $Revision$
 */

public class AnalysisTasksPanel extends JPanel implements Observer {
	//private JScrollPane _servicePanel;
	/**  the analysis task panel */
	private JPanel _servicePanel;
	private DataModel _dataModel;
	/**  contains the mappings of parameter names to ParamRetrievor instances */
	private final Map name_retriever = new HashMap();
	/**  the list of RendererFactory objects */
	private final java.util.List renderer_factories = new ArrayList();
	/**  TaskSubmitter instances in increasing order of presidence */
	private final TaskSubmitter[] submitters;
	/**  the default submitter */
	private final TaskSubmitter default_submitter = new DefaultTaskSubmitter();
	private int _id;
	private AnalysisService _selectedService;
	/**  the error handler if one was supplied or null */
	private final OVExceptionHandler exception_handler;

	private static Category cat = Category.getInstance(AnalysisTasksPanel.class.getName());
	String username = null;
	String password = null;
	JLabel tasksLabel;
	public static boolean RUNNING_ON_MAC = System.getProperty("mrj.version") != null && javax.swing.UIManager.getSystemLookAndFeelClassName().equals(javax.swing.UIManager.getLookAndFeel().getClass().getName());
	Map lsid2VersionsMap;
	DataObjectBrowser dataObjectBrowser;
	JPanel versionPanel = new JPanel();
   /** the latest version for the current task */
   String latestVersion;
    
	/**
	 *  Constructs a new AnalysisTasksPanel with a wrapper for a type of list
	 *  instead of a JComboBox for the analysis tasks
	 *
	 *@param  model              Description of the Parameter
	 *@param  listtype           Description of the Parameter
	 *@param  menu_listener      Description of the Parameter
	 *@param  submitters         Description of the Parameter
	 *@param  exception_handler  Description of the Parameter
	 *@param  username           Description of the Parameter
	 */
	public AnalysisTasksPanel(final DataModel model, final ListTypeAdapter listtype, final ActionListener menu_listener, final TaskSubmitter[] submitters, final OVExceptionHandler exception_handler, String username) {
		this(model, submitters, exception_handler, username);
		
		final ListTypeAdapter list_type = (listtype != null) ? listtype : new DefaultListTypeAdapter();
		tasksLabel = new JLabel();
		//final java.util.Map service_text = new java.util.HashMap();
		final ActionListener listener =
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(menu_listener != null) {
						menu_listener.actionPerformed(e);
					}
					//final ListTypeAdapter source = (ListTypeAdapter)e.getSource();
					final ListTypeAdapter source = list_type;
					_selectedService = (AnalysisService) source.getItemSelected();
					loadTask(_selectedService);
				}
			};
		final int init_num_items = list_type.getItemCount();
		Vector services = _dataModel.getAnalysisServices();
		final int num_services = services.size();
		for(int i = 0; i < num_services; i++) {
			final AnalysisService an_serv = (AnalysisService) services.get(i);
			list_type.addItem(an_serv, listener); // uses toString() to label item
		}

		if(num_services == 0) {
			_servicePanel = new JPanel(); //new JScrollPane();
		} else {
			_selectedService = (AnalysisService) services.get(0);
			_servicePanel = createTaskPane(_selectedService);
		}
		this.setLayout(new BorderLayout());
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BorderLayout());
		listPanel.add(tasksLabel, BorderLayout.CENTER);
		listPanel.add(versionPanel, BorderLayout.EAST);
		if(list_type.isLocalComponent()) {
			listPanel.add(list_type.getComponent());
		}
		
		if(!RUNNING_ON_MAC) {
			listPanel.setBackground(java.awt.Color.white);
         versionPanel.setBackground(java.awt.Color.white);
		}
		
		this.add(listPanel, BorderLayout.NORTH);
		this.add(_servicePanel, BorderLayout.CENTER);
	}
	
	public void init(DataObjectBrowser browser, String server) {
		try {
			this.dataObjectBrowser = browser;
			this.lsid2VersionsMap = new org.genepattern.analysis.AdminProxy(server, username).getLSIDToVersionsMap();
		} catch(Throwable t){
			t.printStackTrace();
		}	
	}


	public AnalysisTasksPanel(DataModel model, String username) {
		this(model, null, null, null, null, username);
	}


	/**
	 *  helper constructor
	 *
	 *@param  model              Description of the Parameter
	 *@param  submitters         Description of the Parameter
	 *@param  exception_handler  Description of the Parameter
	 *@param  username           Description of the Parameter
	 */
	private AnalysisTasksPanel(final DataModel model, final TaskSubmitter[] submitters, final OVExceptionHandler exception_handler, String username) {
		this.username = username;
		this._dataModel = model;
		this.exception_handler = exception_handler;
		this.submitters = (submitters != null) ? submitters : new TaskSubmitter[0];
		final DefaultUIRenderer render = new DefaultUIRenderer();
		final RendererFactory factory =
			new RendererFactory() {
				/**
				 *  returns an UIRenderer array for rendering the params or null if couldn't
				 *  process any params. After returning the input java.util.List will
				 *  contain any remaining ParameterInfo objects that were not processed.
				 *  Note the params can be run through the next RendererFactory to produce
				 *  more Renderers.
				 *
				 *@param  service  Description of the Parameter
				 *@param  params   Description of the Parameter
				 *@return          Description of the Return Value
				 */
				public UIRenderer createRenderer(final AnalysisService service, java.util.List params) {
					return render;
				}
			};
		addRendererFactory(factory);
	}

   

   
	public void loadTask(AnalysisService selectedService) {
      latestVersion = null;
      TaskInfo taskInfo = selectedService.getTaskInfo();
      String taskDisplay = taskInfo.getName();
     
      try {
        LSID lsid = new LSID((String) selectedService.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID));
        if(!org.genepattern.gpge.ui.maindisplay.LSIDUtil.isBroadTask(lsid)) {
           String authority = lsid.getAuthority();
           taskDisplay += " (" + authority + ")";
        }
        final String lsidNoVersion = lsid.toStringNoVersion();
        List versions = (List) lsid2VersionsMap.get(lsidNoVersion);
        String currentVersion = lsid.getVersion();
        latestVersion = currentVersion;
        for(int i = 0; i < versions.size(); i++) {
           String version = (String) versions.get(i);
           if(version.compareTo(latestVersion) > 0) {
              latestVersion = version;
           }
        }
        
        if(lsid.getVersion().equals(latestVersion)) {
           taskDisplay += ", " + lsid.getVersion() + " (latest)";
        } else {
           taskDisplay += ", " + lsid.getVersion();
        }
        
      } catch(Exception e){}
           
      
      
		tasksLabel.setText(taskDisplay);
		this._selectedService = selectedService;
		this.remove(_servicePanel);
		this._servicePanel = createTaskPane(selectedService);
		this.add(this._servicePanel, BorderLayout.CENTER);
		this.revalidate();
		this.doLayout();
	}
	
		

	/**
	 *  add additional RendererFactory objects last ones added are the first ones
	 *  tried
	 *
	 *@param  factory  The feature to be added to the RendererFactory attribute
	 */
	public void addRendererFactory(final RendererFactory factory) {
		this.renderer_factories.add(factory);
	}

	

	/**
	 *  adds a new
	 *
	 *@param  service  Description of the Parameter
	 *@return          Description of the Return Value
	 */
	//private JScrollPane createTaskPane(final AnalysisService service){
	private JPanel createTaskPane(final AnalysisService service) {
		versionPanel.removeAll();
		String lsidString = (String) service.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID);
		if(lsidString!=null) {
			try {
				final LSID lsid = new LSID(lsidString);
				final String lsidNoVersion = lsid.toStringNoVersion();
				List versions = (List) lsid2VersionsMap.get(lsidNoVersion);
				Vector versionsCopy = new Vector();
				for(int i = 0; i < versions.size(); i++) {
               String version = (String) versions.get(i);
      
               if(version.equals(latestVersion)) {
                  version += " (latest)";
               }
               versionsCopy.add(version);
            }
           Collections.sort(versionsCopy, String.CASE_INSENSITIVE_ORDER);
				
				if(versionsCopy.size()>1) {
					versionPanel.add(new JLabel("Version:"));
					final JComboBox cb = new JComboBox(versionsCopy);
					if(!RUNNING_ON_MAC) {
						cb.setBackground(java.awt.Color.white);
					}
					if(lsid.getVersion().equals(latestVersion)) {
						cb.setSelectedItem(lsid.getVersion() + " (latest)");
					} else {
						cb.setSelectedItem(lsid.getVersion());
					}
					
					cb.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							String selectedItem = (String) cb.getSelectedItem();
							if(selectedItem.equals("")) {
								return;	
							}
                    int index = selectedItem.indexOf(" (latest");
                
                    if(index > 0) {
                       selectedItem = selectedItem.substring(0, index);
                    }
						  if(selectedItem.equals(lsid.getVersion())) {
							  return;
						  }
							String selectedLSID = lsidNoVersion + ":" + selectedItem;
							AnalysisService svc = dataObjectBrowser.getAnalysisService(selectedLSID);
							if(svc==null) {
								JOptionPane.showMessageDialog(GenePattern.getDialogParent(), "The task was not found.");
							} else {
								loadTask(svc);
							}
						}
					});
					versionPanel.add(cb);
				}
			} catch(Exception e){}
		}
		
		this.name_retriever.clear();
		// contains the top_pane, JScrollPane(pane), bottom_pane
		final JPanel main_pane = new JPanel(new BorderLayout());

		final JPanel pane = new JPanel(new GridBagLayout()); // middle

		if(!RUNNING_ON_MAC) {
			pane.setBackground(java.awt.Color.white);
		}

		main_pane.add(new JScrollPane(pane), BorderLayout.CENTER);
		final JPanel top_pane = new JPanel(); // FlowLayout
		
		if(!RUNNING_ON_MAC) {
			top_pane.setBackground(java.awt.Color.white);
		}
		
		((FlowLayout) top_pane.getLayout()).setAlignment(FlowLayout.LEADING);
		main_pane.add(top_pane, BorderLayout.NORTH);
		final JPanel bottom_pane = new JPanel(new BorderLayout());
		main_pane.add(bottom_pane, BorderLayout.SOUTH);

		//final JScrollPane scrollPane = new JScrollPane(main_pane);
		//final JScrollPane scrollPane = new JScrollPane(pane);
		final TaskInfo task = service.getTaskInfo();
		// last renderer will get the chance to create the task panel header label
		// and create the submit button
		UIRenderer last_renderer = null;

		if(task != null && task.getParameterInfoArray() != null && task.getParameterInfoArray().length > 0) {
			_id = task.getID();

			//get parameters
			//final ParameterInfo[] params = task.getParameterInfoArray();
			final ParameterInfo[] original_params = task.getParameterInfoArray();
			final int num_params = original_params.length;
			//final int num_params = params.length;
			if(num_params == 0) {
				bottom_pane.add(getErrorLabel("Error: no parameters!"));
				//return scrollPane;
				return main_pane;
			}
			// clone the param infos since they are mutable objects
			final ParameterInfo[] params = new ParameterInfo[num_params];
			for(int i = 0; i < num_params; i++) {
				final ParameterInfo old = original_params[i];
				final ParameterInfo neu = new ParameterInfo(old.getName(), old.getValue(), old.getDescription());
				final HashMap attrs = old.getAttributes();
				if(attrs != null) {
					neu.setAttributes((HashMap) attrs.clone());
				}
				params[i] = neu;
			}
			// end cloning of ParameterInfo objs

			//loop through the parameters to create UI
			final java.util.List param_list = new ArrayList(java.util.Arrays.asList(params));
			// get the last added RendererFactory objects first
			final int num_factories = renderer_factories.size();

			for(int i = num_factories - 1; i >= 0 && param_list.size() > 0; i--) { // rev. loop
				final RendererFactory factory = (RendererFactory) renderer_factories.get(i);
				final UIRenderer renderer = factory.createRenderer(service, param_list);
				if(renderer != null) {
					last_renderer = renderer;
					renderer.render(pane, service, param_list, name_retriever);
				}
			}
			final int num_left_over = param_list.size();
			if(num_left_over > 0) {
				if(num_left_over == num_params) {
					bottom_pane.add(getErrorLabel("Error: none of the " + num_params + " were processed and displayed!"));
					return main_pane;
				} else {
					bottom_pane.add(getWarningLabel("Warning: " + num_left_over
							 + " parameters could not be processed and are missing from the display!"),
							BorderLayout.SOUTH);
				}

			}

			//add bottom panel for submit button
			if(last_renderer != null) {
				top_pane.add(last_renderer.createTaskLabel(service));
				//((FlowLayout)bottom_pane.getLayout()).setAlignment(FlowLayout.LEADING);
				bottom_pane.add(last_renderer.createSubmitPanel(service, new SubmitActionListener()),
						BorderLayout.CENTER);
			}
		} else {
			bottom_pane.add(getWarningLabel("Note: This module has no input parameters."),
					BorderLayout.SOUTH);
			//add bottom panel for submit button
			//((FlowLayout)bottom_pane.getLayout()).setAlignment(FlowLayout.LEADING);
			bottom_pane.add(createSubmitPanel(), BorderLayout.CENTER);
		}
		

		//return scrollPane;
		return main_pane;
	}
	//private JPanel createLabelPanel(final String desc, final GridBagConstraints gbc, final int gridy){

	private static JComponent createLabelPanel(final String desc) {
		JLabel nameLabel = new JLabel(desc + ":  ");
		return nameLabel;
		//JPanel labelPane = new JPanel();
		//labelPane.add(nameLabel);
		//setGridBagConstraints(gbc, 0, gridy, 1, 1, 100, 1);
		//return labelPane;
	}
	//private JPanel createSubmitPanel(final GridBagConstraints gbc, final int gridy){

	private JComponent createSubmitPanel() {
		JButton submit = new JButton("Submit");
		submit.addActionListener(new SubmitActionListener());
		return submit;
	}


	protected static void setGridBagConstraints(final GridBagConstraints gbc, final int gridx, final int gridy, final int gridw, final int gridh, final int weightx, final int weighty) {
		gbc.gridx = gridx;
		gbc.gridy = gridy;
		gbc.gridwidth = gridw;
		gbc.gridheight = gridh;
		gbc.weightx = weightx;
		gbc.weighty = weighty;
	}


	/**
	 *  adds the error message to the container
	 *
	 *@param  message  Description of the Parameter
	 *@return          The errorLabel
	 */
	//private void addErrorMessage(final String message, final JComponent pane) {
	private JComponent getErrorLabel(final String message) {
		final JLabel label = new JLabel(message);
		label.setForeground(Color.red);
		return label;
	}


	/**
	 *  adds the error message to the container
	 *
	 *@param  message  Description of the Parameter
	 *@return          The warningLabel
	 */
	//private void addErrorMessage(final String message, final JComponent pane) {
	private JComponent getWarningLabel(final String message) {
		final JLabel label = new JLabel(message);
		label.setForeground(Color.magenta);
		return label;
	}


	/**
	 *  implements the method from interface Observer
	 *
	 *@param  o    Description of the Parameter
	 *@param  arg  Description of the Parameter
	 */
	public void update(Observable o, Object arg) {
		cat.debug("updating observer AnalysisTasksPanel");
		_dataModel = (DataModel) o;
		this.revalidate();
	}
	// helpers for the exception listeners

	/**
	 *  fires an error message
	 *
	 *@param  title    Description of the Parameter
	 *@param  message  Description of the Parameter
	 *@param  ex       Description of the Parameter
	 */
	protected void fireError(String title, String message, final Throwable ex) {
		if(exception_handler == null) {
			if(title == null) {
				title = "Error: ";
			}
			if(message == null) {
				message = "";
			}
			if(ex != null) {
				ex.printStackTrace();
				message = message + '\n' + ex.getMessage();
			}
			System.err.println(message);
			JOptionPane.showMessageDialog(AnalysisTasksPanel.this, message, title, JOptionPane.ERROR_MESSAGE);
		} else {
			exception_handler.setError(title, message, ex);
		}
	}


	/**
	 *  fires a warning message
	 *
	 *@param  title    Description of the Parameter
	 *@param  message  Description of the Parameter
	 *@param  ex       Description of the Parameter
	 */
	protected void fireWarning(String title, String message, final Exception ex) {
		if(exception_handler == null) {
			if(title == null) {
				title = "Error: ";
			}
			if(message == null) {
				message = "";
			}
			if(ex != null) {
				ex.printStackTrace();
				message = message + '\n' + ex.getMessage();
			}

			JOptionPane.showMessageDialog(AnalysisTasksPanel.this, message, title, JOptionPane.WARNING_MESSAGE);
		} else {
			exception_handler.setWarning(title, message, ex);
		}
	}
	// I N N E R   C L A S S E S

	private static class DefaultUIRenderer implements UIRenderer {

		// fields
		private GridBagConstraints _c = new GridBagConstraints();
		private int _gridy = 0;
		/**  The file chooser */
		private JFileChooser chooser;


		/**
		 *  renders as many parameters as it can on the supplied JComponent The input
		 *  List of ParameterInfo objects will contain those that were not processed.
		 *  The specified Map will map parameter names to ParamRetrievor instances.
		 *
		 *@param  pane            Description of the Parameter
		 *@param  service         Description of the Parameter
		 *@param  params_list     Description of the Parameter
		 *@param  name_retriever  Description of the Parameter
		 */
		public void render(final JComponent pane, final AnalysisService service, final java.util.List params_list, final Map name_retriever) {

			_c.anchor = GridBagConstraints.WEST;
			TaskInfo task = service.getTaskInfo();
			if(task != null) {
				//_id = task.getID();
				//create task label
				String desc = task.getDescription();
				pane.add(createLabelPanel(desc), _c);

				//create input panel
				if(!task.containsInputFileParam()) {
					pane.add(createInputPanel(null, "None", name_retriever), _c);
					pane.add(createFilePanel(null, "Nothing", name_retriever), _c);
				}
				//get parameters
				//ParameterInfo[] params = task.getParameterInfoArray();
				ParameterInfo[] params = (ParameterInfo[]) params_list.toArray(new ParameterInfo[params_list.size()]);
				//loop through the parameters to create UI
				int i;
				//loop through the parameters to create UI
				int length = 0;
				if(params != null) {
					length = params.length;
				}
				_gridy++;
				for(i = 0; i < length; i++) {
					ParameterInfo info = params[i];
					//_gridy=_gridy+i;
					_gridy++;
					pane.add(createParamPanel(info, name_retriever), _c);
				}
			}
			params_list.clear();
		}


		private JPanel createParamPanel(final ParameterInfo info, final Map name_retriever) {
			final String param_desc = info.getDescription();
			final String param_name = info.getName();
			final String param_value = info.getValue();
			cat.debug("name:" + param_name + " value:" + param_value + " desc:" + param_desc);
			System.out.println("name:" + param_name + " value:" + param_value + " desc:" + param_desc);

			//create param panel
			final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			JLabel label = new JLabel(param_desc + ": ");
			panel.add(label);

			//check the parameter value to decide which kind of JComponent to create
			if(param_value == null || param_value.equals("")) {
				cat.debug("creating text field");
				//            final JTextField field = new JTextField(35);
				//            panel.add(field);
				//            setGridBagConstraints(_c, 0, _gridy,GridBagConstraints.REMAINDER, 1, 100, 1);
				if(info.isInputFile()) {
					//panel.add(createBrowseButton(field));

					panel.add(createBrowseButton(info, param_name, info.getLabel(), name_retriever));
					//this._paramFileField.put(param_name, field);
				} else {

					//_paramField.put(param_name+":JTextField", field);
					panel.add(createTextInput(info, param_name, param_desc, name_retriever));
				}
			} else {
				StringTokenizer tokenizer = new StringTokenizer(param_value, ";");
				final JComboBox list = new JComboBox();
				while(tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					cat.debug("token:" + token);
					list.addItem(token);
				}
				//JComboBox list = new JComboBox(content);
				panel.add(list);
				//setGridBagConstraints(_c, 0, _gridy, GridBagConstraints.REMAINDER, 1, 100, 1);
				//_paramField.put(param_name+":JComboBox", list);

				name_retriever.put(param_name,
					new NoFileParamRetrievor() {
						/**
						 *  returns the value
						 *
						 *@return    The value
						 */
						public String getValue() {
							return list.getSelectedItem().toString();
						}


						public ParameterInfo getParameterInfo() {
							return info;
						}
					});
			}
			setGridBagConstraints(_c, 0, _gridy, GridBagConstraints.REMAINDER, 1, 100, 1);
			return panel;
		}


		private JPanel createInputPanel(final ParameterInfo info, final String param_name, final Map name_retriever) {
			JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JLabel inputLabel = new JLabel("Input: ");
			final JTextArea inputArea = new JTextArea(6, 50);
			JScrollPane textPane = new JScrollPane(inputArea);

			inputPanel.add(inputLabel);
			inputPanel.add(textPane);
			setGridBagConstraints(_c, 0, ++_gridy, 1, 4, 100, 4);

			name_retriever.put(param_name,
				new NoFileParamRetrievor() {
					/**
					 *  returns the value
					 *
					 *@return    The value
					 */
					public String getValue() {
						return inputArea.getText();
					}


					public ParameterInfo getParameterInfo() {
						return info;
					}
				});

			return inputPanel;
		}


		/**
		 *  creates a JTextField
		 *
		 *@param  info            Description of the Parameter
		 *@param  param_name      Description of the Parameter
		 *@param  desc            Description of the Parameter
		 *@param  name_retriever  Description of the Parameter
		 *@return                 Description of the Return Value
		 */
		private JComponent createTextInput(final ParameterInfo info, final String param_name, final String desc, final Map name_retriever) {
			final JPanel panel = new JPanel(); // FlowLayout
			panel.add(new JLabel(desc + ": "));
			final JTextField field = new JTextField(35);
			panel.add(field);
			name_retriever.put(param_name,
				new NoFileParamRetrievor() {
					/**
					 *  returns the value
					 *
					 *@return    The value
					 */
					public String getValue() {
						return field.getText();
					}


					public ParameterInfo getParameterInfo() {
						return info;
					}
				});
			return panel;
		}


		private JPanel createFilePanel(final ParameterInfo info, final String param_name, final Map name_retriever) {
			final JPanel filePanel = new JPanel();
			final JLabel inputFileLabel = new JLabel("Load file from disk: ");
			final JTextField filenameField = new JTextField(35);
			final JButton findButton = new JButton("Browse");
			final File[] file = new File[1];
			findButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						JFileChooser chooser = getFileChooser();
						int state = chooser.showOpenDialog(null);
						file[0] = chooser.getSelectedFile();
						if(file[0] != null && state == JFileChooser.APPROVE_OPTION) {
							filenameField.setText(file[0].getPath());
						}
					}
				});

			filePanel.add(inputFileLabel);
			filePanel.add(filenameField);
			filePanel.add(findButton);
			//_gridy=_gridy+4;
			_gridy++;
			setGridBagConstraints(_c, 0, _gridy, 1, 1, 100, 1);

			name_retriever.put(param_name,
				new ParamRetrievor() {
					/**
					 *  returns the value
					 *
					 *@return                  The value
					 *@exception  IOException  Description of the Exception
					 */
					public String getValue() throws IOException {
						final StringBuffer buffer = new StringBuffer();
						final BufferedReader reader = new BufferedReader(new FileReader(filenameField.getText()));
						for(String line = reader.readLine(); line != null; line = reader.readLine()) {
							buffer.append(line);
						}
						return buffer.toString();
					}


					/**
					 *  returns true if the value should be in a file
					 *
					 *@return    The file
					 */
					public boolean isFile() {
						return true;
					}


					public String getSourceName() {
						if(file[0] == null) {
							throw new IllegalStateException("No File Choosen!");
						}
						try {
							return file[0].getCanonicalPath();
						} catch(java.io.IOException ex) {
							throw new IllegalStateException(ex.getMessage());
						} // wouldn't happen
					}


					public ParameterInfo getParameterInfo() {
						return info;
					}
				});

			return filePanel;
		}


		/**
		 *  creates an Object input panel
		 *
		 *@param  info            Description of the Parameter
		 *@param  param_name      Description of the Parameter
		 *@param  label           Description of the Parameter
		 *@param  name_retriever  Description of the Parameter
		 *@return                 Description of the Return Value
		 */
		private JComponent createBrowseButton(final ParameterInfo info, final String param_name, final String label, final Map name_retriever) {
			final JPanel panel = new JPanel();
			if(label != null && label.length() > 0) {
				panel.add(new JLabel(label));
			}
			final JTextField filenameField = new JTextField(35);
			panel.add(filenameField);

			final JButton findButton = new JButton("Browse");
			panel.add(findButton);
			_gridy++;

			final File[] file = new File[1];
			findButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						JFileChooser chooser = getFileChooser();
						int state = chooser.showOpenDialog(null);
						file[0] = chooser.getSelectedFile();
						if(file[0] != null && state == JFileChooser.APPROVE_OPTION) {
							filenameField.setText(file[0].getPath());
						}
					}
				});
			name_retriever.put(param_name,
				new ParamRetrievor() {
					/**
					 *  returns the value
					 *
					 *@return                  The value
					 *@exception  IOException  Description of the Exception
					 */
					public String getValue() throws IOException {
						final StringBuffer buffer = new StringBuffer();
						final BufferedReader reader = new BufferedReader(new FileReader(filenameField.getText()));
						for(String line = reader.readLine(); line != null; line = reader.readLine()) {
							buffer.append(line);
						}
						return buffer.toString();
					}


					public boolean isFile() {
						return true;
					}


					public String getSourceName() {
						if(file[0] == null) {
							throw new IllegalStateException("No File Choosen for " + label + "!");
						}
						try {
							return file[0].getCanonicalPath();
						} catch(java.io.IOException ex) {
							throw new IllegalStateException(ex.getMessage());
						} // wouldn't happen
					}


					public ParameterInfo getParameterInfo() {
						return info;
					}
				});
			return panel;
		}
		//helper

		private JFileChooser getFileChooser() {
			if(chooser == null) {
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
			}
			return chooser;
		}


		/**
		 *  creates a component that is or contains another component that the user
		 *  can press (a JButton) to submit the job
		 *
		 *@param  service   Description of the Parameter
		 *@param  listener  Description of the Parameter
		 *@return           Description of the Return Value
		 */
		public JComponent createSubmitPanel(final AnalysisService service, final java.awt.event.ActionListener listener) {
			final JButton submit = new JButton("Submit");
			submit.addActionListener(listener);
			return submit;
		}


		/**
		 *  creates a component that is the label for the analysis service.
		 *
		 *@param  service  the analysis service that defines a task
		 *@return          JComponent the label or name of this task
		 */
		public JComponent createTaskLabel(final AnalysisService service) {
			String desc = service.getTaskInfo().getDescription();
			return createLabelPanel(desc);
		}
	}


	/**
	 *  Abstact implemtation for those non file ParamRetrievor implementations
	 *
	 *@author     jgould
	 *@created    February 9, 2004
	 */
	private abstract static class NoFileParamRetrievor implements ParamRetrievor {
		/**
		 *  returns the value
		 *
		 *@return                          The value
		 *@exception  java.io.IOException  Description of the Exception
		 */
		public abstract String getValue() throws java.io.IOException;


		/**
		 *  returns true if the value should be in a file
		 *
		 *@return    The file
		 */
		public final boolean isFile() {
			return false;
		}


		/**
		 *  returns the file name URI or null
		 *
		 *@return    The sourceName
		 */
		public final String getSourceName() {
			throw new UnsupportedOperationException("not implemented not a file");
		}
	}


	/**
	 *  handles getting the valuesfrom all the input components
	 *
	 *@author     jgould
	 *@created    February 9, 2004
	 */
	private class SubmitActionListener implements ActionListener {
		public final void actionPerformed(ActionEvent ae) {
		final JButton source = (JButton) ae.getSource();
		source.setEnabled(false);
		final Map nameRetriever = name_retriever;
		final AnalysisService selectedService = _selectedService;
				new Thread() {
					public final void run() {
						try {
							submit(nameRetriever, selectedService);
						} catch(Throwable th) {
							exception_handler.setError("Error:", "While submitting " + _selectedService.toString() + ':', th);
						} finally {
							if(source != null && source.isDisplayable()) {
								source.setEnabled(true);
							}
						}
					}
				}.start();
		}


		private void submit(Map nameRetriever, AnalysisService _selectedService) {
			final int size = name_retriever.size();
			//final ParameterInfo[] parmInfos = new ParameterInfo[size];
			final List param_list = new ArrayList(size);
			String param_name = "";
			System.out.println("New params Names:");
			try {
				//int i = 0;
				final ParameterInfo[] old_params = _selectedService.getTaskInfo().getParameterInfoArray();
				final int old_parm_cnt = ((old_params != null) ? old_params.length : 0);
				for(int i = 0, j = 0; i < old_parm_cnt; i++) {
					//final String name = (String)iter.next();
					final String name = old_params[i].getName();
					System.out.println(name);
					param_name = name;
					final ParamRetrievor rtrvr = (ParamRetrievor) name_retriever.get(name);
					if(rtrvr == null) { // skip those not processed
						System.out.println("No ParamRetrievor skipping " + name);
						continue;
					}
					final boolean is_file = rtrvr.isFile();

					final ParameterInfo param_copy = rtrvr.getParameterInfo();
					if(is_file) {
						param_copy.getAttributes().put(GPConstants.PARAM_INFO_CLIENT_FILENAME[0], rtrvr.getSourceName());
					}
					final String contents = (is_file) ? rtrvr.getSourceName() : rtrvr.getValue();

					//System.out.println("is File " + is_file + " contents \"" + contents + "\"");
					//final HashMap old_attrs = old_params[i].getAttributes();
					final HashMap old_attrs = param_copy.getAttributes();
					if(contents == null || contents.length() == 0) {
						final Object optional = old_attrs.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
						System.out.println("Optional=\"" + optional + "\"");
						if(optional != null && optional.toString().length() > 0) {
							System.out.println("Skipping optional parameter" + old_params[i].getName());
							continue;
						} else {
							System.err.println("Error: required parameter left blank: " + name);
							fireWarning("Input Error: ", "Required parameter left blank: " + name, null);
							return;
						}
					}
					param_copy.setValue(contents);
					param_copy.setDescription((is_file) ? "input file" : "Job");
					System.out.println("paramInfo=" + param_copy);
					param_list.add(param_copy);
					j++;

				}
			} catch(IOException ioe) {
				cat.debug("Error in reading value(" + param_name + "). " + ioe.getMessage());
				fireError("Error: ", "While getting the input data for " + param_name + ".", ioe);
				return;
			}
			final int num_params = param_list.size();
			final ParameterInfo[] parmInfos = (ParameterInfo[]) param_list.toArray(new ParameterInfo[num_params]);

			
			java.util.ArrayList directoryInputs = new java.util.ArrayList();
			for(int i = 0, length = parmInfos.length; i < length; i++) {
				if(ParameterInfo.FILE_TYPE.equals(parmInfos[i].getAttributes().get(ParameterInfo.TYPE)) && ParameterInfo.INPUT_MODE.equals(parmInfos[i].getAttributes().get(ParameterInfo.MODE))) {
					File temp = new File(parmInfos[i].getValue());
					if(temp.isDirectory()) {
						directoryInputs.add(parmInfos[i]);
					}
				}
			}
			if(directoryInputs.size()>1) {
				String message = "<html><p>Only one input can be a folder.<br>The following inputs are folders: ";
				for(int i = 0, length = directoryInputs.size(); i < length; i++) {
					ParameterInfo param = (ParameterInfo) directoryInputs.get(i);
					message +="<br>" + param.getName();
				}
				
				JOptionPane.showMessageDialog(AnalysisTasksPanel.this, message, "Submit Error", JOptionPane.ERROR_MESSAGE);
				return;
						
			}
			ParameterInfo directoryParam = null;
			if(directoryInputs.size()==1) {
				directoryParam = (ParameterInfo) directoryInputs.get(0);
			}
			
			//submit job, update the dataModel, delete temp file
			try {
				final String name = _selectedService.getName();
				
				final RequestHandler handler = RequestHandlerFactory.getInstance(username, password).getRequestHandler(name);
				final int id = _selectedService.getTaskInfo().getID();
				final TaskSubmitter task_submitter = getPreferedTaskSubmitter(_selectedService, id, parmInfos, handler);
				if(handler != null) {
					
					if(directoryParam != null) {
						File dir = new File(directoryParam.getValue());
						File[] files = dir.listFiles(new java.io.FileFilter() {
							public boolean accept(File f) {
								return !f.isDirectory()	&& !f.getName().startsWith(".");
							}
						});
						for(int i = 0, length = files.length; i < length; i++) {
							directoryParam.getAttributes().put(GPConstants.PARAM_INFO_CLIENT_FILENAME[0], files[i].getCanonicalPath());
							directoryParam.setValue(files[i].getCanonicalPath());
						
							AnalysisJob aJob = task_submitter.submitTask(_selectedService, id, parmInfos, handler);
							if(aJob != null) {
								AnalysisTasksPanel.this._dataModel.addJob(aJob);
							}
							
							//try {
						//		Thread.sleep(1000); // this seems to help with synchronization issues
						//	} catch(InterruptedException ie){} 
						}
					} else {
						// submit here
						AnalysisJob aJob = task_submitter.submitTask(_selectedService, id, parmInfos, handler);
						if(aJob != null) {
							AnalysisTasksPanel.this._dataModel.addJob(aJob);
						}
					}
				} else {
					cat.debug("Error in getting the handler");
					fireError("Internal Error: ", "Cannot get the request handler", null);
				}
			} catch(IOException ioe) {
				fireError("Error: ", "Error while submitting " + _selectedService, ioe);
				return;
			} catch(OmnigeneException oe) {
				fireError("Internal Error: ", "Error while submitting " + _selectedService, oe);
				return;
			} catch(WebServiceException wse) {
				if(wse.getMessage().indexOf("ConnectException") >= 0) {
					GenePattern.getDataObjectBrowser().disconnectedFromServer();
				} else {
					cat.error("", wse);
					fireError("Internal Error: ", "Error while submitting " + _selectedService, wse);
				}
			} catch(Throwable t) {
				cat.error("", t);
				fireError("Internal Error: ", "Error while submitting " + _selectedService, t);
			}

		}


		/**
		 *  returns the last (in list) TaskSubmitter that will handle submitting the
		 *  job
		 *
		 *@param  selectedService  Description of the Parameter
		 *@param  id               Description of the Parameter
		 *@param  parmInfos        Description of the Parameter
		 *@param  handler          Description of the Parameter
		 *@return                  The preferedTaskSubmitter
		 */
		private TaskSubmitter getPreferedTaskSubmitter(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) {
			for(int i = submitters.length - 1; i >= 0; i--) { // rev. loop
				final TaskSubmitter submitter = submitters[i];
				if(submitter.check(selectedService, id, parmInfos, handler)) {
					return submitter;
				}
			}
			return default_submitter; // default
		}

	} // end SubmitActionListener


	/**
	 *  default TaskSubmitter
	 *
	 *@author     jgould
	 *@created    February 9, 2004
	 */
	public static class DefaultTaskSubmitter implements TaskSubmitter {
		/**
		 *  submits the task
		 *
		 *@param  selectedService          Description of the Parameter
		 *@param  id                       Description of the Parameter
		 *@param  parmInfos                Description of the Parameter
		 *@param  handler                  Description of the Parameter
		 *@return                          Description of the Return Value
		 *@exception  OmnigeneException    Description of the Exception
		 *@exception  WebServiceException  Description of the Exception
		 *@exception  IOException          Description of the Exception
		 */
		public final AnalysisJob submitTask(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) throws OmnigeneException, WebServiceException, IOException {
			
			final String task = selectedService.getTaskInfo().getName();
			
			final String name = selectedService.getName();
			cat.debug("name: " + name + " tasks: " + task);

			final ParameterFormatConverter converter = new ParameterFormatConverter();
			System.out.println(converter.getJaxbString(parmInfos));

			final JobInfo job = handler.submitJob(id, parmInfos);
			final AnalysisJob aJob = new AnalysisJob(name, task, job);
			String lsid = (String) selectedService.getTaskInfo().getTaskInfoAttributes().get(GPConstants.LSID);
			aJob.setLSID(lsid);
			return aJob;
		}


		/**
		 *  determines if this submitter is acceptable for the AnalysisService since
		 *  this is the default it accepts any
		 *
		 *@param  _selectedService  Description of the Parameter
		 *@param  id                Description of the Parameter
		 *@param  parmInfos         Description of the Parameter
		 *@param  handler           Description of the Parameter
		 *@return                   Description of the Return Value
		 */
		public final boolean check(final AnalysisService _selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) {
			return true;
		}
	} // end DefaultTaskSubmitter


	/**
	 *  default ListTypeAdapter implementation that uses a JComboBox
	 *
	 *@author     jgould
	 *@created    February 9, 2004
	 */
	private final static class DefaultListTypeAdapter implements ListTypeAdapter {
		// fields
		/**  the Component */
		private final JComboBox component;


		DefaultListTypeAdapter() {
			this.component = new JComboBox();
		}


		public void addActionListener(ActionListener listener) {
			component.addActionListener(listener);
		}


		public void addItem(Object item) {
			component.addItem(item);
		}


		public void addItem(Object item, ActionListener listener) {
			component.addItem(item);
			component.addActionListener(listener);
		}


		public java.awt.Component getComponent() {
			return component;
		}


		public int getItemCount() {
			return component.getItemCount();
		}


		public Object getItemSelected() {
			return component.getSelectedItem();
		}


		public String getSelectedAsString() {
			return getItemSelected().toString();
		}


		public void insert(Object item, int index) {
			component.insertItemAt(item, index);
		}


		public boolean isLocalComponent() {
			return true;
		}


		public void remove(Object item) {
			component.removeItem(item);
		}


		public void remove(int index) {
			component.removeItemAt(index);
		}


		public void removeActionListener(ActionListener listener) {
			component.removeActionListener(listener);
		}


		public void removeAll() {
			component.removeAllItems();
		}
	}
}
