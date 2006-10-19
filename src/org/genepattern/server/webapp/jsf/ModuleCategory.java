package org.genepattern.server.webapp.jsf;

import javax.faces.event.ActionEvent;

import java.net.MalformedURLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

public class ModuleCategory implements java.io.Serializable {
    static Logger log = Logger.getLogger(ModuleCategory.class);

    private boolean expanded = true;
    private String name;
    private List modules;

    public ModuleCategory(String name, TaskInfo[] taskInfos) {
        this.name = name;
        initialize(taskInfos);
    }

    private void initialize(TaskInfo[] taskInfos) {
        HashMap<String, Module> tmp = new HashMap<String, Module>();
        for(TaskInfo ti : taskInfos) {
            try {
                LSID lsid = new LSID(ti.getLsid());
                int version = Integer.parseInt(lsid.getVersion());            
                Module module = tmp.get(ti.getName());
                if(module == null) {
                    tmp.put(ti.getName(), new Module(ti, version));
                }
                else {
                    module.addVersion(version);  
                }
            }
            catch (NumberFormatException e) {
                log.error("Non-numerical version number for lsid: " + ti.getLsid(), e);
            }
            catch (MalformedURLException e) {
                log.error("Malformed lsid: " + ti.getLsid(), e);
           }           
        }
        modules = new ArrayList(tmp.values());
        Collections.sort(modules, new Comparator() {
            public int compare(Object o1, Object o2) {
                String n1 = ((Module) o1).getName();
                String n2 = ((Module) o2).getName();
                return n1.compareTo(n2);
            }
            
        });
    }

    public String getName() {
        return name;
    }

    public List getModules() {
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

}
