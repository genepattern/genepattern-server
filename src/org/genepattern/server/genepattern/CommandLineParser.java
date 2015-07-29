/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.INPUT_BASENAME;
import static org.genepattern.util.GPConstants.INPUT_EXTENSION;
import static org.genepattern.util.GPConstants.INPUT_FILE;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.Param;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Joiner;

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
        return ValueResolver.resolveValue(gpConfig, gpContext, cmdLine, props, formalParameters);
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
        Map<String,ParameterInfo> parameterInfoMap = ValueResolver.createParameterInfoMap(formalParameters);
        return resolveValue(gpConfig, gpContext, cmdLine, parameterInfoMap, 0);
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

    protected static String asString(final List<String> arr) {
        if (arr==null) {
            return "<null>";
        }
        String joined=Joiner.on(",").join(arr);
        return joined;
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
        List<String> tokens = ValueResolver.getTokens(value);
        for(String token : tokens) {
            List<String> substitution = substituteValue(gpConfig, gpContext, token, parameterInfoMap);
            if (log.isTraceEnabled()) {
                log.trace("substitute('"+token+"')=[ "+asString(substitution) +" ]");
            }
            
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
            // TODO: special-case for <<param>_basename>
            //else if (paramName.endsWith("_basename")) {
            //    
            //}
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
    
    protected static String getBasenameSubstitution(final GpConfig gpConfig, final GpContext gpContext, final String paramName, final Map<String,ParameterInfo> parameterInfoMap) {
        if (paramName == null) {
            return null;
        }
        
        if (paramName.endsWith("_basename")) {
            int endIndex=paramName.lastIndexOf("_basename");
            Param matchingParam = getParam(gpContext, paramName.substring(0, endIndex));
            if (matchingParam != null) {
                if (matchingParam.getNumValues()>0) {
                    Properties fileProps=initFileProps(matchingParam.getParamId().getFqName(), matchingParam.getValues().get(0).getValue());
                    return fileProps.getProperty(paramName);
                }
            }
        }
        return paramName;
    }
    
    protected static Properties initFileProps(final String inputParamName, final String inputFilename) {
        Properties props=new Properties();
        if (inputFilename == null || inputFilename.length() == 0) {
            return props;
        }
        
        String filePath;
        try {
            URL urlValue=new URL(inputFilename);
            filePath=urlValue.getPath();
        }
        catch (Throwable t) {
            // expected
            filePath=inputFilename;
        }           
        String fileName=new File(filePath).getName();
        if (fileName.startsWith("Axis")) {
            // strip off the AxisNNNNNaxis_ prefix
            if (fileName.indexOf("_") != -1) {
                fileName = fileName.substring(fileName.indexOf("_") + 1);
            }
        }

        props.put(inputParamName, fileName);
        //TODO: props.put(inputParamName + INPUT_PATH, new String(outDirName));

        // filename without path
        props.put(inputParamName + INPUT_FILE, fileName);
        int j = fileName.lastIndexOf(".");
        if (j != -1) {
            props.put(inputParamName + INPUT_EXTENSION, new String(fileName.substring(j + 1)));
            final String baseName = fileName.substring(0, j);
            // filename without path or extension
            props.put(inputParamName + INPUT_BASENAME, baseName);
        } 
        else {
            props.put(inputParamName + INPUT_BASENAME, fileName);
            props.put(inputParamName + INPUT_EXTENSION, "");
        }
                                
        return props;
    }
    
    protected static Param getParam(final GpContext gpContext, final String pname) {
        if (gpContext==null) {
            return null;
        }
        if (gpContext.getJobInput()==null) {
            return null;
        }
        return gpContext.getJobInput().getParam(pname);
    }

    public static List<String> translateCmdLine(final GpConfig gpConfig, final GpContext gpContext, final String cmdLine) {
        final Map<String,ParameterInfo> emptyParameterInfoMap = Collections.emptyMap();
        return resolveValue(gpConfig, gpContext, cmdLine, emptyParameterInfoMap, 0);
    }
    
    public static List<String> translateCmdLine(final GpConfig gpConfig, final GpContext gpContext, final String cmdLine, final Map<String,ParameterInfo> parameterInfoMap) {
        return resolveValue(gpConfig, gpContext, cmdLine, parameterInfoMap, 0);
    }

}
