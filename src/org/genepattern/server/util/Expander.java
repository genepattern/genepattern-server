package org.genepattern.server.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Expand;
import org.genepattern.server.config.GpConfig;


/**
 * The Expander uses ant's unzip instead of Java's to preserve file permissions
 *
 * We add the extra unzip method to handle use on windows
 */
public class Expander extends Expand {
    private static Logger log = Logger.getLogger(Expander.class);
    
    public Expander() {
        project = new Project();
        project.init();
        taskType = "unzip";
        taskName = "unzip";
        target = new Target();
    }
    
    
    public static void unzip(File zipFile, String destinationDir){
        // unzip using ants classes to allow file permissions to be retained
        boolean useAntUnzip = true;
        if (!GpConfig.getJavaProperty("os.name").toLowerCase().startsWith("windows")) {
            useAntUnzip = false;
            Execute execute = new Execute();
            execute.setCommandline(new String[] { "unzip", "-qq", zipFile.getAbsolutePath(), "-d", destinationDir });
            try {
                int result = execute.execute();
                if (result != 0) {
                    useAntUnzip = true;
                }
            } 
            catch (IOException ioe) {
                log.error(ioe);
                useAntUnzip = true;
            }
        }
        if (useAntUnzip) {
            Expander expander = new Expander();
            expander.setSrc(new File(zipFile.getAbsolutePath()));
            expander.setDest(new File(destinationDir));
            expander.execute();
        }

    }
    
    
 }