package org.genepattern.server.webapp.jsf;

import javax.faces.event.ActionEvent;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

public class ModuleCategory implements java.io.Serializable {
    static Logger log = Logger.getLogger(ModuleCategory.class);

    private boolean expanded = true;
    private String name;
    private List<Module> modules;

    public ModuleCategory(String name, TaskInfo[] taskInfos) {
        this.name = name;
        initialize(taskInfos);
    }

    private void initialize(TaskInfo[] taskInfos) {
        HashMap<String, Module> tmp = new HashMap<String, Module>();
        for(TaskInfo ti : taskInfos) {
            try {
                LSID lsid = new LSID(ti.getLsid());
                Module module = tmp.get(ti.getName());
                if(module == null) {
                    tmp.put(ti.getName(), new Module(ti, lsid));
                }
                else {
                    module.addVersion(lsid);  
                }
            }
            catch (NumberFormatException e) {
                log.error("Non-numerical version number for lsid: " + ti.getLsid(), e);
            }
            catch (MalformedURLException e) {
                log.error("Malformed lsid: " + ti.getLsid(), e);
           }           
        }
        modules = new ArrayList<Module>(tmp.values());
        Collections.sort(modules, new Comparator() {
            public int compare(Object o1, Object o2) {
                String n1 = ((Module) o1).getName();
                String n2 = ((Module) o2).getName();
                return n1.compareToIgnoreCase(n2);
            }
            
        });
    }

    public String getName() {
        return name;
    }
    
    public String getIdentifier() {
        
        return name. replace(' ', '_').replace('&', '-').trim();
        
    }

    public List<Module> getModules() {
        return modules;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean exp) {
        expanded = exp;
    }

    public void toggleExpanded(ActionEvent event) {
        expanded = !expanded;
        System.out.println("toggle");
    }

    public int getModuleCount() {
        return (modules == null ? 0 : modules.size());
    }
    
    public void setSelectedVersionOfModules(Map<String, TaskInfo> lsidToTaskInfoMap) {
    	LSID lsidObj=null;
    	for (Module module:modules) {
    		for (Map.Entry<String, TaskInfo> entry:lsidToTaskInfoMap.entrySet()) {
    			try {
    				lsidObj = new LSID(entry.getKey());
    			}catch (MalformedURLException e) {
                    log.error("Error parsing lsid: " + (String)entry.getKey(), e);
                }
    			String version = (lsidObj!=null)?lsidObj.getVersion():"";
    			if (!version.equals("")) {
    				module.setSelectedVersion(version);
    			}
    			
    		}
    		
    	}
    }
    
    public void setSelected(List<String> selectedLsids) {
    	LSID lsidObj=null;
    	for (String lsid : selectedLsids) {
    		try {
				lsidObj = new LSID(lsid);
			}catch (MalformedURLException e) {
                log.error("Error parsing lsid: " + lsid, e);
            }
    		
    		for (Module module:modules) {
    			if (lsid.equals(module.getLSID().toStringNoVersion())) {
    				module.setSelected(true);
    				if (!"".equals(lsidObj.getVersion())) {
    					module.setSelectedVersion(lsidObj.getVersion());
    				}
    			}
    		}
    		
    	}
    }

}
