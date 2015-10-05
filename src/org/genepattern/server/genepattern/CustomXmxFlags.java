/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Helper class to set the java '-Xmx' arg based on the optional job configuration flags:
 *     job.memory,
 *     job.javaXmx
 *     job.javaXmxMin
 * 
 * This is called after all other command line substitutions. It replaces all
 * occurrences of -Xmx<originalValue> with -Xmx<customValue> if a custom value has been determined.
 * 
 * Note: this is only applied to java modules. Determined by the command line starting with the '<java>'
 * substitution parameter; as well as special cases for RJava wrappers.
 * 
 * @see {@link JobRunner#PROP_MEMORY}, @see {@link JobRunner#PROP_JAVA_XMX}, @see {@link JobRunner#PROP_JAVA_XMX_MIN}
 * @author pcarr
 *
 */
public class CustomXmxFlags {
    private static final Logger log = Logger.getLogger(CustomXmxFlags.class);

    public static final String XMX="-Xmx";

    public static String replaceXmx(final Memory mem, final String arg) {
        final String XMX="-Xmx";
        if (arg==null) {
            return arg;
        }
        final int i0=arg.indexOf(XMX);
        if (i0<0) {
            //no match
            return arg;
        }
        String xmxVal="";
        String tail="";
        final int i1=arg.indexOf(" ", i0);
        if (i1>=0) {
            xmxVal=arg.substring(i0+XMX.length(),i1);
            tail=arg.substring(i1);
        }
        else {
            xmxVal=arg.substring(i0+XMX.length());
        }
        try {
            Memory memOrig=Memory.fromString(xmxVal);
            if (memOrig==null) {
                return arg;
            }
        }
        catch (Throwable t) {
            //ignore
            return arg;
        }
        return arg.substring(0, i0) + XMX+mem.toXmx() + tail;
    }
    
    /**
     * Get the '-Xmx' memory value from the config.
     * Use 'job.memory', unless 'job.javaXmx' is set.
     * The returned value will be >= 'job.javaXmxMin' when set;
     * 
     * @param gpConfig
     * @param jobContext
     * @return
     */
    public static Memory getXmxValueFromConfig(final GpConfig gpConfig, final GpContext jobContext) {
        final Memory jobMemory=gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_MEMORY);
        final Memory javaXmx=gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX);
        final Memory javaXmxMin=gpConfig.getGPMemoryProperty(jobContext, JobRunner.PROP_JAVA_XMX_MIN);
        
        final Memory memoryFlag;
        if (javaXmx != null) {
            memoryFlag=Memory.max(javaXmx, javaXmxMin);
        }
        else {
            memoryFlag=Memory.max(jobMemory, javaXmxMin);
        }
        return memoryFlag;
    }
    
    /**
     * Adjust the command line args, add or edit the -Xmx arg if it is a java module.
     * 
     * 
     * When 'job.javaXmxMin' is set, make sure that there is a -Xmx arg and that it is >= job.javaXmxMin;
     * 
     * @param gpConfig
     * @param jobContext
     * @param the initial cmdLineArgs
     * @return
     */
    /**
     * Adjust the command line args, add or edit the -Xmx arg if it is a java module.
     * By default it is based on 'job.memory';
     * When 'job.javaXmx' is set, it takes precedence;
     * In either case 'job.javaXmxMin' is 
     * 
     * @param gpConfig
     * @param jobContext
     * @param cmdLineArgsIn, the initial argument list
     * @return a new arg list with additional or edited -Xmx flag, or the original one if no change was made
     */
    public static String[] addOrReplaceXmxFlag(final GpConfig gpConfig, final GpContext jobContext, final String[] cmdLineArgsIn) {
        final Memory memoryFlag=getXmxValueFromConfig(gpConfig, jobContext);
        if (memoryFlag!=null) {
            if (log.isDebugEnabled()) {
                ///CLOVER:OFF
                log.debug("setting javaXmx flag: "+memoryFlag);
                ///CLOVER:ON
            }
            return CustomXmxFlags.addOrReplaceXmxFlag(jobContext, memoryFlag, cmdLineArgsIn);
        }
        return cmdLineArgsIn;
    }

    public static String[] addOrReplaceXmxFlag(final GpContext jobContext, final Memory mem, final String[] cmdLineArgs) {
        if (mem==null) {
            log.debug("mem==null");
            return cmdLineArgs;
        }
        if (cmdLineArgs==null || cmdLineArgs.length==0) {
            log.debug("cmdLineArgs==null || length is zero");
            //ignore
            return cmdLineArgs;
        } 
        if (!isJavaCmd(jobContext)) {
            return cmdLineArgs;
        }
        
        //case 1: replace existing -Xmx flag
        boolean hasXmx=false;
        int idx=0;
        final int idxOfJavaCmd=Arrays.asList(cmdLineArgs).indexOf("java");
        for(final String arg : cmdLineArgs) {
            if (arg.startsWith("-Xmx")) {
                cmdLineArgs[idx]=replaceXmx(mem,arg);
                hasXmx=true;
                break;
            }
            ++idx;
        }
        if (hasXmx) {
            return cmdLineArgs;
        }
        else  {
            String[] rval=new String[ 1+cmdLineArgs.length ];
            int i=0;
            for(; i<=idxOfJavaCmd; ++i) {
                rval[i]=cmdLineArgs[i];
            }
            //++i;
            rval[i]="-Xmx"+mem.toXmx();
            for(; i<cmdLineArgs.length; ++i) {
                rval[i+1]=cmdLineArgs[i];
            }
            return rval;
        }
    }
    
    public static boolean isJavaCmd(final GpContext jobContext) {
        if (jobContext==null) {
            return false;
        }
        return isJavaCmd(jobContext.getTaskInfo());
    }
    
    public static boolean isJavaCmd(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.warn("taskInfo==null");
            return false;
        }
        if (taskInfo.isPipeline()) {
            return false;
        }
        final TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
        if (taskInfoAttributes==null) {
            log.warn("taskInfo.taskInfoAttributes==null");
            return false;
        }
        final String cmdLine=taskInfoAttributes.get(COMMAND_LINE);
        return isJavaCmd(cmdLine);
    }
    
    /**
     * Check if the module is a java module based on the commandLine declared in the manifest file.
     * 
     * @param cmdLine
     * @return
     */
    public static boolean isJavaCmd(final String cmdLine) {
        if (cmdLine==null) {
            return false;
        }
        if (cmdLine.startsWith("<java>")) {
            return true;
        }
        
        final String arg=cmdLine.split(" ")[0];
        if ("<R>".equals(arg)) {
            // <R> is wrapped as a java command; set -Xmx flag
            return true;
        }
        else if ("<R2.5>".equals(arg)) {
            // <R2.5> is wrapped as a java command; set -Xmx flag
            return true;
        }
        else if ("<R2.7>".equals(arg)) {
            // <R2.7> is wrapped as a java command; set -Xmx flag
            return true;
        }
        else if ("<R2.13>".equals(arg)) {
            // <R2.13> is wrapped as a java command; set -Xmx flag
            return true;
        }
        else if (cmdLine.startsWith("<R2.5_Rjava>")) {
            // <R2.5_Rjava> wrappers are wrapped as java command; set -Xmx flag
            return true;
        }
        else if (arg.startsWith("<R") && arg.endsWith("_Rjava>")) {
            // <R{version}_Rjava> wrappers are wrapped as java command; set -Xmx flag
            return true;
        }
        else if (arg.startsWith("<java")) {
            // <java-{version}> and <java_{version}> wrappers should set -Xmx flag
            return true;
        }
        else if (arg.startsWith("java")) {
            return true;
        }
        return false;
    }

}
