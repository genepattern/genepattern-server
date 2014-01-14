/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.process.InstallSuite;
import org.genepattern.server.process.InstallTask;
import org.genepattern.server.process.SuiteRepository;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.WebServiceException;

public class SuiteCatalogBean {

    private final String NEW_TEXT = "Search for new suites to install";

    private final String UPDATED_TEXT = "Search for updates of the currently installed suites";

    private final String UP_TO_DATE = "Search for up to date suites";

    private MySelectItem[] states = new MySelectItem[] { new MySelectItem(InstallTask.NEW, NEW_TEXT),
            new MySelectItem(InstallTask.UPDATED, UPDATED_TEXT), new MySelectItem(InstallTask.UPTODATE, UP_TO_DATE) };

    private static Logger log = Logger.getLogger(SuiteCatalogBean.class);

    private boolean error;

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
        if (!AuthorizationHelper.createPublicSuite() || !AuthorizationHelper.createModule()) {
            throw new SecurityException();
        }
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
            String[] requestedStates = UIBeanHelper.getRequest().getParameterValues("state");
            if (requestedStates == null || requestedStates.length == 0) {
                requestedStates = getDefaultStates();
            }
            updateSelectedItems(requestedStates, states);

            if (UIBeanHelper.getRequest().getParameter("suiteCatalogForm:suiteCatalogSubmit") == null) {
                filter();
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e);
            error = true;
        }

    }

    private static List<String> getSelection(MySelectItem[] items) {
        List<String> selection = new ArrayList<String>();
        for (MySelectItem i : items) {
            if (i.isSelected()) {
                selection.add(i.getValue().toString());
            }
        }
        return selection;
    }

    private static void updateSelectedItems(String[] request, MySelectItem[] selectItems) {
        Set<String> set = new HashSet<String>(Arrays.asList(request));
        for (MySelectItem i : selectItems) {
            i.setSelected(set.contains(i.getValue()));
        }
    }

    private String[] getDefaultStates() {
        List<String> l = new ArrayList<String>();
        l.add(InstallTask.NEW);
        l.add(InstallTask.UPDATED);
        return l.toArray(new String[0]);
    }

    public String install() {

        final String[] lsids = UIBeanHelper.getRequest().getParameterValues("installLsid");
        if (lsids != null) {
            final String username = UIBeanHelper.getUserId();
            final InstallSuite s = new InstallSuite(username);
            boolean suiteInstallAllowed = AuthorizationHelper.createSuite();
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
                            s.install(lsid);
                            HibernateUtil.commitTransaction();
                            installBean.setStatus(lsid, "success");
                        } catch (WebServiceException e) {
                            HibernateUtil.rollbackTransaction();
                            log.error(e);
                            installBean.setStatus(lsid, "error", e.getMessage());
                        } catch (TaskInstallationException tie) {
                            log.error(tie);
                            installBean.setStatus(lsid, "warning", tie.getWarningMessage());
                            HibernateUtil.commitTransaction();
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

    public boolean isNewer(final String reposLsidStr) {
        log.debug("reposLsidStr="+reposLsidStr);
        try {
            final LSID reposLsid = new LSID(reposLsidStr);
            if (!reposLsid.hasVersion()) {
                //it's newer
                log.error("reposLsid has no version, return true, reposLsidStr="+reposLsidStr);
                return true;
            }
            final List<SuiteInfo> suites = baseLsid2InstalledSuites.get(reposLsid.toStringNoVersion());
            if (suites != null && suites.size()>0) {
                final SuiteInfo si = suites.get(0);
                final String installedLsidStr=si.getLsid();
                log.debug("installedLsidStr="+installedLsidStr);
                final LSID installedLsid=new LSID(installedLsidStr);
                if (!installedLsid.hasVersion()) {
                    //the installed version has no LSID
                    log.error("installedLsid has no version, return false, installedLsidStr="+installedLsidStr);
                    return false;
                }
                
                Integer reposVersion=Integer.parseInt(reposLsid.getVersion().trim());
                Integer installedVersion=Integer.parseInt(installedLsid.getVersion().trim());
                return reposVersion > installedVersion;
            }
        } 
        catch (NumberFormatException e) {
            log.error("Unexpected number format exception, reposLsidStr="+reposLsidStr, e);
            return false;
        }
        catch (MalformedURLException e) {
            log.error("Error with reposLsidStr="+reposLsidStr, e);
            return false;
        }
        return true;
    }

    public void filter() {
        List<String> selection = getSelection(states);
        boolean getNew = selection.contains(InstallTask.NEW);
        boolean getUpdated = selection.contains(InstallTask.UPDATED);
        boolean getUpToDate = selection.contains(InstallTask.UPTODATE);

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

    public MySelectItem[] getStates() {
        return this.states;
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

    public static class MySelectItem extends SelectItem {

        public MySelectItem(String value, String label) {
            super(value, label);

        }

        public MySelectItem(String value) {
            super(value);
        }

        private boolean selected;

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
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
            String catEnv = "prod";
            if (suiteUrl.indexOf("env=")>0){
            	int idx = suiteUrl.indexOf("env=");
            	catEnv=suiteUrl.substring(idx+4);
            }
            
            try {
				LSID suiteLsid = new LSID(getLsid());
				String auth = suiteLsid.getAuthority();
				String ns = suiteLsid.getNamespace();;
				String id = suiteLsid.getIdentifier();
				String ver = suiteLsid.getVersion();
				String name = getName();
				String filePath = "/"+ name + "/" + auth+":"+ns+"/"+id+"/"+ver+"/";
				url = suiteUrl + "/"+ catEnv + "/download/?file=" +filePath + name + ".zip";
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				 url = suiteUrl + "/"+catEnv + "/download/?file=" + getName() + ".zip";
			}
            
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
