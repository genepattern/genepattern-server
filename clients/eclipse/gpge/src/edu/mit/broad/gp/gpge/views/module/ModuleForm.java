/*
 * Created on Jun 17, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.module;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import edu.mit.broad.gp.core.ServiceManager;
import edu.mit.broad.gp.gpge.util.BrowserLauncher;
import org.genepattern.webservice.AnalysisService;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.util.GPConstants;


/**
 * @author genepattern
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ModuleForm extends ScrolledForm {
	ServiceManager serviceManager = null;
	AnalysisService service = null;
	Map controlMap;
	static Color red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
	/**
	 * @param parent
	 */
	public ModuleForm(Composite parent, ServiceManager mgr, AnalysisService service) {
		super(parent);
		serviceManager = mgr;
		this.service = service;
	}

	/**
	 * @param parent
	 * @param style
	 */
	public ModuleForm(Composite parent, int style) {
		super(parent, style);
		// TODO Auto-generated constructor stub
	}
	
	 
	public void copyAppearance(ScrolledForm peer){
	    this.setFont(peer.getFont());
	    this.setForeground(peer.getForeground());
	    this.setBackground(peer.getBackground());
	   
	}
	
	 public void createPartsForModule(FormToolkit toolkit, ScrolledForm peer) {
	     
	//public void createPartsForModule(AnalysisService service){
		//FormToolkit toolkit = new FormToolkit(getParent().getDisplay());
		toolkit.adapt(this);
		copyAppearance(peer);
		
		org.eclipse.swt.layout.GridLayout layout = new org.eclipse.swt.layout.GridLayout();
		layout.numColumns=3;
		this.getBody().setLayout(layout);
		this.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		TaskInfo ti = service.getTaskInfo();
		this.setData("name", ti.getName());		
				
		
		this.setText(ti.getName());
		
		
		String description = ti.getDescription();
		if(description!=null && !description.equals("")) {
		    Label lblDesc = toolkit.createLabel(this.getBody(), "Description:");		
		    GridData lblGridData = new GridData(GridData.GRAB_HORIZONTAL);
		    lblGridData.horizontalAlignment = GridData.END;
		    lblDesc.setLayoutData(lblGridData);
		    lblDesc.setForeground(peer.getForeground());
		    lblDesc.setBackground(peer.getBackground());
	   
		
		    GridData descLayout = new GridData(GridData.GRAB_HORIZONTAL & GridData.GRAB_VERTICAL);
		    descLayout.horizontalSpan = 2;
		    Label svcdesc = toolkit.createLabel(this.getBody(), description);		
		    svcdesc.setLayoutData(descLayout);
		}
		
		
		controlMap = createPartsForModuleOnForm(service, this.getBody());
//		 remember what went into what control for later retrieval
		this.setData("controlMap", controlMap);
		
		String helpUrl = "http://" + service.getName() + "/gp/getTaskDoc.jsp?name="+ ti.getName();
		this.setData("helpUrl", helpUrl);		
		
		final ScrolledForm form = this;
		
		Composite c = toolkit.createComposite(getBody());
		GridLayout btnLayout = new GridLayout();
		btnLayout.numColumns = 3;
		c.setLayout(btnLayout);
		
		// add the submit and help buttons
		Button submit = toolkit.createButton(c,"Submit", SWT.PUSH);
		submit.addSelectionListener(new SelectionAdapter() {
 	      public void widgetSelected(SelectionEvent event) {
	 	       String name = (String)form.getData("name");
	 	       HashMap controlMap = (HashMap)form.getData("controlMap");
	 	       SubmitAnalysisAction saa = new SubmitAnalysisAction(name, controlMap, serviceManager, service.getTaskInfo().getParameterInfoArray(), getShell());
	 	       saa.submitAnalysis();
	 	  }
 	    });
	
		Button help = toolkit.createButton(c,"Help", SWT.PUSH);
		help.addSelectionListener(new SelectionAdapter() {
	 	      public void widgetSelected(SelectionEvent event) {
		 	      String url = (String)form.getData("helpUrl");
		 	      try {
		 	          BrowserLauncher.openURL(url);
		 	      } catch (Exception e){
		 	          e.printStackTrace();
		 	      }
		 	  }
	 	    });
		
		Button reset = toolkit.createButton(c,"Reset", SWT.PUSH);
		reset.addSelectionListener(new SelectionAdapter() {
	 	      public void widgetSelected(SelectionEvent event) {
	 	         setDefaultValues();
		 	  }
	 	    });
		
		form.setVisible(false);
		
	}
	
	 public static String getParameterText(ParameterInfo param){
	     return param.getName().replace('.', ' ');
	 }
	 
	 /**
	  * 
	  * Displays the default values for this module. 
	  *
	  */
	 public void setDefaultValues() {
	     TaskInfo ti = service.getTaskInfo();
	     Map tia = ti.getTaskInfoAttributes();
	     ParameterInfo[] params = (ParameterInfo[])ti.getParameterInfoArray();
		for(int i = 0, length = params.length; i < length; i++) {
		    ParameterInfo param = params[i];
			HashMap attrs = param.getAttributes();
			String defaultVal = (String)attrs.get("default_value");
			if (defaultVal == null) defaultVal = "";
			Control c = (Control) controlMap.get(param.getName());
			if (c.getClass() == Text.class) {
			    ((Text)c).setText(defaultVal);
			} else {
			    int index = ((Combo)c).indexOf(defaultVal);
			    if(index!=-1) {
			        ((Combo)c).select(index);
			    } else {
			        ((Combo)c).select(0);
			    }
			}
		    
		}
	 }
	 
	public static Map createPartsForModuleOnForm(AnalysisService service, Composite parent){
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		TaskInfo ti = service.getTaskInfo();
		
		Map tia = ti.getTaskInfoAttributes();
		ParameterInfo[] params = (ParameterInfo[])ti.getParameterInfoArray();
		toolkit.adapt(parent);
		//form.setText("Module: " + ti.getName());
		
		if (params == null) params = new ParameterInfo[0];
		HashMap controlMap = new HashMap();
		for (int i=0; i < params.length; i++){
			ParameterInfo param = params[i];
			HashMap attrs = param.getAttributes();
			String defaultVal = (String)attrs.get("default_value");
			if (defaultVal == null) defaultVal = "";
			String value = param.getValue();
			
			GridData gridData = new GridData(GridData.GRAB_HORIZONTAL);
			gridData.horizontalAlignment = GridData.END;
			Composite c = toolkit.createComposite(parent);
			GridLayout nameLayout = new GridLayout();
			nameLayout.numColumns = 2;
			c.setLayout(nameLayout);
			
			Label name = toolkit.createLabel(c, getParameterText(param));
			String sOptional = (String)attrs.get(GPConstants.PARAM_INFO_OPTIONAL[0]);
			boolean optional = (sOptional != null && sOptional.length() > 0);
			if(!optional) {
			    Label star = toolkit.createLabel(c, "*");
				star.setForeground(red);
			}
			name.setLayoutData(gridData);
		
			
			if ((value == null) || (value.length() == 0)){
				final Text text = toolkit.createText(parent, defaultVal, SWT.BORDER);
				text.setTextLimit(60);
				controlMap.put(param.getName(), text);
				
				int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;
				Transfer[] types = new Transfer[] {TextTransfer.getInstance(), org.eclipse.swt.dnd.FileTransfer.getInstance()};
				DropTarget target = new DropTarget(text, operations);
				target.setTransfer(types);
				target.addDropListener (new DropTargetAdapter() {
					public void drop(DropTargetEvent event) {
						if (event.data == null) {
							event.detail = DND.DROP_NONE;
							return;
						}
						if(event.data instanceof String[]){ // drag from finder
						    text.setText(((String[])event.data)[0]);
						} else {
						    text.setText (event.data.toString());
						}
					}
				});
				
			} else {
			    
				Combo values = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
				
				StringTokenizer strtok = new StringTokenizer(value, ";");
				Vector items = new Vector(); 
				while (strtok.hasMoreTokens()){
					String token = strtok.nextToken();
					StringTokenizer strtok2 = new StringTokenizer(token, "=");
					String val = null;
					if (strtok2.hasMoreTokens()){
						val = strtok2.nextToken();
					} else {
						val = "";
					}
					if (strtok2.hasMoreTokens()){
						String lab = strtok2.nextToken();
						items.add(lab);
					} else {
						items.add(val);
					}
				}
				values.setItems((String[])items.toArray(new String[items.size()]));
				
				int index = values.indexOf(defaultVal);
			    if(index!=-1) {
			        values.select(index);
			    } else {
			        values.select(0);
			    }
				controlMap.put(param.getName(), values);
			}
			Label desc = toolkit.createLabel(parent, param.getDescription());
							
		}
		
		return controlMap;
	}
	

}
