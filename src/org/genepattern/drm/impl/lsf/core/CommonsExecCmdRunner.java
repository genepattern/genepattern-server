/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.log4j.Logger;


public class CommonsExecCmdRunner implements CmdRunner {
    private static final Logger log = Logger.getLogger(CommonsExecCmdRunner.class);
    
    // kill the process after 'timeout_millis' milliseconds
    final Long timeout_millis;

    // don't wrap arguments in quotation marks
    final boolean handleQuoting=false;
    
    public CommonsExecCmdRunner() {
        this(60L*1000L);
    }
    
    public CommonsExecCmdRunner(final long timeout_millis) {
        this.timeout_millis=timeout_millis;
    }

    /**
     * Read each line into memory, retrieve as a List<String>.
     * Skip the first (header) line.
     */
    protected class LineReaderLogOutputStream extends LogOutputStream {
        private final List<String> lines = Collections.synchronizedList(new ArrayList<String>());

        @Override
        protected void processLine(final String line, final int level) {
            lines.add(line);
        }
        
        @Override
        protected void processLine(final String line) {
            super.processLine(line);
        }

        public List<String> getLines() {
            return lines;
        }
    }

    @Override
    public List<String> runCmd(List<String> cmd) throws CmdException {
        final CommandLine cmdLine=initCommandLine(cmd);
        final LineReaderLogOutputStream collectLines=new LineReaderLogOutputStream();
        final DefaultExecutor exec=new DefaultExecutor();
        // collect lines from stdout
        exec.setStreamHandler( new PumpStreamHandler( collectLines ) );
        exec.setWatchdog(new ExecuteWatchdog(timeout_millis));
        exec.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        try {
            exec.execute(cmdLine);
        }
        catch (ExecuteException e) {
            log.error(e);
            throw new CmdException("Error running cmd", e);
        }
        catch (IOException e) {
            log.error(e);
            throw new CmdException("Error running cmd", e);
        }
        return collectLines.getLines();
    }

    protected CommandLine initCommandLine(final List<String> gpCommand) {
        final CommandLine cl=new CommandLine(gpCommand.get(0));
        for(final String arg : gpCommand.subList(1, gpCommand.size())) {
            cl.addArgument(arg, handleQuoting);
        }
        return cl;
    }

}
