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


package org.genepattern.server.process;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.config.ServerProperties;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.repository.RepositoryInfo;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;

public class InstallTasksCollectionUtils {

    protected InstallTask[] unfilteredTasks = new InstallTask[0];
    protected InstallTask[] filteredTasks = new InstallTask[0];
    final protected String userID;
    final protected URL repositoryURL;
    final protected ModuleRepository repos;
    boolean initialInstall = false;

    public InstallTasksCollectionUtils(String userID, boolean initialInstall) {
        this.userID = userID;
        this.initialInstall = initialInstall;
        final Context userContext=ServerConfiguration.Context.getContextForUser(userID);
        final RepositoryInfo repositoryInfo=RepositoryInfo.getRepositoryInfoLoader(userContext).getCurrentRepository();
        this.repositoryURL = repositoryInfo.getUrl();
        repos = new ModuleRepository(repositoryURL);
    }

    // get a list of all modules available for download
    public InstallTask[] getAvailableModules() throws Exception {
        //String repositoryURL = System.getProperty("ModuleRepositoryURL");
        String repositoryUrlQuery=repositoryURL.toExternalForm();
        boolean notFirstParam = (repositoryUrlQuery.indexOf("?") > 0);       
        String paramPrefix = notFirstParam? "&" :"?";
        if (initialInstall) {
            repositoryUrlQuery = repositoryURL.toExternalForm() + paramPrefix + "initialInstall=1&GenePatternVersion=" + ServerProperties.instance().getProperty("GenePatternVersion");
        } 
        else {
            repositoryUrlQuery = repositoryURL.toExternalForm() + paramPrefix + "GenePatternVersion=" + ServerProperties.instance().getProperty("GenePatternVersion");
        }


        Vector modules = new Vector();
        modules.addAll(Arrays.asList(repos.parse(repositoryUrlQuery)));
        // weed out any bad LSIDs right away
        InstallTask task = null;
        for (ListIterator itModule = modules.listIterator(); itModule.hasNext();) {
            try {
                task = (InstallTask) itModule.next();
                task.setInitialInstall(initialInstall);
                Map attributes = task.getAttributes();
                LSID lsid = new LSID((String) attributes.get(GPConstants.LSID));
            } 
            catch (MalformedURLException mue) {
                System.err.println("InstallTasksCollectionUtils: skipping "
                        + task.getName() + ": " + mue.getMessage());
                itModule.remove();
            }
        }

        unfilteredTasks = (InstallTask[]) modules.toArray(new InstallTask[0]);
        filteredTasks = unfilteredTasks;
        return unfilteredTasks;
    }

    protected void setAvailableModules(InstallTask[] modules) {
        unfilteredTasks = modules;
        filteredTasks = modules;
    }

    // reduce the list of modules, matching only those with matching attributes
    public InstallTask[] filterTasks(Map attributeNameValuePairs) {
        Vector vTasks = new Vector();
        for (int t = 0; t < unfilteredTasks.length; t++) {
            if (unfilteredTasks[t].matchesAttributes(attributeNameValuePairs)) {
                vTasks.add(unfilteredTasks[t]);
            }
        }
        filteredTasks = (InstallTask[]) vTasks.toArray(new InstallTask[0]);
        return filteredTasks;
    }

    // sort the list according to a particular attribute, in either ascending or
    // descending order
    public InstallTask[] sortTasks(String attributeName, boolean ascending) {
        Arrays.sort(filteredTasks, new AttributeComparator(attributeName, ascending));
        return filteredTasks;
    }

    // return an array of install return values (installed, overwrote, failed),
    // one per module
    public Vector install(InstallTask[] tasks, int access_id) {
        Vector returnValues = new Vector();
        for (int t = 0; t < tasks.length; t++) {
            try {
                returnValues.add((tasks[t].install(userID, access_id, null) ? "installed" : "overwrote") + " " + tasks[t].getName());
            } 
            catch (TaskInstallationException tie) {
                returnValues.addAll(tie.getErrors());
            }
        }
        return returnValues;
    }

    // update installation for all installed modules only
    public String[] refreshInstalledModules() {
        Vector vModules = new Vector();
        for (int i = 0; i < unfilteredTasks.length; i++) {
            if (unfilteredTasks[i].isAlreadyInstalled() && unfilteredTasks[i].isNewer()) {
                //System.out.println("will refresh " + unfilteredTasks[i].getName());
                vModules.add(unfilteredTasks[i]);
            }
        }
        InstallTask[] obsoleteTasks = (InstallTask[]) vModules.toArray(new InstallTask[0]);
        Vector vProblems = install(obsoleteTasks, GPConstants.ACCESS_PUBLIC);
        return (String[]) vProblems.toArray(new String[0]);
    }

    // return a sorted list of unique values for a particular attribute
    public String[] getUniqueValues(String attributeName) {
        TreeSet tsValues = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < unfilteredTasks.length; i++) {
            String val = (String)unfilteredTasks[i].getAttributes().get(attributeName);
            if ((val == null) || val.length() == 0) val = GPConstants.ANY; // blanks treated as any
            tsValues.add(val);
        }
        return (String[]) tsValues.toArray(new String[0]);
    }

    public String[] getAttributeNames() {
        return InstallTask.getAttributeNames();
    }

    public String getMOTD_message() {
        return repos.getMOTD_message();
    }

    public String getMOTD_url() {
        return repos.getMOTD_url();
    }

    public int getMOTD_urgency() {
        return repos.getMOTD_urgency();
    }

    public Date getMOTD_timestamp() {
        return repos.getMOTD_timestamp();
    }

    public String getMOTD_latestServerVersion() {
        return repos.getMOTD_latestServerVersion();
    }

    public static void main(String[] args) {
        args = new String[] { "KNN", "KScore", "NMF" };
        try {
            System.out.println(test(args));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        System.exit(0);
    }

    public static String test(String[] args) throws Exception {
        StringBuffer out = new StringBuffer();
        String userID = "GenePattern";
        InstallTasksCollectionUtils collection = new InstallTasksCollectionUtils(
                userID, false);

        Vector vModules = new Vector();
        for (int arg = 0; arg < args.length; arg++) {
            vModules.add(InstallTask.loadFromRepositoryAndZipFile(args[arg],
                    userID));
        }
        collection.setAvailableModules((InstallTask[]) vModules
                .toArray(new InstallTask[0]));
        out.append("availableModules: " + collection.toString() + "\n");

        // get just the Java-based prediction algorithms
        HashMap match = new HashMap();
        match.put(GPConstants.TASK_TYPE, "Prediction");
        match.put(GPConstants.LANGUAGE, "Java");
        collection.filterTasks(match);
        out.append("after filtering on Java/Prediction: "
                + collection.toString() + "\n");

        // sort by task name in inverse alphabetical order
        collection.sortTasks(GPConstants.NAME, false);
        out.append("after sorting on name, descending order: "
                + collection.toString() + "\n");

        out.append("done\n");

        return out.toString();
    }

    // set up modules for testing while waiting for Michael's XML-based support
    public void setupTestCollection(String[] modules) throws Exception {
        Vector vModules = new Vector(modules.length);
        for (int module = 0; module < modules.length; module++) {
            vModules.add(InstallTask.loadFromRepositoryAndZipFile(
                    modules[module], userID));
        }
        setAvailableModules((InstallTask[]) vModules
                .toArray(new InstallTask[0]));
    }

    // set up modules for testing while waiting for Michael's XML-based support
    public void setupTestCollection() throws Exception {
        setupTestCollection(new String[] { "KNN", "GeneNeighbors", "NMF",
                "ClassNeighbors", "TransposeDataset" });
    }

    public String toString() {
        StringBuffer out = new StringBuffer();
        int i;
        out.append("unfiltered tasks: ");
        for (i = 0; i < unfilteredTasks.length; i++) {
            if (i > 0)
                out.append(", ");
            out.append(unfilteredTasks[i].getName());
        }
        out.append("\n");

        out.append("filtered tasks: ");
        for (i = 0; i < filteredTasks.length; i++) {
            if (i > 0)
                out.append(", ");
            out.append(filteredTasks[i].getName());
        }
        out.append("\n");
        return out.toString();
    }
}

class AttributeComparator implements Comparator {

    String sortKey;

    boolean ascending;

    public AttributeComparator(String sortKey, boolean ascending) {
        this.sortKey = sortKey;
        this.ascending = ascending;
    }

    public int compare(Object o1, Object o2) {
        String v1 = (String) ((InstallTask) o1).getAttributes().get(sortKey);
        String v2 = (String) ((InstallTask) o2).getAttributes().get(sortKey);
        if (sortKey.equals(InstallTask.REFRESHABLE)) {
            v1 = "" + InstallTask.vRefreshable.indexOf(v1);
            v2 = "" + InstallTask.vRefreshable.indexOf(v2);
        } else if (sortKey.equals(InstallTask.STATE)) {
            v1 = "" + InstallTask.vStates.indexOf(v1);
            v2 = "" + InstallTask.vStates.indexOf(v2);
        }
        int r = v1.compareToIgnoreCase(v2);
        if (!ascending)
            r = -r;
        return r;
    }
}
