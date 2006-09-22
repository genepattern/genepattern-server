package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.faces.component.html.HtmlDataTable;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.util.PropertiesManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.IGPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class CommandPrefixBean extends AbstractUIBean implements IGPConstants {

	  private static Logger log = Logger.getLogger(CommandPrefixBean.class);
	  LocalAdminClient admin;
		
	  HtmlDataTable prefixTable = null;
	  HtmlDataTable tpmappingTable = null;
		
	  private String defaultCommandPrefix;
	  
	  private String newPrefixName;
	  private String newPrefixValue;
	  
	  private List newMappingLSID;
	  private String newMappingPrefix;
	  
	  PropertiesManager pm = null;
	  
	  public CommandPrefixBean(){
		  
		  admin = new LocalAdminClient(getUserId());
		  pm = PropertiesManager.getInstance(); 
		  defaultCommandPrefix = System.getProperty(COMMAND_PREFIX);
	  }
	  
	  
	  
	public String getDefaultCommandPrefix() {
		return defaultCommandPrefix;
	}



	public void setDefaultCommandPrefix(String defaultCommandPrefix) {
		this.defaultCommandPrefix = defaultCommandPrefix;
	}



	public List getCommandPrefixes() {
		
		 return new ArrayList(pm.getCommandPrefixes().entrySet());
	}

	public List<SelectItem> getCommandPrefixesAsSelectItems() {
		ArrayList out = new ArrayList<SelectItem>();
		for (Iterator iter = pm.getCommandPrefixes().keySet().iterator(); iter.hasNext(); ){
			String key = (String)iter.next();
			out.add(new SelectItem(key, key));
		}
		return out;
	}
	
	
	/**
	 * We store the mapping with LSIDs (unique) but display with names (not unique)
	 * @return
	 */
	public List getTaskPrefixMapping() throws WebServiceException{
		ArrayList out = new ArrayList();
		Properties taskPrefixMapping = pm.getTaskPrefixMapping();
		
		//return tastPrefixMapping; by name not LSID as we really keep it
		for (Object lsidStr : taskPrefixMapping.keySet() ){
			String name = nameFromLSID((String)lsidStr);	
			out.add(new KeyValuePair(name, (String)lsidStr, taskPrefixMapping.getProperty((String)lsidStr)));
		}
		return out;
	}

	
	
	public String getNewPrefixName() {
		return newPrefixName;
	}
	
	public void setNewPrefixName(String newPrefix) {
		this.newPrefixName = newPrefix;
	}
	
	
	public String getNewPrefixValue() {
		return newPrefixValue;
	}

	public void setNewPrefixValue(String newPrefix) {
		this.newPrefixValue = newPrefix;
	}
	
	
	 public List getNewMappingLSID() {
		return newMappingLSID;
	}

	public void setNewMappingLSID(List newMappingLSID) {
		this.newMappingLSID = newMappingLSID;
	}

	public String getNewMappingPrefix() {
		return newMappingPrefix;
	}

	public void setNewMappingPrefix(String newMappingPrefix) {
		this.newMappingPrefix = newMappingPrefix;
	}

	/**
     * Save a new set of prefixes for use in mappings
     * 
     * @param event --
     *            ignored
     */
    public void saveCommandPrefixes(ActionEvent event) {
    	Properties p = pm.getCommandPrefixes();
    	p.setProperty(newPrefixName, newPrefixValue);
    	pm.saveProperties(COMMAND_PREFIX, p);
    }

    public void deletePrefix(ActionEvent event) {
    	Map.Entry row = (Map.Entry)prefixTable.getRowData();
    	String prefixToDelete = (String)row.getKey();
    	Properties cp = pm.getCommandPrefixes();
    	cp.remove(prefixToDelete);
    	pm.saveProperties(COMMAND_PREFIX, cp);
    	
    	Properties tpm = pm.getTaskPrefixMapping();
    	boolean tpmChanged = false;
    	for (Object oKey : tpm.keySet() ){
    		String key = (String)oKey;
    		String prefixInUse = tpm.getProperty(key);
    		if (prefixInUse.equals(prefixToDelete)){
    			tpm.remove(key);
    			tpmChanged = true;
    		}
    	}
    	if (tpmChanged) pm.saveProperties(TASK_PREFIX_MAPPING, tpm);   	
    }
	
    public void saveDefaultCommandPrefix(ActionEvent event) {
    	
    	pm.storeChange(COMMAND_PREFIX, defaultCommandPrefix);
    	System.setProperty(COMMAND_PREFIX, defaultCommandPrefix);
    	System.out.println("set " + defaultCommandPrefix +"  to " + System.getProperty(COMMAND_PREFIX));
    }
    
    /**
     * Save the mappings between a task and a prefix
     * 
     * @param event -- ignored
     */
    public void savePrefixTaskMapping(ActionEvent event) throws MalformedURLException, WebServiceException{
      	Properties p = pm.getTaskPrefixMapping();
      	
      	for (String anLsid : (List<String>)newMappingLSID){
      		String lsid = lsidFromName(anLsid);
      		
      		p.setProperty(lsid, newMappingPrefix);
      	}
      	pm.saveProperties(TASK_PREFIX_MAPPING, p);
    }
    
    public void deleteTaskPrefixMapping(ActionEvent event)  throws MalformedURLException, WebServiceException{
    	KeyValuePair row = (KeyValuePair)tpmappingTable.getRowData();
    	
    	Properties p = pm.getTaskPrefixMapping();
    	String k = lsidFromName(row.getAltKey());
    	p.remove(k);
    	pm.saveProperties(TASK_PREFIX_MAPPING, p);
    	
    }
   
    
	protected String nameFromLSID(String lsid) throws WebServiceException {
		
		TaskInfo task = admin.getTask(lsid);
		if (task != null)
			return task.getName();	
		else 
			return lsid;
		
	}
	
	protected String lsidFromName(String name) throws MalformedURLException, WebServiceException{
		
		TaskInfo task = admin.getTask(name);
		if (task != null){
			LSID lsid = new LSID((String)task.getTaskInfoAttributes().get("LSID"));
			return lsid.toStringNoVersion();	
		} else {
			return name;
		}
	}
    
	
	public HtmlDataTable getPrefixTable() {
		return prefixTable;
	}



	public void setPrefixTable(HtmlDataTable prefixTable) {
		this.prefixTable = prefixTable;
	}



	public HtmlDataTable getTpmappingTable() {
		return tpmappingTable;
	}



	public void setTpmappingTable(HtmlDataTable tpmappingTable) {
		this.tpmappingTable = tpmappingTable;
	}
	
}


