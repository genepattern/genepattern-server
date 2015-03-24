package org.genepattern.server.genepattern;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.ParameterInfo;

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
     * Create the list of cmd line args for the module, GP <= 3.9.1 implementation.
     * The GPAT.java onJob method must initialize the full set of substitution parameters before calling this method.
     * 
     * @param cmdLine, the command line string from the module manifest file
     * @param props, the lookup table of all substitution parameters
     * @param formalParameters, the formal parameters from the TaskInfo, needed for some special-cases such as splitting prefix when run params. 
     * @return
     */
    public static List<String> createCmdLine(String cmdLine, Properties props, ParameterInfo[] formalParameters) { 
        return createCmdLine(null, null, cmdLine, props, formalParameters);
    }

    /**
     * Updated createCmdLine method which uses the gpConfig to resolve global parameters, such as R2.15_HOME, which can be set at server runtime
     * as a result of installing a patch.
     * 
     * @param gpConfig
     * @param gpContext
     * @param cmdLine
     * @param props
     * @param formalParameters
     * @return
     */
    public static List<String> createCmdLine(final GpConfig gpConfig, final GpContext gpContext, String cmdLine, Properties props, ParameterInfo[] formalParameters) { 
        Map<String,String> env = new HashMap<String,String>();
        for(Object keyObj : props.keySet()) {
            String key = keyObj.toString();
            env.put( key.toString(), props.getProperty(key));
        }
        Map<String,ParameterInfo> parameterInfoMap = createParameterInfoMap(formalParameters);
        return resolveValue(gpConfig, gpContext, cmdLine, env, parameterInfoMap, 0);
    }

    /**
     * Proposed newer createCmdLine method (this works when generating the command line for installing patches) which is not yet ready for production.
     * Still need to implement support for file input parameters.
     * 
     * @param gpConfig
     * @param gpContext
     * @param cmdLine
     * @param formalParameters
     * @return
     */
    public static List<String> createCmdLine(GpConfig gpConfig, GpContext gpContext, String cmdLine, ParameterInfo[] formalParameters) { 
        Map<String,ParameterInfo> parameterInfoMap = createParameterInfoMap(formalParameters);
        return resolveValue(gpConfig, gpContext, cmdLine, parameterInfoMap, 0);
    }
    
    private static Map<String,ParameterInfo> createParameterInfoMap(ParameterInfo[] params) {
        Map<String,ParameterInfo> map = new HashMap<String,ParameterInfo>();
        if (params != null) {
            for(ParameterInfo param : params) {
                map.put(param.getName(), param);
            }
        }
        return map;
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
        //String patternRegex = "<[-|\\w|\\.&^\\s]*>";
        //new way
        String patternRegex = "(<[^<^ ][^>&^ ]*>)";
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
                    break;
                case in_word:
                    //with a '"' in a word, include in the word, scan until the next WS char
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

    private static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value, final Map<String,String> props, final Map<String,ParameterInfo> parameterInfoMap, final int depth) {
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
            List<String> substitution = substituteValue(gpConfig, gpContext, token, props, parameterInfoMap);
            
            if (substitution == null || substitution.size() == 0) {
                //remove empty substitutions
            }
            else if (substitution.size() == 1) {
                String singleValue = substitution.get(0);
                if (singleValue == null) {
                    //ignore
                }
                else if (token.equals(singleValue)) {
                    rval.add( token );
                }
                else {
                    List<String> resolvedList = resolveValue(gpConfig, gpContext, singleValue, props, parameterInfoMap, 1+depth );
                    rval.addAll( resolvedList );
                }
            }
            else {
                for(String sub : substitution) {
                    List<String> resolvedSub = resolveValue(gpConfig, gpContext, sub, props, parameterInfoMap, 1+depth);
                    rval.addAll( resolvedSub );
                }
            }
        }
        return rval;
    }
    
    protected static List<String> resolveValue(final GpConfig gpConfig, final GpContext gpContext, final String value, final Map<String,ParameterInfo> parameterInfoMap, final int depth) {
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
            List<String> substitution = substituteValue(gpConfig, gpContext, token, parameterInfoMap);
            
            if (substitution == null || substitution.size() == 0) {
                //remove empty substitutions
            }
            else if (substitution.size() == 1) {
                String singleValue = substitution.get(0);
                if (singleValue == null) {
                    //ignore
                }
                else if (token.equals(singleValue)) {
                    rval.add( token );
                }
                else {
                    List<String> resolvedList = resolveValue(gpConfig, gpContext, singleValue, parameterInfoMap, 1+depth );
                    rval.addAll( resolvedList );
                }
            }
            else {
                for(String sub : substitution) {
                    List<String> resolvedSub = resolveValue(gpConfig, gpContext, sub, parameterInfoMap, 1+depth);
                    rval.addAll( resolvedSub );
                }
            }
        }
        return rval;
    }

    private static List<String> substituteValue(final GpConfig gpConfig, final GpContext gpContext, final String arg, final Map<String,String> dict, final Map<String,ParameterInfo> parameterInfoMap) {
        List<String> rval = new ArrayList<String>();
        List<String> subs = getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            rval.add(arg);
            return rval;
        }
        String substitutedValue = arg;
        boolean isOptional = true;
        for(String sub : subs) {
            String paramName = sub.substring(1, sub.length()-1);
            String value = null;
            if (dict.containsKey(paramName)) {
                value = dict.get(paramName);
            }
            else if (gpConfig != null) {
                value = gpConfig.getGPProperty(gpContext, paramName);
            }
 
            //default to empty string, to handle optional parameters which have not been set
            //String replacement = props.getProperty(varName, "");
            if (paramName.equals("resources") && value != null) {
                //TODO: this should really be in the setupProps
                //special-case for <resources>, 
                // make this an absolute path so that pipeline jobs running in their own directories see the right path
                value = new File(value).getAbsolutePath();
            }

            ParameterInfo paramInfo = parameterInfoMap.get(paramName);
            if (paramInfo != null) {
                isOptional = paramInfo.isOptional();
                String optionalPrefix = paramInfo._getOptionalPrefix();
                if (value != null && value.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
                    if (optionalPrefix.endsWith("\\ ")) {
                        //special-case: if optionalPrefix ends with an escaped space, don't split into two args
                        value = optionalPrefix.substring(0, optionalPrefix.length()-3) + value;
                    }
                    else if (optionalPrefix.endsWith(" ")) {
                        //special-case: GP-2866, if optionalPrefix ends with a space, split into two args 
                        rval.add(optionalPrefix.substring(0, optionalPrefix.length()-1));
                    }
                    else {
                        //otherwise, append the prefix to the value
                        value = optionalPrefix + value;
                    }
                }
            }
            
            if (value == null && isOptional == false) {
                //TODO: throw exception
                log.error("missing substitution value for '"+sub+"' in expression: "+arg);
                value = sub;
            }
            else if (value == null &&  isOptional == true) {
                value = "";
            }
            substitutedValue = substitutedValue.replace(sub, value);
        }
        if (substitutedValue.length() == 0 && isOptional) {
            //return an empty list
        }
        else {
            rval.add(substitutedValue);
        }
        return rval;
    }

    protected static List<String> substituteValue(final GpConfig gpConfig, final GpContext gpContext, final String arg, final Map<String,ParameterInfo> parameterInfoMap) {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig == null");
        }
        List<String> rval = new ArrayList<String>();
        List<String> subs = getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            rval.add(arg);
            return rval;
        }
        String substitutedValue = arg;
        boolean isOptional = true;
        for(final String sub : subs) {
            final String paramName = sub.substring(1, sub.length()-1);
            String value=null;

            //special-case for <job_id>
            if ("job_id".equals(paramName)) {
                if (gpContext.getJobNumber() != null) {
                    value=""+gpContext.getJobNumber();
                }
            }
            //special-case for <name>
            else if ("name".equals(paramName)) {
                if (gpContext.getTaskInfo() != null) {
                    value=gpContext.getTaskInfo().getName();
                }
            }
            //special-case for <parent_job_id>
            else if ("parent_job_id".equals(paramName)) {
                if (gpContext.getJobInfo() != null) {
                    value=""+gpContext.getJobInfo()._getParentJobNumber();
                }
            }
            //special-case for <userid>
            else if ("userid".equals(paramName)) {
                if (gpContext.getJobInfo() != null) {
                    value=gpContext.getJobInfo().getUserId();
                }
            }
            //special-case for <LSID>
            else if ("LSID".equals(paramName)) {
                value=gpContext.getLsid();
            }
            //special-case for <libdir>
            else if ("libdir".equals(paramName)) {
                File libdir=gpContext.getTaskLibDir();
                if (libdir==null) {
                    log.error("Unexpected null value for gpContext.getTaskLibDir, calling gpConfig.getGPFileProperty instead");
                    libdir=gpConfig.getGPFileProperty(gpContext, paramName);
                }
                if (libdir != null) {
                    value=""+libdir;
                    if (!value.endsWith(File.separator)) {
                        value+=File.separator;
                    }
                }
            }
            //special-case for <patches>
            else if ("patches".equals(paramName)) {
                File pluginDir=gpConfig.getRootPluginDir(gpContext);
                if (pluginDir != null) {
                    value=""+pluginDir;
                }
            }
            //special-case for <resources>
            else if ("resources".equals(paramName)) {
                File f=gpConfig.getResourcesDir();
                if (f!=null) {
                    value = f.getAbsolutePath();
                }
                else {
                    value = gpConfig.getGPProperty(gpContext, paramName);
                }
            }
            else {
                value = gpConfig.getGPProperty(gpContext, paramName);
            }
            ParameterInfo paramInfo = parameterInfoMap == null ? null :
                parameterInfoMap.get(paramName);
            if (paramInfo != null) {
                isOptional = paramInfo.isOptional();
                String optionalPrefix = paramInfo._getOptionalPrefix();
                if (value != null && value.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
                    if (optionalPrefix.endsWith("\\ ")) {
                        //special-case: if optionalPrefix ends with an escaped space, don't split into two args
                        value = optionalPrefix.substring(0, optionalPrefix.length()-3) + value;
                    }
                    else if (optionalPrefix.endsWith(" ")) {
                        //special-case: GP-2866, if optionalPrefix ends with a space, split into two args 
                        rval.add(optionalPrefix.substring(0, optionalPrefix.length()-1));
                    }
                    else {
                        //otherwise, append the prefix to the value
                        value = optionalPrefix + value;
                    }
                }
            }
            
            if (value == null && isOptional == false) {
                log.error("missing substitution value for '"+sub+"' in expression: "+arg);
                value = sub;
            }
            else if (value == null &&  isOptional == true) {
                value = "";
            }
            substitutedValue = substitutedValue.replace(sub, value);
        }
        if (substitutedValue.length() == 0 && isOptional) {
            //return an empty list
        }
        else {
            rval.add(substitutedValue);
        }
        return rval;
    }

    public static List<String> translateCmdLine(final GpConfig gpConfig, final GpContext gpContext, final String cmdLine) {
        final Map<String,ParameterInfo> emptyParameterInfoMap = Collections.emptyMap();
        return resolveValue(gpConfig, gpContext, cmdLine, emptyParameterInfoMap, 0);
    }
    
    public static List<String> translateCmdLine(final GpConfig gpConfig, final GpContext gpContext, final String cmdLine, final Map<String,ParameterInfo> parameterInfoMap) {
        return resolveValue(gpConfig, gpContext, cmdLine, parameterInfoMap, 0);
    }

}
