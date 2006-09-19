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
import java.util.Properties;
import java.util.Set;

import javax.faces.component.html.HtmlDataTable;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.IGPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

public class CommandPrefixBean extends AbstractUIBean implements IGPConstants {

	  private static Logger log = Logger.getLogger(CommandPrefixBean.class);
	  LocalAdminClient admin;
		
	  HtmlDataTable prefixTable = null;
		
	  private Properties commandPrefixes;
	  private Properties taskPrefixMapping;
	  private static final String TPM_Name = TASK_PREFIX_MAPPING;
	  private static final String CP_Name = COMMAND_PREFIX;
	  
	  private String newPrefixName;
	  private String newPrefixValue;
	  
	  private String newMappingLSID;
	  private String newMappingPrefix;
	  
	  public CommandPrefixBean(){
		  try {
			  admin = new LocalAdminClient(getUserName());
	  
			  commandPrefixes = (Properties)getRequest().getSession().getAttribute(CP_Name);
			  taskPrefixMapping = (Properties)getRequest().getSession().getAttribute(TPM_Name);
			  
			  if ((commandPrefixes == null) || (taskPrefixMapping==null)) {  
				  reloadFromDisk();
			  }
			  
		  }catch (IOException e){
			  log.error(e);
			  commandPrefixes = new Properties();
			  taskPrefixMapping = new Properties();
		  }
		 
	  }
	  
	  
	  
	public List getCommandPrefixes() {
		 return new ArrayList(commandPrefixes.entrySet());
	}

	public List<SelectItem> getCommandPrefixesAsSelectItems() {
		ArrayList out = new ArrayList<SelectItem>();
		for (Iterator iter = commandPrefixes.keySet().iterator(); iter.hasNext(); ){
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
		//return tastPrefixMapping; by name not LSID as we really keep it
		for (Object lsidStr : taskPrefixMapping.keySet() ){
			String name = nameFromLSID((String)lsidStr);	
			out.add(new KeyValuePair(name, taskPrefixMapping.getProperty((String)lsidStr)));
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
	
	
	 public String getNewMappingLSID() {
		return newMappingLSID;
	}

	public void setNewMappingLSID(String newMappingLSID) {
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
    	commandPrefixes.setProperty(newPrefixName, newPrefixValue);
    	storeChanges(CP_Name, commandPrefixes);
    }

    public void deletePrefix(ActionEvent event) {
    	System.out.println("-->"+prefixTable.getRowIndex());
    	System.out.println("-->"+prefixTable.getRowData().getClass());
    	
    	//commandPrefixes.setProperty(newPrefixName, newPrefixValue);
    	//storeChanges(CP_Name, commandPrefixes);
    }
	
    /**
     * Save the mappings between a task and a prefix
     * 
     * @param event -- ignored
     */
    public void savePrefixTaskMapping(ActionEvent event) throws MalformedURLException, WebServiceException{
      	log.info("savePrefixTaskMappings");
		String lsid = lsidFromName(newMappingLSID);
		taskPrefixMapping.setProperty(lsid, newMappingPrefix);
        storeChanges(TPM_Name, taskPrefixMapping);
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
    
	public static String getPropsDir(){
		return System.getProperty("genepattern.properties"); // props dir
	}
	
	public boolean storeChanges(String name, Properties props){
		boolean storeSuccess = false;
			
		getRequest().getSession().setAttribute(name, props);		
		try {
			File propFile = new File(getPropsDir(), name + ".properties");
			FileOutputStream fos = new FileOutputStream(propFile);
			props.store(fos, " ");
			fos.close();			
			fos = null;
			storeSuccess = true;
		} catch (Exception e){
			storeSuccess = false;
		} 
		
		return storeSuccess;
	}
	
	public void reloadFromDisk() throws IOException {
		  commandPrefixes = new Properties();
		  taskPrefixMapping = new Properties();
		  File cpFile = new File(getPropsDir(), CP_Name+".properties");
		  File tpmFile = new File(getPropsDir(), TPM_Name +".properties");
		  
		  if (cpFile.exists()) commandPrefixes.load(new FileInputStream(cpFile));
		  if (tpmFile.exists())taskPrefixMapping.load(new FileInputStream(tpmFile));
		  
		  getRequest().getSession().setAttribute(CP_Name, commandPrefixes);		
		  getRequest().getSession().setAttribute(TPM_Name, taskPrefixMapping);		  	 
	}



	public HtmlDataTable getPrefixTable() {
		return prefixTable;
	}



	public void setPrefixTable(HtmlDataTable prefixTable) {
		this.prefixTable = prefixTable;
	}
	
}


