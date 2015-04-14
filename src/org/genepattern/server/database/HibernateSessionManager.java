package org.genepattern.server.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Entity;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;

import com.google.common.reflect.ClassPath;

/**
 * refactored from HibernateUtil class, to avoid reliance on top level static initializers.
 * @author pcarr
 *
 */
public final class HibernateSessionManager {
    private static final Logger log = Logger.getLogger(HibernateSessionManager.class);

    /**
     * Set the 'database.annotation_scanner' property in the database_default.properties or database_custom.properties file
     * to overwrite the default method for loading JPA Annotated Entity classes.
     * Valid values are:
     *     'hard-coded', use the build-in values compiled into GenePattern jar file
     *     'reflections', use the reflections package to scan for Entity annotated classes
     *     'guava', use Google guava package to scan for Entity annotated classes
     * added while debugging GP-5626. 
     */
    public static final String PROP_DB_ANNOTATION_SCANNER="database.annotation_scanner";
    public static final String DEFAULT_DB_ANNOTATION_SCANNER="reflections"; //"hard-coded";
    private static final String packagePrefix="org.genepattern.server";

    private final SessionFactory sessionFactory;
    public HibernateSessionManager(String hibernateConfigurationFile, String connectionUrl) {
        if (log.isDebugEnabled()) {
            log.debug("initializing session factory from configFile="+hibernateConfigurationFile+", connectionUrl="+connectionUrl);
        }
        sessionFactory = createSessionFactory(hibernateConfigurationFile, connectionUrl);
    }
    
    public HibernateSessionManager(Properties hibernateProperties) {
        if (log.isDebugEnabled()) {
            log.debug("initializing session factory from hibernate properties="+hibernateProperties);
        }
        sessionFactory = createSessionFactory(hibernateProperties);
    }

    /**
     * Hard-coded list of hbm.xml files for the project.
     * This used to be set in the hibernate.cfg.xml file.
     * 
     * @return
     */
    protected final static String[] hbmXmls() {
        return new String[]{
                "org/genepattern/server/domain/AnalysisJob.hbm.xml", 
                "org/genepattern/server/domain/BatchJob.hbm.xml", 
                "org/genepattern/server/domain/JobStatus.hbm.xml", 
                "org/genepattern/server/domain/Lsid.hbm.xml", 
                "org/genepattern/server/domain/Sequence.hbm.xml", 
                "org/genepattern/server/domain/Suite.hbm.xml", 
                "org/genepattern/server/domain/TaskAccess.hbm.xml", 
                "org/genepattern/server/domain/TaskMaster.hbm.xml", 
                "org/genepattern/server/domain/GsAccount.hbm.xml", 
                "org/genepattern/server/domain/PinModule.hbm.xml", 
                "org/genepattern/server/message/SystemMessage.hbm.xml", 
                "org/genepattern/server/user/JobCompletionEvent.hbm.xml", 
                "org/genepattern/server/user/User.hbm.xml",
                "org/genepattern/server/auth/JobGroup.hbm.xml", 
                "org/genepattern/server/executor/sge/JobSge.hbm.xml"  
        };
    }

    /**
     * Hard-coded list of Hibernate mapping annotated classes.
     * To generate this list, try 
     *     find . -name "*.java" -exec grep -l "@Entity" {} \; | xargs ls -ltrhg
     * @return
     */
    protected static List<Class<?>> hardCodedAnnotatedClasses() {
        return Arrays.<Class<?>>asList( 
                org.genepattern.server.dm.congestion.Congestion.class,
                org.genepattern.server.dm.userupload.dao.UserUpload.class,
                org.genepattern.server.dm.jobinput.JobInput.class,
                org.genepattern.server.dm.jobinput.JobInputAttribute.class,
                org.genepattern.server.dm.jobresult.JobResult.class,
                org.genepattern.server.jobqueue.JobQueue.class,
                org.genepattern.server.eula.dao.EulaRecord.class,
                org.genepattern.server.eula.dao.EulaRemoteQueue.class,
                org.genepattern.server.taskinstall.dao.TaskInstall.class,
                org.genepattern.server.task.category.dao.TaskCategory.class,
                org.genepattern.server.executor.drm.dao.JobRunnerJob.class,
                org.genepattern.server.job.input.dao.JobInputValue.class,
                org.genepattern.server.job.output.JobOutputFile.class,
                org.genepattern.server.job.comment.JobComment.class,
                org.genepattern.server.tag.Tag.class,
                org.genepattern.server.job.tag.JobTag.class,
                org.genepattern.server.plugin.PatchInfo.class,
                org.genepattern.server.domain.PropsTable.class,
                org.genepattern.server.user.UserProp.class
                );
    }

    protected static AnnotationConfiguration preInitAnnotationConfiguration(final String databaseAnnotationScanner) {
        if (log.isDebugEnabled()) {
            log.debug("preparing hibernate annotation configuration ...");
        }
        AnnotationConfiguration config = new AnnotationConfiguration();

        // add mappings from xml files here, instead of in the .xml file
        if (log.isDebugEnabled()) {
            log.debug("add mapping from xml files ...");
        }
        for (final String hbmXml : hbmXmls()) {
            if (log.isDebugEnabled()) {
                log.debug("\thbmXml="+hbmXml);
            }
            config.addResource(hbmXml);
        }
        
        Collection<Class<?>> annotatedClasses=initAnnotatedClasses(databaseAnnotationScanner);
        if (log.isDebugEnabled()) {
            log.debug("found " + annotatedClasses.size() + " annotated classes");
        }
        for(Class<?> clazz : annotatedClasses) {
            if (log.isDebugEnabled()) {
                log.debug("\tannotated class="+clazz);
            }
            config.addAnnotatedClass( clazz );
        }
        return config;
    }
    
    protected static Collection<Class<?>> initAnnotatedClasses(String databaseAnnotationScanner) {
        if (databaseAnnotationScanner == null || databaseAnnotationScanner.isEmpty()) {
            log.debug(""+PROP_DB_ANNOTATION_SCANNER+" not set, using default value="+DEFAULT_DB_ANNOTATION_SCANNER);
            databaseAnnotationScanner=DEFAULT_DB_ANNOTATION_SCANNER;
        } 
        else {
            log.debug(PROP_DB_ANNOTATION_SCANNER+"="+databaseAnnotationScanner);
        }
        try {
            if ("hard-coded".equalsIgnoreCase(databaseAnnotationScanner)) {
                log.info("initializing hard-coded Entity Annotations ...");
                return hardCodedAnnotatedClasses();
            }
            else if ("reflections".equalsIgnoreCase(databaseAnnotationScanner)) {
                log.info("scanning for Entity Annotations with reflections package ...");
                return initAnnotatedClasses_Reflections();
            }
            else if ("guava".equalsIgnoreCase(databaseAnnotationScanner)) {
                log.info("scanning for Entity Annotations with guava package ...");
                return initAnnotatedClasses_Guava();
            }
            else {
                log.error("Invalid value for "+PROP_DB_ANNOTATION_SCANNER+"="+databaseAnnotationScanner);
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        log.error("");
        return hardCodedAnnotatedClasses();
    }
    
    /**
     * Scan the current class loader for all classes with the given list of package prefixes.
     * Return a list of Classes which have the JPA Entity annotation.
     */
    protected static List<Class<?>> initAnnotatedClasses_Guava() throws IOException, ClassNotFoundException {
        if (log.isDebugEnabled()) {
            log.debug("scanning classpath for Entity annotated classes...");
        }
        ClassLoader cl=UserUpload.class.getClassLoader();
        if (cl==null) {
            log.error("UserUpload.class.getClassLoader returned null, trying system class loader");
            cl=ClassLoader.getSystemClassLoader();
        }
        if (cl==null) {
            log.error("ClassLoader.getSystemClassLoader() returned null");
        }
        final ClassPath classPath=ClassPath.from(cl);
        Set<ClassPath.ClassInfo> set=new HashSet<ClassPath.ClassInfo>();
        Set<ClassPath.ClassInfo> classInfos=classPath.getTopLevelClassesRecursive(packagePrefix);
        for(final ClassPath.ClassInfo classInfo : classInfos) {
            if (classInfo.getName().startsWith(packagePrefix)) {
                set.add(classInfo);
            }
            else {
                log.error("Unexpected result: "+classInfo);
            }
        }
        List<Class<?>> list=new ArrayList<Class<?>>();
        for(ClassPath.ClassInfo ci : set) {
            try {
                Class<?> clazz=Class.forName(ci.getName(), false, cl);
                if (clazz.isAnnotationPresent(Entity.class)) {
                    list.add(clazz);
                }
            }
            catch (Throwable t) {
                log.error(t);
            }
        }
        return list;
    }
    
    protected static Set<Class<?>> initAnnotatedClasses_Reflections() {
        if (log.isDebugEnabled()) {
            log.debug("scanning classpath for Entity annotated classes...");
        }
        //scan urls that contain the packagePrefix, include inputs starting with the packagePrefix, use the default scanners
        Reflections reflections = new Reflections(packagePrefix);
        Set<Class<?>> annotatedClasses=reflections.getTypesAnnotatedWith(Entity.class);
        return annotatedClasses;
    }

    /**
     * Initialize the hibernate connection based on the values set in the given Properties arg.
     * Default values are loaded from the ./resources/hibernate_default.properties file.
     * 
     * @param hibernateProperties
     * @return
     */
    public static SessionFactory createSessionFactory(Properties hibernateProperties) {
        String databaseAnnotationScanner=hibernateProperties.getProperty(PROP_DB_ANNOTATION_SCANNER, DEFAULT_DB_ANNOTATION_SCANNER);
        AnnotationConfiguration config = preInitAnnotationConfiguration(databaseAnnotationScanner);
        config.addProperties(hibernateProperties);
        mergeSystemProperties(config);
        log.info("hibernate.connection.url="+config.getProperty("hibernate.connection.url"));
        if (log.isDebugEnabled()) {
            log.debug("building session factory ...");
        }
        return config.buildSessionFactory();
    }

    /**
     * @deprecated, should initialize from the a Properties object instead.
     * 
     * @param configResource
     * @param connectionUrl
     * @return
     */
    public static SessionFactory createSessionFactory(String configResource, final String connectionUrl) {
        AnnotationConfiguration config = preInitAnnotationConfiguration(DEFAULT_DB_ANNOTATION_SCANNER);
        config.configure(configResource);
        mergeSystemProperties(config);
        if (connectionUrl != null) {
            config.setProperty("hibernate.connection.url", connectionUrl);
        }        
        log.info("hibernate.connection.url="+config.getProperty("hibernate.connection.url"));
        return config.buildSessionFactory();
    }

    private static void mergeSystemProperties(Configuration config) {
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            String name = (String) key;
            if (name.startsWith("hibernate.")) {
                config.setProperty(name, (String) props.get(name));
            }
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public Session getSession() {
        if (sessionFactory != null) {
            return sessionFactory.getCurrentSession();
        }
        throw new ExceptionInInitializerError("Hibernate session factory is not initialized");
    }

    /**
     * Close the current session, if open.
     * 
     */
    public void closeCurrentSession() {
        Session session = getSession();
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    /**
     * If the current session has an open transaction commit it and close the current session, otherwise do nothing.
     */
    public void commitTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            tx.commit();
            closeCurrentSession();
        }
    }

    /**
     * If the current session has an open transaction roll it back and close the current session, otherwise do nothing.
     * 
     */
    public void rollbackTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (tx.isActive()) {
            tx.rollback();
            closeCurrentSession();
        }
    }

    /**
     * Begin a new transaction. If a transaction is in progress do nothing.
     * 
     * @return
     */
    public void beginTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            session.beginTransaction();
        }
    }

    public boolean isInTransaction() {
        Session session = getSession();
        Transaction tx = session.getTransaction();
        if (!tx.isActive()) {
            return false;
        }
        return true;
    }

    /**
     * Close the current session, if open.
     * 
     */
    public static void closeCurrentSession(SessionFactory sessionFactory) {
        Session session = sessionFactory.getCurrentSession();
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}