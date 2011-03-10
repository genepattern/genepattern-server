package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.INPUT_BASENAME;
import static org.genepattern.util.GPConstants.INPUT_EXTENSION;
import static org.genepattern.util.GPConstants.INPUT_FILE;
import static org.genepattern.util.GPConstants.INPUT_PATH;
import static org.genepattern.util.GPConstants.JAVA_FLAGS;
import static org.genepattern.util.GPConstants.JOB_ID;
import static org.genepattern.util.GPConstants.LIBDIR;
import static org.genepattern.util.GPConstants.LSID;
import static org.genepattern.util.GPConstants.NAME;
import static org.genepattern.util.GPConstants.PARAM_INFO_NAME_OFFSET;
import static org.genepattern.util.GPConstants.PARAM_INFO_OPTIONAL;
import static org.genepattern.util.GPConstants.PARAM_INFO_PREFIX;
import static org.genepattern.util.GPConstants.PIPELINE_ARG_STOP_AFTER_TASK_NUM;
import static org.genepattern.util.GPConstants.TASK_ID;
import static org.genepattern.util.GPConstants.USERID;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask.INPUT_FILE_MODE;
import org.genepattern.server.util.PropertiesManager;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.util.LSID;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;

/**
 * Utility class for parameter substitution.
 * 
 * @author pcarr
 */
public class CommandLineParser {
    public static Logger log = Logger.getLogger(CommandLineParser.class);
    
    public static class Exception extends java.lang.Exception {
        public Exception(String message) {
            super(message);
        }
        public Exception(String message, Throwable t) {
            super(message, t);
        }
    }
    
    /**
     * Create the command line to run.
     * 
     * @param cmdLine, the raw command line from the module's manifest.
     * @param props, a lookup table for mapping variable names to substitution values.
     * @param formalParameters, the list of ParameterInfo for the module.
     * 
     * @return the list of command line arguments, including the executable as the first item.
     * 
     * @throws Exception
     */
    public static String[] createCmdArray(String cmdLine, Properties props, ParameterInfo[] formalParameters) 
    throws CommandLineParser.Exception
    {
        //StringTokenizer stCommandLine;
        List<String> commandTokens = new ArrayList<String>();
        //String firstToken;

        // TODO: handle quoted arguments within the command line (eg. echo "<p1> <p2>" as a single token)

        // check that the user didn't quote the program name
        if (!cmdLine.startsWith("\"")) {
            // since we could have a definition like "<perl>=perl -Ifoo",
            // we need to double-tokenize the first token to extract just "perl"
            StringTokenizer stCommandLine = new StringTokenizer(cmdLine);
            String firstToken = stCommandLine.nextToken();
            // now the command line contains the real first word (perl)
            // followed by the rest, ready for space-tokenizing
            cmdLine = substitute(firstToken, props, formalParameters) + cmdLine.substring(firstToken.length());
            stCommandLine = new StringTokenizer(cmdLine);
            while(stCommandLine.hasMoreTokens()) {
                String token = stCommandLine.nextToken();
                String substitutedToken = substitute(token, props, formalParameters);
                if (substitutedToken != null) {
                    commandTokens.add(substitutedToken);
                }
            }
        } 
        else {
            // the user quoted the command, so it has to be handled specially
            int endQuote = cmdLine.indexOf("\"", 1);
            // find the matching closing quote
            if (endQuote == -1) {
                //vProblems.add("Missing closing quote on command line: " + cmdLine);
                throw new CommandLineParser.Exception("Missing closing quote on command line: " + cmdLine);
            } 
            String firstToken = cmdLine.substring(1, endQuote);
            commandTokens.add(substitute(firstToken, props, formalParameters));
            StringTokenizer stCommandLine = new StringTokenizer(cmdLine.substring(endQuote + 1));
            while(stCommandLine.hasMoreTokens()) {
                String token = stCommandLine.nextToken();
                String substitutedToken = substitute(token, props, formalParameters);
                if (substitutedToken != null) {
                    commandTokens.add(substitutedToken);
                }
            }
        }

        // do the substitutions one more time to allow, for example, p2=<p1>.res
        List<String> secondPass = new ArrayList<String>();
        for(String token : commandTokens) {
            String substitutedToken = substitute(token, props, formalParameters);
            if (substitutedToken != null) {
                secondPass.add(substitutedToken);
            }
        }
        String[] commandArray = new String[secondPass.size()];
        commandArray = secondPass.toArray(commandArray);
        return commandArray;
    }
    
    private void addTaskProperties(Properties props, JobInfo jobInfo) {
        props.put(NAME, jobInfo.getTaskName());
        props.put(JOB_ID, Integer.toString(jobInfo.getJobNumber()));
        props.put("parent_" + JOB_ID, Integer.toString(jobInfo._getParentJobNumber()));
        props.put(TASK_ID, Integer.toString(jobInfo.getTaskID()));
        props.put(USERID, "" + jobInfo.getUserId());
        props.put(PIPELINE_ARG_STOP_AFTER_TASK_NUM, ""); 
        // should be overridden by actuals if provided
        props.put(LSID, jobInfo.getTaskLSID());
        // as a convenience to the user, create a <libdir> property which is
        // where DLLs, JARs, EXEs, etc. are dumped to when adding tasks
        String taskLibDir = "taskLibDir";
        if (jobInfo.getTaskID() != -1) {
            try {
                taskLibDir = DirectoryManager.getTaskLibDir(jobInfo.getTaskName(), jobInfo.getTaskLSID(), jobInfo.getUserId());
                File f = new File(taskLibDir);
                taskLibDir = f.getPath() + System.getProperty("file.separator");
            }
            catch (Throwable t) {
                log.error("Error setting <"+LIBDIR+"> property for job #"+jobInfo.getJobNumber(), t);
            }
        }
        props.put(LIBDIR, taskLibDir);

        // set the java flags if they have been overridden in the java_flags.properties file
        PropertiesManager pm = PropertiesManager.getInstance();
        Properties javaFlagProps = pm.getJavaFlags();
        String javaFlags = javaFlagProps.getProperty(jobInfo.getTaskLSID());
        if (javaFlags == null) {
            try {
                LSID lsid = new LSID(jobInfo.getTaskLSID());
                javaFlags = javaFlagProps.getProperty(lsid.toStringNoVersion());
            }
            catch (Throwable t) {
                log.error("Error setting <"+JAVA_FLAGS+"> for job #"+jobInfo.getJobNumber(), t);
            }
        }
        if (javaFlags != null) {
            props.put(JAVA_FLAGS, javaFlags);
        }
    }
    
    /**
     * Fill returned Properties with everything that the user can get a
     * substitution for, including all System.getProperties() properties plus
     * all of the actual ParameterInfo name/value pairs.
     * <p/>
     * <p/>
     * Each input file gets additional entries for the directory (INPUT_PATH)
     * the file name (just filename, no path) aka INPUT_FILE, and the base name
     * (no path, no extension), aka INPUT_BASENAME. These are considered helper
     * parameters which can be used in command line substitutions.
     * <p/>
     * <p/>
     * Other properties added to the command line substitution environment are:
     * <ul>
     * <li>NAME (task name)</li>
     * <li>JOB_ID (job number when executing)</li>
     * <li>TASK_ID (task ID number from task_master table)</li>
     * <li>&lt;JAVA&gt; fully qualified filename to Java VM running the
     * GenePatternAnalysis engine</li>
     * <li>LIBDIR directory containing the task's support files (post-fixed by a
     * path separator for convenience of task writer)</li>
     * </ul>
     * <p/>
     * <p/>
     * Called by onJob() to create actual run-time parameter lookup, and by
     * validateInputs() for both task save-time and task run-time parameter
     * validation.
     * <p/>
     * 
     * @param taskName, name of task to be run
     * @param jobNumber, job number of job to be run
     * @param taskID, task ID of job to be run
     * @param taskInfoAttributes, TaskInfoAttributes metadata of job to be run
     * @param actuals, actual parameters to substitute for job to be run
     * @param env, Hashtable of environment variables values
     * @param formalParameters, ParameterInfo[] of formal parameter definitions, 
     *     used to determine which parameters are input files 
     *     (therefore needing additional attributes added to substitution table)
     * @return Properties object with all substitution name/value pairs defined
     * 
     * @author Jim Lerner
     */
    private static Properties setupProps(
            //TaskInfo taskInfo, 
            String taskName, 
            int parentJobNumber, 
            int jobNumber, 
            int taskID,
            TaskInfoAttributes taskInfoAttributes, 
            ParameterInfo[] actuals,
            Map<String, String> env, 
            ParameterInfo[] formalParameters,
            String userID) 
    throws MalformedURLException 
    {
        Properties props = new Properties();
        int formalParamsLength = 0;
        if (formalParameters != null) {
            formalParamsLength = formalParameters.length;
        }
        //INPUT_FILE_MODE inputFileMode = getInputFileMode();
        try {
            
            // populate props with the input parameters so that they can be looked up by name
            if (actuals != null) {
                for (int i = 0; i < actuals.length; i++) {
                    String value = actuals[i].getValue();
                    if (value == null) {
                        value = "";
                    }
                    props.put(actuals[i].getName(), value);
                }
            }
            String inputFilename = null;
            String inputParamName = null;
            String outDirName = GenePatternAnalysisTask.getJobDir(Integer.toString(jobNumber));
            new File(outDirName).mkdirs();
            int j;
            // find input filenames, create _path, _file, and _basename props for each
            //boolean isPipeline = taskInfo != null && taskInfo.isPipeline();
            return props;
        }
        catch (NullPointerException npe) {
            log.error(npe + " in setupProps.  Currently have:\n" + props);
            throw npe;
        }
    }

    /**
     * Extract a list of substitution parameters from the given String.
     * A substitution parameter is of the form, &lt;name&gt;
     * 
     * @param str
     * @return a List of substitution parameters, each item includes the enclosing '<' and '>' brackets.
     */
    public static List<String> getSubstitutionParameters(String str) {
        List<String> paramNames = new ArrayList<String>();
        //<("[^"]*"|'[^']*'|[^'">])*>
        //String patternRegex = "<[^>]*>";
        String patternRegex = "<[\\w|\\.&^\\s]*>";
        Pattern pattern = null;
        try {
            pattern = Pattern.compile(patternRegex);
        }
        catch (PatternSyntaxException e) {
            log.error("Error creating pattern for: "+patternRegex, e);
            return paramNames;
        }
        catch (Throwable t) {
            log.error("Error creating pattern for: '"+patternRegex+": "+t.getLocalizedMessage() );
            return paramNames;
        }
        Matcher matcher = pattern.matcher(str);
        while(matcher.find()) {
            int sidx = matcher.start();
            int eidx = matcher.end();
            String param = str.substring(sidx, eidx);
            paramNames.add(param);
        }
        return paramNames;
    }

    private static class MyStringTokenizer {
        private enum ST { in_ws, in_quote, in_word, end }
        
        private ST status = ST.in_ws;
        
        private List<String> tokens = new ArrayList<String>();
        private String cur = "";
        
        public List<String> getTokens() {
            return tokens;
        }
         
        public void readNextChar(char c) {
            if (Character.isWhitespace(c)) {
                switch (status) {
                case in_ws:
                    break;
                case in_quote:
                    cur += c;
                    break;
                case in_word: 
                    tokens.add(cur);
                    cur = "";
                    status = ST.in_ws;
                    break;
                }
            }
            else if (c == '\"') {
                switch (status) {
                case in_ws:
                    status = ST.in_quote;
                    cur = "";
                    break;
                case in_quote:
                    status = ST.in_ws;
                    tokens.add(cur);
                    cur = "";
                case in_word:
                    //TODO: what happens with a '"' in a word?
                    cur += c;
                    break;
                }
            }
            else {
                switch (status) {
                case in_ws:
                    status = ST.in_word;
                case in_quote:
                case in_word:
                    cur += c;
                    break;
                }
            }
        }
    }
    
    private static List<String> getTokens(String arg) {  
        //TODO: handle escaped quotes and spaces
        MyStringTokenizer st = new MyStringTokenizer();
        for(int i=0; i<arg.length(); ++i) {
            st.readNextChar(arg.charAt(i));
        }
        st.readNextChar(' ');
        
        return st.getTokens();
    }

    private static List<String> resolveValue(final String value, final Map<String,String> dict, final int depth) {
        if (value == null) {
            //TODO: decide to throw exception or return null or return list containing one null item
            throw new IllegalArgumentException("value is null");
        }
        List<String> rval = new ArrayList<String>();
        
        List<String> variables = getSubstitutionParameters(value);
        //case 1: if value contains no substitutions, return a one-item list, containing the value
        if (variables == null || variables.size() == 0) {
            rval.add( value );
            return rval;
        }
        
        //otherwise, tokenize and return
        List<String> tokens = getTokens(value);
        for(String token : tokens) {
            String substitution = substituteValue(token, dict);
            //if there is no difference, return
            if (token.equals(substitution)) {
                log.error("Token '"+token+"' not resolved.");
                rval.add( token );
            }
            else {
                rval.addAll( resolveValue( substitution, dict, 1+depth ) );
            }
        }
        return rval;
    }

    private static String substituteValue(final String arg, final Map<String,String> dict) {
        List<String> subs = getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            return arg;
        }
        String substitutedValue = arg;
        for(String sub : subs) {
            String paramName = sub.substring(1, sub.length()-1);
            String value = dict.get(paramName);
            if (value == null) {
                //TODO: throw exception
                log.error("missing substitution value for '"+sub+"' in expression: "+arg);
                value = sub;
            }
            substitutedValue = substitutedValue.replace(sub, value);
        }
        return substitutedValue;
    }

    private static Map<String,ParameterInfo> getParameterInfoMap(final ParameterInfo[] parameterInfoArray) {
        List<ParameterInfo> list = new ArrayList<ParameterInfo>();
        for(ParameterInfo param : parameterInfoArray) {
            list.add(param);
        }
        return getParameterInfoMap(list);
    }

    private static Map<String,ParameterInfo> getParameterInfoMap(final List<ParameterInfo> parameterInfoList) {
        Map<String,ParameterInfo> map = new LinkedHashMap<String,ParameterInfo>();
        for(ParameterInfo param : parameterInfoList) {
            map.put(param.getName(), param);
        }
        return map;
    }

    /**
     * Given a cmdLine, a jobInfo object, and any additional runtime properties, generate a command line array, 
     * substituting, when necessary, from system, env, and job properties.
     * 
     * @param cmdLine
     * @param env
     * @param jobInfo
     * @return
     */
    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> env, final JobInfo jobInfo) {
        Map<String,String> props = new LinkedHashMap<String,String>();
        loadSystemProperties(props);
        loadEnvironment(props, env);
        
        Map<String,String> jobProps = getJobParameters(jobInfo);
        loadEnvironment(props, jobProps);

        return translateCmdLine(cmdLine, props);
    }

    /**
     * Copy environment variables into props.
     * @param props
     */
    private static void loadSystemProperties(Map<String,String> props) {
        String key = null;
        String value = null;
        for (Enumeration<?> eVariables = System.getProperties().propertyNames(); eVariables.hasMoreElements();) {
            key = (String) eVariables.nextElement();
            value = System.getProperty(key, "");
            props.put(key, value);
        }
    }
    
    private static void loadEnvironment(Map<String,String> propsTo, Map<String,String> envFrom) {
        for(Entry<String,String> entry : envFrom.entrySet()) {
            propsTo.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get all of the parameter values that were entered for a particular job, include optional parameter values.
     * @param jobInfo
     * @return
     */
    private static Map<String,String> getJobParameters(JobInfo jobInfo) {
        Map<String,String> props = new LinkedHashMap<String,String>();
        if (jobInfo == null) {
            log.error("null arg");
            return props;
        }
        final ParameterInfo[] actuals = jobInfo.getParameterInfoArray();
        if (actuals == null) {
            log.error("jobInfo with null parameterInfoArray, jobId="+jobInfo.getJobNumber());
            return props;
        }

        String lsid = jobInfo.getTaskLSID();
        TaskInfo taskInfo = TaskInfoCache.instance().getTask(lsid);
        ParameterInfo[] formalParameters = taskInfo.getParameterInfoArray();
        String outDirName = GenePatternAnalysisTask.getJobDir(Integer.toString(jobInfo.getJobNumber()));
        INPUT_FILE_MODE inputFileMode = GenePatternAnalysisTask.getInputFileMode();
        boolean isPipeline = taskInfo.isPipeline();

        for (int i = 0; i < actuals.length; i++) {
            for (int f = 0; f < formalParameters.length; f++) {
                if (actuals[i].getName().equals(formalParameters[f].getName())) {
                    if (formalParameters[f].isInputFile() && !isPipeline) { 
                        // don't change parameter values for input files to pipelines
                        String inputFilename = actuals[i].getValue();
                        if (inputFilename == null || inputFilename.length() == 0) {
                            continue;
                        }
                        String inputParamName = actuals[i].getName();
                        File inFile = new File(outDirName, new File(inputFilename).getName());
                        String baseName = inFile.getName();
                        if (baseName.startsWith("Axis")) {
                            // strip off the AxisNNNNNaxis_ prefix
                            if (baseName.indexOf("_") != -1) {
                                baseName = baseName.substring(baseName.indexOf("_") + 1);
                            }
                        }

                        if (inputFileMode != INPUT_FILE_MODE.PATH) {
                            // file is moved to job directory
                            props.put(inputParamName, baseName);
                        }
                        props.put(inputParamName + INPUT_PATH, new String(outDirName));

                        // filename without path
                        props.put(inputParamName + INPUT_FILE, new String(baseName));
                        int j = baseName.lastIndexOf(".");
                        if (j != -1) {
                            props.put(inputParamName + INPUT_EXTENSION, new String(baseName.substring(j + 1))); 
                            // filename extension
                            baseName = baseName.substring(0, j);
                        }
                        else {
                            props.put(inputParamName + INPUT_EXTENSION, ""); 
                            // filename extension
                        }
                        if (inputFilename.startsWith("http:")
                                || inputFilename.startsWith("https:")
                                || inputFilename.startsWith("ftp:")) {
                            j = baseName.lastIndexOf("?");
                            if (j != -1) {
                                baseName = baseName.substring(j + 1);
                            }
                            j = baseName.lastIndexOf("&");
                            if (j != -1) {
                                baseName = baseName.substring(j + 1);
                            }
                            j = baseName.lastIndexOf("=");
                            if (j != -1) {
                                baseName = baseName.substring(j + 1);
                            }
                        }
                        // filename without path or extension
                        props.put(inputParamName + INPUT_BASENAME, new String(baseName));
                    }
                    break;
                }
            }
        }
        return props;
    }
    
    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> in) {
        return resolveValue(cmdLine, in, 0);
    }
    
    /**
     * Replace all substitution variables, of the form &lt;variable&gt;, with values from the given properties instance.
     * Prepend the prefix for any substituted parameters which have a prefix.
     * 
     * @param commandLine from the manifest for the module
     * @param props, Properties object containing name/value pairs for parameter substitution in the command line
     * @param params, ParameterInfo[] describing whether each parameter has a prefix defined.
     * 
     * @return String command line with all substitutions made
     * 
     * @author Jim Lerner, Peter Carr
     */
    public static String substitute(final String commandLine, final Properties props, final ParameterInfo[] params) {
        if (commandLine == null) {
            return null;
        }
        List<String> substituted = substitute(commandLine, props, params, false);
        if (substituted == null || substituted.size() == 0) {
            return null;
        }
        else if (substituted.size()==1) {
            return substituted.get(0);
        }
        //unexpected, rval contains more than one item
        log.error("Unexpected rval in substitute, expecting a single item list, but the list contained "+substituted.size()+" items.");
        String rval = "";
        boolean first = true;
        for(String s : substituted) {
            if (first) {
                first = false;
            }
            else {
                s = " "+s;
            }
            rval += s;
        }
        return rval;
    }
    
    public static List<String> substitute(final String commandLine, final Properties props, final ParameterInfo[] params, boolean split) {
        if (commandLine == null) {
            return null;
        }
        String substituted = commandLine;
        
        Map<String, ParameterInfo> parameterInfoMap = getParameterInfoMap(params);
        List<String> substitutionParameters = getSubstitutionParameters(commandLine);
        
        for(String substitutionParameter : substitutionParameters) {
            String varName = substitutionParameter.substring(1, substitutionParameter.length() - 1);
            //default to empty string, to handle optional parameters which have not been set
            String replacement = props.getProperty(varName, "");
            if (varName.equals("resources")) {
                //TODO: this should really be in the setupProps
                //special-case for <resources>, 
                // make this an absolute path so that pipeline jobs running in their own directories see the right path
                replacement = new File(replacement).getAbsolutePath();
            }
            ParameterInfo paramInfo = parameterInfoMap.get(varName);
            boolean isOptional = true;
            HashMap hmAttributes = null;
            if (paramInfo != null) {
                hmAttributes = paramInfo.getAttributes();
            }
            if (hmAttributes != null) {
                if (hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null) {
                    isOptional = false;
                }
                String optionalPrefix = (String) hmAttributes.get(PARAM_INFO_PREFIX[PARAM_INFO_NAME_OFFSET]);
                if (replacement.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
                    if (optionalPrefix.endsWith(" ")) {
                        //special-case: GP-2866
                        //    if optionalPrefix ends with a space, split into two args
                    }
                    replacement = optionalPrefix + replacement;
                }
            }
            substituted = substituted.replace(substitutionParameter, replacement);
            if (substituted.length() == 0 && isOptional) {
                return null;
            }
        }
        //return substituted;
        List<String> rval = new ArrayList<String>();
        rval.add(substituted);
        return rval;
    }


}
