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
    
    public static List<String> createCmdLine(String cmdLine, Properties props, ParameterInfo[] formalParameters) { 
        Map<String,String> env = new HashMap<String,String>();
        for(Object keyObj : props.keySet()) {
            String key = keyObj.toString();
            env.put( key.toString(), props.getProperty(key));
        }
        Map<String,ParameterInfo> parameterInfoMap = createParameterInfoMap(formalParameters);
        return resolveValue(cmdLine, env, parameterInfoMap, 0);
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

    private static List<String> resolveValue(final String value, final Map<String,String> props, final Map<String,ParameterInfo> parameterInfoMap, final int depth) {
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
            List<String> substitution = substituteValue(token, props, parameterInfoMap);
            
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
                    List<String> resolvedList = resolveValue( singleValue, props, parameterInfoMap, 1+depth );
                    rval.addAll( resolvedList );
                }
            }
            else {
                for(String sub : substitution) {
                    List<String> resolvedSub = resolveValue(sub, props, parameterInfoMap, 1+depth);
                    rval.addAll( resolvedSub );
                }
            }
        }
        return rval;
    }
    
    private static List<String> substituteValue(final String arg, final Map<String,String> dict, final Map<String,ParameterInfo> parameterInfoMap) {
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

    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> props) {
        final Map<String,ParameterInfo> emptyParameterInfoMap = Collections.emptyMap();
        return resolveValue(cmdLine, props, emptyParameterInfoMap, 0);
    }
    
    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> props, final Map<String,ParameterInfo> parameterInfoMap) {
        return resolveValue(cmdLine, props, parameterInfoMap, 0);
    }
    
    
    ///legacy code, originally implemented in GenePatternAnalysisTask
//    private static Map<String,ParameterInfo> getParameterInfoMap(final ParameterInfo[] parameterInfoArray) {
//        List<ParameterInfo> list = new ArrayList<ParameterInfo>();
//        for(ParameterInfo param : parameterInfoArray) {
//            list.add(param);
//        }
//        return getParameterInfoMap(list);
//    }
//
//    private static Map<String,ParameterInfo> getParameterInfoMap(final List<ParameterInfo> parameterInfoList) {
//        Map<String,ParameterInfo> map = new LinkedHashMap<String,ParameterInfo>();
//        for(ParameterInfo param : parameterInfoList) {
//            map.put(param.getName(), param);
//        }
//        return map;
//    }
//    
//  /**
//  * Create the command line to run.
//  * 
//  * @param cmdLine, the raw command line from the module's manifest.
//  * @param props, a lookup table for mapping variable names to substitution values.
//  * @param formalParameters, the list of ParameterInfo for the module.
//  * 
//  * @return the list of command line arguments, including the executable as the first item.
//  * 
//  * @throws Exception
//  */
// private static String[] createCmdArray(String cmdLine, Properties props, ParameterInfo[] formalParameters) 
// throws CommandLineParser.Exception
// {
//     List<String> commandTokens = new ArrayList<String>();
//
//     // TODO: handle quoted arguments within the command line (eg. echo "<p1> <p2>" as a single token)
//
//     // check that the user didn't quote the program name
//     if (!cmdLine.startsWith("\"")) {
//         // since we could have a definition like "<perl>=perl -Ifoo",
//         // we need to double-tokenize the first token to extract just "perl"
//         StringTokenizer stCommandLine = new StringTokenizer(cmdLine);
//         String firstToken = stCommandLine.nextToken();
//         // now the command line contains the real first word (perl)
//         // followed by the rest, ready for space-tokenizing
//         cmdLine = substitute(firstToken, props, formalParameters) + cmdLine.substring(firstToken.length());
//         stCommandLine = new StringTokenizer(cmdLine);
//         while(stCommandLine.hasMoreTokens()) {
//             String token = stCommandLine.nextToken();
//             String substitutedToken = substitute(token, props, formalParameters);
//             if (substitutedToken != null) {
//                 commandTokens.add(substitutedToken);
//             }
//         }
//     } 
//     else {
//         // the user quoted the command, so it has to be handled specially
//         int endQuote = cmdLine.indexOf("\"", 1);
//         // find the matching closing quote
//         if (endQuote == -1) {
//             //vProblems.add("Missing closing quote on command line: " + cmdLine);
//             throw new CommandLineParser.Exception("Missing closing quote on command line: " + cmdLine);
//         } 
//         String firstToken = cmdLine.substring(1, endQuote);
//         commandTokens.add(substitute(firstToken, props, formalParameters));
//         StringTokenizer stCommandLine = new StringTokenizer(cmdLine.substring(endQuote + 1));
//         while(stCommandLine.hasMoreTokens()) {
//             String token = stCommandLine.nextToken();
//             String substitutedToken = substitute(token, props, formalParameters);
//             if (substitutedToken != null) {
//                 commandTokens.add(substitutedToken);
//             }
//         }
//     }
//
//     // do the substitutions one more time to allow, for example, p2=<p1>.res
//     List<String> secondPass = new ArrayList<String>();
//     for(String token : commandTokens) {
//         String substitutedToken = substitute(token, props, formalParameters);
//         if (substitutedToken != null) {
//             secondPass.add(substitutedToken);
//         }
//     }
//     String[] commandArray = new String[secondPass.size()];
//     commandArray = secondPass.toArray(commandArray);
//     return commandArray;
// }

//    /**
//     * Replace all substitution variables, of the form &lt;variable&gt;, with values from the given properties instance.
//     * Prepend the prefix for any substituted parameters which have a prefix.
//     * 
//     * @param commandLine from the manifest for the module
//     * @param props, Properties object containing name/value pairs for parameter substitution in the command line
//     * @param params, ParameterInfo[] describing whether each parameter has a prefix defined.
//     * 
//     * @return String command line with all substitutions made
//     * 
//     * @author Jim Lerner, Peter Carr
//     */
//    private static String substitute(final String commandLine, final Properties props, final ParameterInfo[] params) {
//        if (commandLine == null) {
//            return null;
//        }
//        List<String> substituted = substitute(commandLine, props, params, false);
//        if (substituted == null || substituted.size() == 0) {
//            return null;
//        }
//        else if (substituted.size()==1) {
//            return substituted.get(0);
//        }
//        //unexpected, rval contains more than one item
//        log.error("Unexpected rval in substitute, expecting a single item list, but the list contained "+substituted.size()+" items.");
//        String rval = "";
//        boolean first = true;
//        for(String s : substituted) {
//            if (first) {
//                first = false;
//            }
//            else {
//                s = " "+s;
//            }
//            rval += s;
//        }
//        return rval;
//    }
    
//    private static List<String> substitute(final String commandLine, final Properties props, final ParameterInfo[] params, final boolean split) {
//        if (commandLine == null) {
//            return null;
//        }
//        String substituted = commandLine;
//        
//        Map<String, ParameterInfo> parameterInfoMap = getParameterInfoMap(params);
//        List<String> substitutionParameters = getSubstitutionParameters(commandLine);
//        
//        for(String substitutionParameter : substitutionParameters) {
//            String varName = substitutionParameter.substring(1, substitutionParameter.length() - 1);
//            //default to empty string, to handle optional parameters which have not been set
//            String replacement = props.getProperty(varName, "");
//            if (varName.equals("resources")) {
//                //TODO: this should really be in the setupProps
//                //special-case for <resources>, 
//                // make this an absolute path so that pipeline jobs running in their own directories see the right path
//                replacement = new File(replacement).getAbsolutePath();
//            }
//            ParameterInfo paramInfo = parameterInfoMap.get(varName);
//            boolean isOptional = true;
//            HashMap hmAttributes = null;
//            if (paramInfo != null) {
//                hmAttributes = paramInfo.getAttributes();
//            }
//            if (hmAttributes != null) {
//                if (hmAttributes.get(PARAM_INFO_OPTIONAL[PARAM_INFO_NAME_OFFSET]) == null) {
//                    isOptional = false;
//                }
//                String optionalPrefix = (String) hmAttributes.get(PARAM_INFO_PREFIX[PARAM_INFO_NAME_OFFSET]);
//                if (replacement.length() > 0 && optionalPrefix != null && optionalPrefix.length() > 0) {
//                    if (optionalPrefix.endsWith(" ")) {
//                        //special-case: GP-2866
//                        //    if optionalPrefix ends with a space, split into two args
//                    }
//                    replacement = optionalPrefix + replacement;
//                }
//            }
//            substituted = substituted.replace(substitutionParameter, replacement);
//            if (substituted.length() == 0 && isOptional) {
//                return null;
//            }
//        }
//        //return substituted;
//        List<String> rval = new ArrayList<String>();
//        rval.add(substituted);
//        return rval;
//    }

}
