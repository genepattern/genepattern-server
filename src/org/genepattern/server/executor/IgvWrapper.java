package org.genepattern.server.executor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

public class IgvWrapper implements CommandExecutor {

    /**
     * flag as ERROR.
     */
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        return 0;
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        //replace -f<filename> with special URL to servlet
        int i=0;
        for(String arg : commandLine) {
            if (arg.startsWith("-f")) {
                final String inputfileOrig = arg.substring(2);
                final String inputfileWrapper = registerInputfile(inputfileOrig);
                commandLine[i] = "-f"+inputfileWrapper;
                break;
            }
            ++i;
        }
        
        ParameterInfo inputFileParam = getInputFile(jobInfo);
        
        CommandExecutor runtimeExec = CommandManagerFactory.getCommandManager().getCommandExecutorsMap().get("RuntimeExec");
        runtimeExec.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
    }
    
    private ParameterInfo getInputFile(JobInfo jobInfo) {
        for(ParameterInfo param : jobInfo.getParameterInfoArray()) {
            if ("input.file".equals(param.getName())) {
                return param;
            }
        }
        return null;
    }
    
    private String getFileServletUrl() {
        ServerConfiguration.Context context = ServerConfiguration.Context.getServerContext();
        String server = ServerConfigurationFactory.instance().getGPProperty(context, "GenePatternURL");
        if (!server.endsWith("/")) {
            server += "/";
        }
        return server + "data/";
    }

    /**
     *
     * @param inputfile, the original input file from the module command line
     * @return a ur callback to the GP server from which IGV can access the file without user credentials.
     */
    private String registerInputfile(String inputfile) {
        String path=inputfile;
        //if it's a URL, pass it along to the IGV command line
        try {
            URL url = new URL(inputfile);
            return inputfile;
        }
        catch (MalformedURLException e) {
            //continue
        }
        
        //otherwise, it's a server file path, construct a url to the data servlet
        String rval = getFileServletUrl();
        rval += path;
        return rval;
    }

    /**
     * @deprecated
     */
    public void setConfigurationFilename(String filename) {
    }

    /**
     * @deprecated
     */
    public void setConfigurationProperties(CommandProperties properties) {
    }

    public void start() {
    }

    public void stop() {
    }

    public void terminateJob(JobInfo jobInfo) throws Exception {
    }

}
