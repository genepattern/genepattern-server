/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import java.io.IOException;
import java.util.ArrayList;
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
    private int numHeaderLines=0;
    
    public CommonsExecCmdRunner() {
    }
    public CommonsExecCmdRunner(int numHeaderLines) {
        this.numHeaderLines=numHeaderLines;
    }

    /**
     * Read each line into memory, retrieve as a List<String>.
     * Skip the first (header) line.
     */
    protected class LineReaderLogOutputStream extends LogOutputStream {
//        public LineReaderLogOutputStream() {
//            this(0);
//        }
//        public LineReaderLogOutputStream(int numHeaderLines) {
//            this.numHeaderLines=numHeaderLines;
//        }
        
//        private int numHeaderLines=0; //number of lines to skip
        private List<String> lines = new ArrayList<String>();

        int lineNum=0;
        @Override
        protected void processLine(String line, int level) {
            ++lineNum; // first line is '1'
            if (lineNum<=numHeaderLines) {
                //ignore
                return;
            }
            lines.add(line);
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
        // kill the process after 60 seconds
        exec.setWatchdog(new ExecuteWatchdog(60000));
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
        boolean handleQuoting=false;
        CommandLine cl=new CommandLine(gpCommand.get(0));
        for(int i=1; i<gpCommand.size(); ++i) {
            cl.addArgument(gpCommand.get(i), handleQuoting);
        }
        return cl;
    }

}
