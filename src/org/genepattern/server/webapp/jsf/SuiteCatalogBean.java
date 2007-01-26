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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.util.AuthorizationManagerFactory;
import org.genepattern.server.util.IAuthorizationManager;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

public class SuiteCatalogBean {

    private SelectItem[] states = new SelectItem[] { new SelectItem("new", "new"),
            new SelectItem("updated", "updated"), new SelectItem("up to date", "up to date") };;

    private static Logger log = Logger.getLogger(SuiteCatalogBean.class);

    private boolean error;

    private List<String> selectedStates;

    private ArrayList<MySuiteInfo> suiteCatalogSuites;

    private static final Comparator<SuiteInfo> versionComparator = new DescendingVersionComparator();

    private enum State {
        NEW, UPDATED, UPTODATE
    };

    private ArrayList<MySuiteInfo> filteredSuites;

    private Map<String, List<SuiteInfo>> baseLsid2InstalledSuites;

    private String suiteUrl;

    private HashMap<String, Map> lsidToSuiteMap;

    public SuiteCatalogBean() {

        try {
            baseLsid2InstalledSuites = createBaseLsidToSuitesMap(new LocalAdminClient(UIBeanHelper.getUserId())
                    .getAllSuites());
            suiteUrl = System.getProperty("SuiteRepositoryURL");
            lsidToSuiteMap = new SuiteRepository().getSuites(suiteUrl);
            Map<String, List<MySuiteInfo>> suiteCatalogBaseLsidToSuitesMap = new HashMap<String, List<MySuiteInfo>>();
            for (Iterator<Map> it = lsidToSuiteMap.values().iterator(); it.hasNext();) {
                MySuiteInfo si = new MySuiteInfo(it.next(), suiteUrl);
                List<MySuiteInfo> suites = suiteCatalogBaseLsidToSuitesMap.get(si.getLsid());
                if (suites == null) {
                    suites = new ArrayList<MySuiteInfo>();
                    suiteCatalogBaseLsidToSuitesMap.put(si.getLsid(), suites);
                }
                suites.add(si);
            }
            suiteCatalogSuites = new ArrayList<MySuiteInfo>(suiteCatalogBaseLsidToSuitesMap.size());
            for (Iterator<List<MySuiteInfo>> it = suiteCatalogBaseLsidToSuitesMap.values().iterator(); it.hasNext();) {
                List<MySuiteInfo> suites = it.next();
                Collections.sort(suites, versionComparator);
                MySuiteInfo s = suites.get(0);
                if (suites.size() > 1) {
                    MySuiteInfo[] later = new MySuiteInfo[suites.size() - 1];
                    for (int i = 1; i < suites.size(); i++) {
                        later[i - 1] = suites.get(i);
                    }
                    s.setLaterVersions(later);
                }
                suiteCatalogSuites.add(s);
            }
            if (UIBeanHelper.getRequest().getParameter("suiteCatalogForm:suiteCatalogSubmit") == null) {
                selectedStates = new ArrayList<String>();
                selectedStates.add("new");
                selectedStates.add("updated");
                filter();
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            error = true;
        }

    }

    public String install() {

        final String[] lsids = UIBeanHelper.getRequest().getParameterValues("installLsid");
        if (lsids != null) {
            final String username = UIBeanHelper.getUserId();
            final LocalTaskIntegratorClient taskIntegrator = new LocalTaskIntegratorClient(username);
            IAuthorizationManager authManager = AuthorizationManagerFactory.getAuthorizationManager();
            boolean suiteInstallAllowed = authManager.checkPermission("createSuite", username);
            if (!suiteInstallAllowed) {
                UIBeanHelper.setErrorMessage("You don't have the required permissions to install suites.");
                return "failure";
            }

            final SuiteInstallBean installBean = (SuiteInstallBean) UIBeanHelper.getManagedBean("#{suiteInstallBean}");
            String[] names = new String[lsids.length];
            int i = 0;
            for (String lsid : lsids) {
                Map suite = lsidToSuiteMap.get(lsid);
                names[i++] = (String) suite.get("name");
            }

            installBean.setSuites(lsids, names);

            new Thread() {
                public void run() {
                    for (String lsid : lsids) {
                        try {
                            HibernateUtil.beginTransaction();
                            taskIntegrator.installSuite(lsid);
                            HibernateUtil.commitTransaction();
                            installBean.setStatus(lsid, "success");
                        } catch (WebServiceException e) {
                            HibernateUtil.rollbackTransaction();
                            log.error(e);
                            installBean.setStatus(lsid, "error", e.getMessage());
                        }
                    }
                }
            }.start();
        }
        return "install";

    }

    private Map<String, List<SuiteInfo>> createBaseLsidToSuitesMap(SuiteInfo[] suites) {
        Map<String, List<SuiteInfo>> lsidToSuitesMap = new HashMap<String, List<SuiteInfo>>();
        if (suites != null) {
            for (SuiteInfo si : suites) {
                try {
                    String baseLsid = new LSID(si.getLSID()).toStringNoVersion();
                    List<SuiteInfo> suiteList = lsidToSuitesMap.get(baseLsid);
                    if (suiteList == null) {
                        suiteList = new ArrayList<SuiteInfo>();
                        lsidToSuitesMap.put(baseLsid, suiteList);
                    }
                    suiteList.add(si);
                } catch (MalformedURLException e) {
                    log.error(e);
                }

            }
        }
        for (Iterator<List<SuiteInfo>> it = lsidToSuitesMap.values().iterator(); it.hasNext();) {
            List<SuiteInfo> lsidSuites = it.next();
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
            return baseLsid2InstalledSuites.containsKey(new LSID(lsid).toStringNoVersion());
        } catch (MalformedURLException e) {
            log.error(e);
            return false;
        }
    }

    public boolean isNewer(String lsid) {

        try {
            LSID reposLsid = new LSID(lsid);
            List<SuiteInfo> suites = baseLsid2InstalledSuites.get(reposLsid.toStringNoVersion());
            if (suites != null) {
                SuiteInfo si = suites.get(0);
                return Integer.parseInt(reposLsid.getVersion()) > Integer.parseInt(new LSID(si.getLsid()).getVersion());
            }
        } catch (MalformedURLException e) {
            log.error(e);
        }

        return true;
    }

    public void filter() {

        boolean getNew = selectedStates.contains("new");
        boolean getUpdated = selectedStates.contains("updated");
        boolean getUpToDate = selectedStates.contains("up to date");
        if (!getNew && !getUpdated && !getUpToDate) {
            selectedStates.add("new");
            selectedStates.add("updated");
            selectedStates.add("up to date");
            getNew = true;
            getUpdated = true;
            getUpToDate = true;
        }

        filteredSuites = new ArrayList<MySuiteInfo>();

        for (MySuiteInfo si : suiteCatalogSuites) {
            String lsid = si.getLsid();
            State state;
            if (isAlreadyInstalled(lsid)) {
                if (isNewer(lsid)) {
                    state = State.UPDATED;
                } else {
                    state = State.UPTODATE;
                }
            } else {
                state = State.NEW;
            }

            if (getNew && state == State.NEW) {
                filteredSuites.add(si);
            } else if (getUpdated && state == State.UPDATED) {
                filteredSuites.add(si);
            } else if (getUpToDate && state == State.UPTODATE) {
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

    private static class DescendingVersionComparator implements Comparator<SuiteInfo> {

        public int compare(SuiteInfo t1, SuiteInfo t2) {
            try {
                return new Integer(Integer.parseInt(new LSID(t2.getLsid()).getVersion())).compareTo(Integer
                        .parseInt(new LSID(t1.getLsid()).getVersion()));
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

    public static class MySuiteInfo extends SuiteInfo {

        private MySuiteInfo[] laterVersions;

        /** URL to download suite zip */
        private String url;

        private ModuleInfo[] moduleInfo;

        public MySuiteInfo(Map hm, String suiteUrl) {
            super(hm);
            ArrayList modules = (ArrayList) hm.get("modules");
            moduleInfo = new ModuleInfo[modules.size()];
            int i = 0;
            for (Iterator iter = modules.iterator(); iter.hasNext(); i++) {
                moduleInfo[i] = new ModuleInfo();
                Map modMap = (Map) iter.next();
                try {
                    moduleInfo[i].version = new LSID((String) modMap.get("lsid")).getVersion();
                } catch (MalformedURLException e) {
                    log.error(e);
                }
                moduleInfo[i].docUrl = (String) modMap.get("docFile");
                moduleInfo[i].name = (String) modMap.get("name");

            }
            url = suiteUrl + "/" + getName() + ".zip";
        }

        public String getUrl() {
            return url;
        }

        public ModuleInfo[] getModuleInfo() {
            return moduleInfo;
        }

        public MySuiteInfo[] getLaterVersions() {
            return laterVersions;
        }

        public void setLaterVersions(MySuiteInfo[] laterVersions) {
            this.laterVersions = laterVersions;
        }

        public String getLsidVersion() {
            try {
                return new LSID(getLsid()).getVersion();
            } catch (MalformedURLException e) {
                log.error(e);
                return null;
            }
        }

    }

    public static class ModuleInfo {
        @Override
        public String toString() {
            return "version= " + version + " name = " + name + " docUrl " + docUrl;
        }

        private String version;

        private String name;

        private String docUrl;

        public ModuleInfo() {

        }

        public String getDocUrl() {
            return docUrl;
        }

        public void setDocUrl(String docUrl) {
            this.docUrl = docUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public ArrayList<MySuiteInfo> getSuites() {
        return filteredSuites;
    }

    public int getNumberOfSuites() {
        return filteredSuites != null ? filteredSuites.size() : 0;
    }
}
