package org.genepattern.server.executor.drm;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.genepattern.server.config.Value;
import org.genepattern.server.executor.CommandProperties;

public class JobExecutorUtil {
    private static final Logger log = Logger.getLogger(JobExecutorUtil.class);

    public static class CustomClassLoader extends URLClassLoader {
        private static final Logger log = Logger.getLogger(CustomClassLoader.class);
        public CustomClassLoader(URL[] urls) {
            super(urls);
        }
        
        protected String findLibrary(String libName) {
            log.debug("findLibrary, libName="+libName);
            if ( libName.contains("drmaa") ) {
                return "/broad/uge/research/lib/lx-amd64/libdrmaa.so";
            }
            else {
                return super.findLibrary(libName);
            }
        }
    }

    /**
     * Optionally set the 'jobRunnerClasspath' to add resources to the classpath 
     * before loading the class. For example, the UGER library requires a site-specific 
     * drmaa.jar file on the classpath.
     * Example:
     *     jobRunnerClasspath: [ "/broad/uge/research/lib/drmaa.jar" ] 
     */
    public static final String PROP_JOB_RUNNER_CLASSPATH="jobRunnerClasspath";
    
    /**
     * Optionally set the 'jobRunnerLibraries' to a list library files to be loaded
     * before initializing the jobRunner. For each file,  
     *     System.load( file );
     * 
     */
    public static final String PROP_JOB_RUNNER_LD_LIBRARY_NAMES="jobRunnerLibraries";
    
    
    protected static ClassLoader initClassLoader(final CommandProperties properties) {
        final Value classpath=properties.get(PROP_JOB_RUNNER_CLASSPATH);
        if (classpath != null) {
            try {
                return initCustomClassLoader( classpath.getValues() );
            }
            catch (Exception e) {
                log.error("Error initializing custom ClassLoader from "+PROP_JOB_RUNNER_CLASSPATH+"="+classpath, e);
            }
        }
        
        // fallback to default
        return Thread.currentThread().getContextClassLoader();
    }
    
    protected static ClassLoader initCustomClassLoader(final List<String> classpaths) throws IOException, ClassNotFoundException {
        if (classpaths==null || classpaths.size()==0) {
            return Thread.currentThread().getContextClassLoader();
        }
        
        final URL[] urls=new URL[ classpaths.size() ];
        int i=0;
        for(final String classpath : classpaths) {
            log.info("adding '"+classpath+"' to classpath");
            urls[i++]=new File(classpath).toURI().toURL();
        }
        log.info("creating new classloader ...");
        final ClassLoader urlCl = new CustomClassLoader( urls ); 
        
        for(final String classpath : classpaths) {
            final JarFile jarFile = new JarFile(classpath);
            loadClassesFromJar(jarFile, urlCl);
        }
        return urlCl;
    }
    
    protected static void loadClassesFromJar(final JarFile jarFile, final ClassLoader cl) throws IOException, ClassNotFoundException {
        final Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            final JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")){
                continue;
            }
            // -6 because of .class
            String classname = je.getName().substring(0,je.getName().length()-6);
            classname = classname.replace('/', '.');
            log.info("initializing class="+classname);
            Class.forName(classname, true, cl);
        }
    }
    

}
