package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

public class SuiteCatalogBean {

    private SelectItem[] states = new SelectItem[] {
            new SelectItem("new", "new"), new SelectItem("updated", "updated"),
            new SelectItem("up to date", "up to date") };;

    private static Logger log = Logger.getLogger(SuiteCatalogBean.class);

    private boolean error;

    private List<String> selectedStates;

    private ArrayList<MySuiteInfo> suiteCatalogSuites;

    private static final Comparator versionComparator = new DescendingVersionComparator();

    private static final int UPDATED = 0;

    private static final int UPTODATE = 1;

    private static final int NEW = 2;

    private ArrayList<MySuiteInfo> filteredSuites;

    private Map<String, List<SuiteInfo>> allSuites;

    public SuiteCatalogBean() {

        try {
            allSuites = lsidToSuitesMap(new LocalAdminClient(UIBeanHelper
                    .getUserId()).getAllSuites());

            HashMap<String, Map> lsidToSuiteMap = new SuiteRepository()
                    .getSuites(System.getProperty("SuiteRepositoryURL"));
            Map<String, List<SuiteInfo>> suiteCatalogLsidToSuitesMap = new HashMap<String, List<SuiteInfo>>();
            for (Iterator<Map> it = lsidToSuiteMap.values().iterator(); it
                    .hasNext();) {
                SuiteInfo si = new SuiteInfo(it.next());
                List<SuiteInfo> suites = suiteCatalogLsidToSuitesMap.get(si
                        .getLsid());
                if (suites == null) {
                    suites = new ArrayList<SuiteInfo>();
                    suiteCatalogLsidToSuitesMap.put(si.getLsid(), suites);
                }
                suites.add(si);
            }
            suiteCatalogSuites = new ArrayList<MySuiteInfo>(
                    suiteCatalogLsidToSuitesMap.size());
            for (Iterator<List<SuiteInfo>> it = suiteCatalogLsidToSuitesMap
                    .values().iterator(); it.hasNext();) {
                List<SuiteInfo> suites = it.next();
                Collections.sort(suites, versionComparator);
                MySuiteInfo s = new MySuiteInfo(suites.get(0));
                if (suites.size() > 1) {
                    MySuiteInfo[] later = new MySuiteInfo[suites.size() - 1];
                    for (int i = 1; i < suites.size(); i++) {
                        later[i - 1] = new MySuiteInfo(suites.get(i));
                    }
                    s.setLaterVersions(later);
                }
                suiteCatalogSuites.add(s);
            }
            if (UIBeanHelper.getRequest().getParameter(
                    "suiteCatalogForm:suiteCatalogSubmit") == null) {
                selectedStates.add("new");
                selectedStates.add("updated");
                filter();
            }

        } catch (Exception e) {
            log.error(e);
            error = true;
        }

    }

    private Map<String, List<SuiteInfo>> lsidToSuitesMap(SuiteInfo[] suites) {
        Map<String, List<SuiteInfo>> lsidToSuitesMap = new HashMap<String, List<SuiteInfo>>();
        if (suites != null) {
            for (SuiteInfo si : suites) {

                try {
                    List<SuiteInfo> suiteList = lsidToSuitesMap.get(new LSID(si
                            .getLSID()).toStringNoVersion());
                    if (suiteList == null) {
                        suiteList = new ArrayList<SuiteInfo>();
                        lsidToSuitesMap.put(si.getLSID(), suiteList);
                    }

                    suiteList.add(si);
                } catch (MalformedURLException e) {
                    log.error(e);
                }

            }
        }
        for (Iterator<String> it = lsidToSuitesMap.keySet().iterator(); it
                .hasNext();) {
            List<SuiteInfo> lsidSuites = lsidToSuitesMap.get(it.next());
            Collections.sort(lsidSuites, versionComparator);
        }
        return lsidToSuitesMap;
    }

    /**
     * Gets whether the suite is already installed (without regard to version)
     * 
     */
    public boolean isAlreadyInstalled(String lsid) {
        try {
            return allSuites.containsKey(new LSID(lsid).toStringNoVersion());
        } catch (MalformedURLException e) {
            log.error(e);
            return false;
        }
    }

    public boolean isNewer(String lsid) {

        try {
            LSID reposLsid = new LSID(lsid);
            List<SuiteInfo> suites = allSuites.get(reposLsid
                    .toStringNoVersion());
            if (suites != null) {
                SuiteInfo si = suites.get(0);
                return Integer.parseInt(reposLsid.getVersion()) > Integer
                        .parseInt(new LSID(si.getLsid()).getVersion());
            }
        } catch (MalformedURLException e) {
            log.error(e);
        }

        return true;
    }

    public void filter() throws WebServiceException {
        boolean getNew = selectedStates.contains("new");
        boolean getUpdated = selectedStates.contains("updated");
        boolean getUpToDate = selectedStates.contains("up to date");
        filteredSuites = new ArrayList<MySuiteInfo>();

        for (MySuiteInfo si : suiteCatalogSuites) {
            String lsid = si.getLsid();
            int state = isAlreadyInstalled(lsid) ? (isNewer(lsid) ? UPDATED
                    : UPTODATE) : NEW;
            if (getNew && state == NEW) {
                filteredSuites.add(si);
            } else if (getUpdated && state == UPDATED) {
                filteredSuites.add(si);
            } else if (getUpToDate) {
                filteredSuites.add(si);
            }
        }
        Collections.sort(filteredSuites, new SuiteNameComparator());

    }

    public List<String> getSelectedStates() {
        return selectedStates;
    }

    public void setSelectedStates(List<String> l) {
        selectedStates = l;
    }

    public SelectItem[] getStates() {
        return this.states;
    }

    public void getStates(SelectItem[] l) {
        this.states = l;
    }

    public boolean isError() {
        return error;
    }

    private static class DescendingVersionComparator implements
            Comparator<SuiteInfo> {

        public int compare(SuiteInfo t1, SuiteInfo t2) {
            try {
                return new Integer(Integer.parseInt(new LSID(t2.getLsid())
                        .getVersion())).compareTo(Integer.parseInt(new LSID(t1
                        .getLsid()).getVersion()));
            } catch (MalformedURLException e) {
                log.error(e);
                return 0;
            }
        }

    }

    private static class SuiteNameComparator implements Comparator<MySuiteInfo> {

        public int compare(MySuiteInfo t1, MySuiteInfo t2) {
            return t1.getName().compareToIgnoreCase(t2.getName());
        }

    }

    public static class MySuiteInfo {
        SuiteInfo suiteInfo;

        MySuiteInfo[] laterVersions;

        public MySuiteInfo(SuiteInfo info) {
            this.suiteInfo = info;
        }

        public boolean equals(Object otherThing) {
            return suiteInfo.equals(otherThing);
        }

        public int getAccessId() {
            return suiteInfo.getAccessId();
        }

        public String getAuthor() {
            return suiteInfo.getAuthor();
        }

        public String getDescription() {
            return suiteInfo.getDescription();
        }

        public String[] getDocFiles() {
            return suiteInfo.getDocFiles();
        }

        public String[] getDocumentationFiles() {
            return suiteInfo.getDocumentationFiles();
        }

        public String getID() {
            return suiteInfo.getID();
        }

        public String getLsid() {
            return suiteInfo.getLsid();
        }

        public String getLSID() {
            return suiteInfo.getLSID();
        }

        public String[] getModuleLsids() {
            return suiteInfo.getModuleLsids();
        }

        public String[] getModuleLSIDs() {
            return suiteInfo.getModuleLSIDs();
        }

        public String getName() {
            return suiteInfo.getName();
        }

        public String getOwner() {
            return suiteInfo.getOwner();
        }

        public int hashCode() {
            return suiteInfo.hashCode();
        }

        public void setAccessId(int accessId) {
            suiteInfo.setAccessId(accessId);
        }

        public void setAuthor(String userId) {
            suiteInfo.setAuthor(userId);
        }

        public void setDescription(String description) {
            suiteInfo.setDescription(description);
        }

        public void setDocFiles(String[] docFiles) {
            suiteInfo.setDocFiles(docFiles);
        }

        public void setDocumentationFiles(String[] mods) {
            suiteInfo.setDocumentationFiles(mods);
        }

        public void setID(String ID) {
            suiteInfo.setID(ID);
        }

        public void setLsid(String lsid) {
            suiteInfo.setLsid(lsid);
        }

        public void setLSID(String LSID) {
            suiteInfo.setLSID(LSID);
        }

        public void setModuleLsids(List<String> moduleList) {
            suiteInfo.setModuleLsids(moduleList);
        }

        public void setModuleLsids(String[] moduleLsids) {
            suiteInfo.setModuleLsids(moduleLsids);
        }

        public void setModuleLSIDs(String[] mods) {
            suiteInfo.setModuleLSIDs(mods);
        }

        public void setName(String taskName) {
            suiteInfo.setName(taskName);
        }

        public void setOwner(String userId) {
            suiteInfo.setOwner(userId);
        }

        public String toString() {
            return suiteInfo.toString();
        }

        public MySuiteInfo[] getLaterVersions() {
            return laterVersions;
        }

        public void setLaterVersions(MySuiteInfo[] laterVersions) {
            this.laterVersions = laterVersions;
        }

    }

    public ArrayList<MySuiteInfo> getFilteredSuites() {
        return filteredSuites;
    }
}
