/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Directory Manager - does the heavy lifting of creating and finding directories for suites, pipelines and tasks
 * 
 * @author Joshua Gould
 */
public class DirectoryManager {
    private static Logger log = Logger.getLogger(DirectoryManager.class);

    /**
     * location on server of taskLib directory where per-task support files are stored
     */
    private static String taskLibDir = null;

    /** mapping of LSIDs to taskLibDir directories */
    @SuppressWarnings("rawtypes")
    private static Hashtable htTaskLibDir = new Hashtable();
    private static Map<String,String> htSuiteLibDir = new ConcurrentHashMap<String,String>();
    
    public static void removeTaskLibDirFromCache(String lsid) {
        htTaskLibDir.remove(lsid);
    }

    /**
     * 
     * @param lsid
     * @return the value removed from the cache, or null.
     */
    public static String removeSuiteLibDirFromCache(final String lsid) {
        return htSuiteLibDir.remove(lsid);
    }

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below taskLib. 
     * Note: should involve userID in this, so that there is no conflict among same-named private tasks. 
     * Creates the directory if it doesn't already exist.
     * 
     * @param taskName, name of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception, if genepattern.properties System property not defined
     * @author Jim Lerner
     * 
     * @deprecated should pass in Hibernate session, GpConfig and GpContext
     */
    public static String getLibDir(String lsid) throws Exception, MalformedURLException {
        final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        return getLibDir(mgr, gpConfig, serverContext, lsid);
    }

    public static String getLibDir(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext serverContext, String lsid) throws Exception, MalformedURLException {
	LSID l = new LSID(lsid);
	if (l.getAuthority().equals("") || l.getIdentifier().equals("") || !l.hasVersion()) {
	    throw new MalformedURLException("invalid LSID");
	}

	if (LSIDUtil.isSuiteLSID(lsid)) {
	    final boolean alwaysMkdirs=false;
	    File suiteLibDir=getSuiteLibDir(gpConfig, serverContext, null, lsid, null, alwaysMkdirs);
	    if (suiteLibDir != null) {
	        return suiteLibDir.getAbsolutePath();
	    }
	    else {
	        log.debug("getSuiteLibDir was null, for lsid="+lsid);
	        return "";
	    }

	} else {
	    return getTaskLibDir(mgr, gpConfig, serverContext, null, lsid, null);
	}
    }

    /** @deprecated should pass in a valid Hibernate session, gpConfig and gpContext */
    public static String getTaskLibDir(final String taskName, final String sLSID, final String username) throws MalformedURLException {
        final HibernateSessionManager mgr=org.genepattern.server.database.HibernateUtil.instance();
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        return getTaskLibDir(mgr, gpConfig, serverContext, taskName, sLSID, username);
    }
    
    /**
     * Locates the directory where the a particular task's files are stored. It is one level below taskLib. 
     * Note: should involve userID in this, so that there is no conflict among same-named private tasks. 
     * Creates the directory if it doesn't already exist.
     * 
     * Warning: this method creates new DB connections, it is up to the calling method to close 
     *     the db connection if necessary.
     * 
     * @param taskName, name of task to look up
     * @return directory name on server where taskName support files are stored
     * @author Jim Lerner
     * @throws MalformedURLException, If the lsid is not properly formed.
     * @throws IllegalArgumentException, If the task name or lsid is not found in the database.
     */
    @SuppressWarnings("unchecked")
    public static String getTaskLibDir(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext gpContext, String taskName, String sLSID, String username) throws MalformedURLException {
        String ret = null;
        if (sLSID != null) {
            ret = (String) htTaskLibDir.get(sLSID);
            if (ret != null) {
                return ret;
            }
        }

        File f = null;
        getLibDir(gpConfig, gpContext);
        LSID lsid = null;
        TaskInfo taskInfo = null;

        if (sLSID != null && sLSID.length() > 0) {
            lsid = new LSID(sLSID);
            if (taskName == null || taskInfo == null) {
                // lookup task name for this LSID
                taskInfo = (new AdminDAO(mgr)).getTask(lsid.toString(), username);
                if (taskInfo == null) {
                    throw new IllegalArgumentException("can't get TaskInfo from " + lsid.toString());
                }
                if (taskInfo != null) {
                    taskName = taskInfo.getName();
                    if (username == null) {
                        username = taskInfo.getUserId();
                    }
                }
            }
        }

        if (lsid == null && taskName != null) {
            lsid = new LSID(taskName);
            // lookup task name for this LSID
            taskInfo = (new AdminDAO(mgr)).getTask(lsid.toString(), username);
            if (taskInfo == null) {
                throw new IllegalArgumentException("can't get TaskInfo from " + lsid.toString());
            }
            taskName = taskInfo.getName();
            if (username == null) {
                username = taskInfo.getUserId();
            }
        }

        String dirName = makeDirName(lsid, taskName, taskInfo);
        f = new File(taskLibDir, dirName);
        f.mkdirs();
        ret = f.getAbsolutePath();
        if (lsid != null) {
            htTaskLibDir.put(lsid, ret);
        }
        return ret;
    }

    /** deprecated should pass in a valid gpConfig and gpContext */
    public static String getTaskLibDir(final TaskInfo taskInfo) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        return getTaskLibDir(gpConfig, serverContext, taskInfo);
    }
    
    /**
     * Locates the directory where the a particular task's files are stored. It is one level below taskLib. 
     * Note: should involve userID in this, so that there is no conflict among same-named private tasks. 
     * Creates the directory if it doesn't already exist.
     * @param gpConfig
     * @param gpContext
     * @param taskInfo, must set the taskName of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception, if genepattern.properties System property not defined
     * @author Jim Lerner (Moved to DirManager from GenePatternAnalysisTask by Ted Liefeld)
     */
    public static String getTaskLibDir(final GpConfig gpConfig, final GpContext gpContext, final TaskInfo taskInfo) {
        if (taskInfo == null) {
            log.error("Unexpected null arg in DirectoryManager.getTaskLibDir");
            return "";
        }
        File f = null;
        getLibDir(gpConfig, gpContext);

        String taskName = taskInfo.getName();
        String sLsid = taskInfo.getLsid();
        LSID lsid = null;
        try {
            lsid = new LSID(sLsid);
        }
        catch (MalformedURLException e) {
            //ignore, null lsid arg handled in makeDirName
        }
        catch (Exception e2) {
        }

        String dirName = makeDirName(lsid, taskName, taskInfo);
        f = new File(taskLibDir, dirName);
        f.mkdirs();
        return f.getAbsolutePath();
    }
    
    protected static String makeDirName(LSID lsid, String taskName, TaskInfo taskInfo) {
	String dirName;
	int MAX_DIR_LENGTH = 255; // Mac OS X directory name limit
	String version;

	String invariantPart = (taskInfo != null ? ("" + taskInfo.getID()) : Integer.toString(Math.abs(taskName
		.hashCode()), 36)); // [a-z,0-9];
	if (lsid != null) {
	    // invariantPart = lsid.getAuthority() + "-" + lsid.getNamespace() +
	    // "-" + lsid.getIdentifier();
	    version = lsid.getVersion();
	    if (version.equals("")) {
		// invariantPart = "" + Math.random() + "-" + Math.random() ;
		version = "tmp";
	    }
	    // String hashBase36 =
	    // Integer.toString(Math.abs(invariantPart.hashCode()), 36); //
	    // [a-z,0-9]
	} else {
	    // try { throw new Exception("no LSID given"); } catch (Exception e)
	    // { System.out.println(e.getMessage()); e.printStackTrace(); }
	    dirName = taskName;
	    version = "1";
	}
	dirName = "." + version + "." + invariantPart; // hashBase36;
	dirName = taskName.substring(0, Math.min(MAX_DIR_LENGTH - dirName.length(), taskName.length())) + dirName;

	return dirName;
    }

    protected static String makeDirName(LSID lsid, String taskName) {
	String dirName;
	int MAX_DIR_LENGTH = 255; // Mac OS X directory name limit
	String version;
	String invariantPart = Integer.toString(Math.abs(taskName.hashCode()), 36); // [a-z,0-9];
	if (lsid != null) {
	    // invariantPart = lsid.getAuthority() + "-" + lsid.getNamespace() +
	    // "-" + lsid.getIdentifier();
	    version = lsid.getVersion();
	    if (version.equals("")) {
		// invariantPart = "" + Math.random() + "-" + Math.random() ;
		version = "tmp";
	    }
	    // String hashBase36 =
	    // Integer.toString(Math.abs(invariantPart.hashCode()), 36); //
	    // [a-z,0-9]
	} else {
	    // try { throw new Exception("no LSID given"); } catch (Exception e)
	    // { System.out.println(e.getMessage()); e.printStackTrace(); }
	    dirName = taskName;
	    version = "1";
	}
	dirName = "." + version + "." + invariantPart; // hashBase36;
	dirName = taskName.substring(0, Math.min(MAX_DIR_LENGTH - dirName.length(), taskName.length())) + dirName;

	return dirName;
    }

    public static String getLibDir(final GpConfig gpConfig, final GpContext gpContext) {
        if (taskLibDir == null) {
            log.debug("initializing taskLibDir...");
            taskLibDir = getRootTaskLibDir(gpConfig, gpContext);
            if (taskLibDir == null || !new File(taskLibDir).exists()) {
                taskLibDir = ".." + File.separator + "taskLib";
                log.debug("taskLibDir not set, setting taskLibDir="+taskLibDir);
            }
            File f = new File(taskLibDir);
            taskLibDir = f.getAbsolutePath();
            log.debug("taskLibDir="+taskLibDir);
        }
        return taskLibDir;
    }

    protected static String getRootTaskLibDir(final GpConfig gpConfig, final GpContext gpContext) {
        File tasklibDir=gpConfig.getRootTasklibDir(gpContext);
        if (tasklibDir==null) {
            return null;
        }
        return tasklibDir.getPath();
    }

    /**
     * Locates the directory where the a particular task's files are stored. It is one level below taskLib. 
     * Note: should involve userID in this, so that there is no conflict among same-named private tasks. 
     * Creates the directory if it doesn't already exist.
     * 
     * @param taskName, name of task to look up
     * @return directory name on server where taskName support files are stored
     * @throws Exception, if genepattern.properties System property not defined
     * @author Jim Lerner
     * 
     * @deprecated should pass in a valid GpConfig and GpContext
     */
    public static File getSuiteLibDir(String suiteName, String sLSID, String username) throws Exception  {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        final boolean alwaysMkdirs=false;
        return getSuiteLibDir(gpConfig, serverContext, suiteName, sLSID, username, alwaysMkdirs);
    }

    /**
     * Get the installation directory for the given SuiteInfo. First check the htSuiteLibDir cache.
     * If it's not in the cache, create a file name, and create the directory.
     * When alwaysMkdirs is true, always create the directory, even if it's already been cached.
     * 
     * @param suiteInfo
     * @param alwaysMkdirs
     * @return
     * @throws Exception
     * 
     * @deprecated should pass in a valid GpConfig and GpContext
     */
    public static File getSuiteLibDir(final SuiteInfo suiteInfo, final boolean alwaysMkdirs) throws Exception  {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext serverContext=GpContext.getServerContext();
        return getSuiteLibDir(gpConfig, serverContext, suiteInfo.getName(), suiteInfo.getLsid(), suiteInfo.getOwner(), alwaysMkdirs);
    }

    public static File getSuiteLibDir(final GpConfig gpConfig, final GpContext gpContext, final String suiteName, final String suiteLsid, final String username, final boolean alwaysMkdirs) throws Exception  {
        String ret = null;
        ret = (String) htSuiteLibDir.get(suiteLsid);
        if (ret != null) {
            final File suiteLibDir=new File(ret);
            if (alwaysMkdirs) {
                boolean success=suiteLibDir.mkdirs();
                if (success) {
                    log.debug("Created dir for cached suite, lsid="+suiteLsid+", suiteLibDir="+ret);
                }
            }
            return suiteLibDir;
        }

        getLibDir(gpConfig, gpContext);
        
        final String name;
        if (suiteName==null) {
            IAdminClient adminClient = new LocalAdminClient(username);
            SuiteInfo si = adminClient.getSuite(suiteLsid);
           name=si.getName();
        }
        else {
            name=suiteName;
        }
        // (for compatibility, pre 3.8.0, always pass 'null' into this method)
        final String dirName = makeDirName(null, name);
        final File suiteLibDir = new File(taskLibDir, dirName);
        suiteLibDir.mkdirs();
        ret = suiteLibDir.getAbsolutePath();
        htSuiteLibDir.put(suiteLsid, ret);
        return suiteLibDir;
    }

}
