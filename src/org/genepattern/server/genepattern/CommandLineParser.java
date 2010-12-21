package org.genepattern.server.genepattern;

import static org.genepattern.util.GPConstants.PARAM_INFO_NAME_OFFSET;
import static org.genepattern.util.GPConstants.PARAM_INFO_OPTIONAL;
import static org.genepattern.util.GPConstants.PARAM_INFO_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    throws Exception
    {
        StringTokenizer stCommandLine;
        String[] commandTokens = null;
        String firstToken;
        String token;

        // TODO: handle quoted arguments within the command line (eg. echo "<p1> <p2>" as a single token)

        // check that the user didn't quote the program name
        if (!cmdLine.startsWith("\"")) {
            // since we could have a definition like "<perl>=perl -Ifoo",
            // we need to double-tokenize the first token to extract just "perl"
            stCommandLine = new StringTokenizer(cmdLine);
            firstToken = stCommandLine.nextToken();
            // now the command line contains the real first word (perl)
            // followed by the rest, ready for space-tokenizing
            cmdLine = substitute(firstToken, props, formalParameters) + cmdLine.substring(firstToken.length());
            stCommandLine = new StringTokenizer(cmdLine);
            commandTokens = new String[stCommandLine.countTokens()];
            for (int i = 0; stCommandLine.hasMoreTokens(); i++) {
                token = stCommandLine.nextToken();
                commandTokens[i] = substitute(token, props, formalParameters);
                if (commandTokens[i] == null) {
                    String[] copy = new String[commandTokens.length - 1];
                    System.arraycopy(commandTokens, 0, copy, 0, i);
                    if ((i + 1) < commandTokens.length) {
                        System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
                    }
                    commandTokens = copy;
                    i--;
                }
            }
        } 
        else {
            // the user quoted the command, so it has to be handled specially
            int endQuote = cmdLine.indexOf("\"", 1);
            // find the matching closing quote
            if (endQuote == -1) {
                //vProblems.add("Missing closing quote on command line: " + cmdLine);
                throw new Exception("Missing closing quote on command line: " + cmdLine);
            } 
            else {
                firstToken = cmdLine.substring(1, endQuote);
                stCommandLine = new StringTokenizer(cmdLine.substring(endQuote + 1));
                commandTokens = new String[stCommandLine.countTokens() + 1];
                commandTokens[0] = substitute(firstToken, props, formalParameters);
                for (int i = 1; stCommandLine.hasMoreTokens(); i++) {
                    token = stCommandLine.nextToken();
                    commandTokens[i] = substitute(token, props, formalParameters);
                    // empty token?
                    if (commandTokens[i] == null) {
                        String[] copy = new String[commandTokens.length - 1];
                        System.arraycopy(commandTokens, 0, copy, 0, i);
                        if ((i + 1) < commandTokens.length) {
                            System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
                        }
                        commandTokens = copy;
                        i--;
                    }
                }
            }
        }

        // do the substitutions one more time to allow, for example, p2=<p1>.res
        for (int i = 1; i < commandTokens.length; i++) {
            commandTokens[i] = substitute(commandTokens[i], props, formalParameters);
            if (commandTokens[i] == null) {
                String[] copy = new String[commandTokens.length - 1];
                System.arraycopy(commandTokens, 0, copy, 0, i);
                if ((i + 1) < commandTokens.length) {
                    System.arraycopy(commandTokens, i + 1, copy, i, commandTokens.length - i - 1);
                }
                commandTokens = copy;
                i--;
            }
        }
        
        return commandTokens;
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
            rval.addAll( resolveValue( substitution, dict, 1+depth ) );
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
    
    public static List<String> translateCmdLine(final String cmdLine, final Map<String,String> in) throws IOException {
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
        return substituted;
    }


}
