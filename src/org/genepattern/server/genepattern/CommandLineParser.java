package org.genepattern.server.genepattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

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

    private static String substitute(Map<String,String> dict, String arg) {
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

    public static List<String> translateCmdLine(final Map<String,String> dict, String cmdLine) {
        //break up the command line into tokens
        List<String> argList = tokenizeCmdLine(cmdLine);
        List<String> subList = new ArrayList<String>();
        
        //1st pass
        int i=0;
        for(String arg : argList) {
            String sub = substitute(dict, arg);
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
            String sub = substitute(dict, arg);
            subList.set(i, sub);
            ++i;
        }
        return Collections.unmodifiableList(subList);
    }
}
