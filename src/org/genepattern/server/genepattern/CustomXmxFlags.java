package org.genepattern.server.genepattern;

import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpContext;
import static org.genepattern.util.GPConstants.COMMAND_LINE;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

/**
 * Helper class for java modules, to set the java max memory based on the GP server configuration setting 'job.memory'.
 * 
 * This is called after all other command line substitutions have happened and simply replaces all
 * occurrences of -Xmx<originalValue> with -Xmx<customValue> if a custom value has been specified.
 * 
 * Note: this is only applied to java modules. Determined by the command line starting with the '<java>'
 * substitution parameter. 
 * 
 * @see JobRunner#PROP_MEMORY
 * @author pcarr
 *
 */
public class CustomXmxFlags {
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
    
    public static String[] addOrReplaceXmxFlag(final GpContext jobContext, final Memory mem, final String[] cmdLineArgs) {
        if (mem==null) {
            return cmdLineArgs;
        }
        
        if (!isJavaCmd(jobContext)) {
            return cmdLineArgs;
        }
        
        //case 1: replace existing -Xmx flag
        boolean hasXmx=false;
        int idx=0;
        for(final String arg : cmdLineArgs) {
            if (arg.contains("-Xmx")) {
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
            //case 2: add -Xmx flag to command line
            String[] rval=new String[ 1+cmdLineArgs.length ];
            rval[0]=cmdLineArgs[0];
            rval[1]="-Xmx"+mem.toXmx();
            System.arraycopy(cmdLineArgs, 1, rval, 2, cmdLineArgs.length-1);
            return rval;
        }
    }
    
    private static boolean isJavaCmd(final GpContext jobContext) {
        return isJavaCmd(jobContext.getTaskInfo());
    }
    
    private static boolean isJavaCmd(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            return false;
        }
        if (taskInfo.isPipeline()) {
            return false;
        }
        final TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
        final String cmdLine=taskInfoAttributes.get(COMMAND_LINE);
        ///CLOVER:OFF
        if (cmdLine==null) {
            return false;
        }
        ///CLOVER:ON
        return cmdLine.startsWith("<java>");
    }

}
