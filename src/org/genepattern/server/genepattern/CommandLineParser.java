package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.PARAM_INFO_NAME_OFFSET;
import static org.genepattern.util.GPConstants.PARAM_INFO_OPTIONAL;
import static org.genepattern.util.GPConstants.PARAM_INFO_PREFIX;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;

/**
 * Utility class for parameter substitution.
 * 
 * @author pcarr
 */
public class CommandLineParser {
    public static Logger log = Logger.getLogger(CommandLineParser.class);

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
        String patternRegex = "<[^>]*>";
        Pattern pattern = null;
        try {
            pattern = Pattern.compile(patternRegex);
        }
        catch (PatternSyntaxException e) {
            log.error("Error creating pattern for: "+patternRegex, e);
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

    private static List<String> tokenizeCmdLine(String cmdLine) {
        List<String> args = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(cmdLine);
        while(st.hasMoreElements()) {
            args.add( st.nextToken() );
        }
        return args;
    }

    private static String substitute(String arg, Map<String,String> dict) {
        List<String> subs = getSubstitutionParameters(arg);
        if (subs == null || subs.size() == 0) {
            return arg;
        }
        String substitutedValue = arg;
        for(String sub : subs) {
            String paramName = sub.substring(1, sub.length()-1);
            String value = dict.get(paramName);
            if (value == null) {
                //TODO: handle missing substitution value
                value = sub;
            }
            substitutedValue = substitutedValue.replaceAll(sub, value);
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

    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> dict) {
        //break up the command line into tokens
        List<String> argList = tokenizeCmdLine(cmdLine);
        List<String> subList = new ArrayList<String>();
        
        //1st pass
        int i=0;
        for(String arg : argList) {
            String sub = substitute(arg, dict);
            if (i==0) {
                //tokenize the first entry
                List<String> firstArgs = tokenizeCmdLine(sub);
                subList.addAll( firstArgs );
            }
            else {
                subList.add( sub );
            }
            ++i;
        }
        
        //2nd pass
        i=0;
        for(String arg : subList) {
            String sub = substitute(arg, dict);
            subList.set(i, sub);
            ++i;
        }
        return Collections.unmodifiableList(subList);
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
            substituted = substituted.replaceAll(substitutionParameter, replacement);
            if (substituted.length() == 0 && isOptional) {
                return null;
            }
        }
        return substituted;
    }


}
