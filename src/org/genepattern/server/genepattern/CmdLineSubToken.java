package org.genepattern.server.genepattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/** 
 * Helper data structure for holding the results of parsing a String into a list of zero or more 'tokens' 
 * E.g.
 *     "-f<input.arg>" is split into two SubTokens ["-f" (literal) and "<input.arg>" (non-literal)]
 */
public class CmdLineSubToken {
    private static final Logger log = Logger.getLogger(CmdLineSubToken.class);

    // old way: String patternRegex = "<[-|\\w|\\.&^\\s]*>";
    private static final String patternRegex = "(<[^<^ ][^>&^ ]*>)";
    private static final Pattern pattern = initPattern();
    private static Pattern initPattern() {
        try {
            return Pattern.compile(patternRegex);
        }
        catch (PatternSyntaxException e) {
            log.error("Error creating pattern for: "+patternRegex, e);
        }
        catch (Throwable t) {
            log.error("Error creating pattern for: '"+patternRegex+": "+t.getLocalizedMessage() );
        }
        return null;
    }

    /**
     * Split the given string into a list of zero or more CmdLineSubTokens.
     * Each sub token can be either a literal value or a substitution.
     * 
     * For example, the string "A<arg.a> B <arg.b> POSTFIX" is split into 5 tokens:
     *     "A", "<arg.a>", " B ", "<arg.b>", " POSTFIX"
     * 
     * @param str
     * @return a List of CmdLineSubToken, each parameter item includes the enclosing '<' and '>' brackets.
     */
    public static List<CmdLineSubToken> splitIntoSubTokens(final String str) {
        if (Strings.isNullOrEmpty(str)) {
            return Collections.emptyList();
        }
        if (pattern==null) {
            log.error("pattern==null, returning literal value");
            return Arrays.asList(new CmdLineSubToken(str));
        }
        final List<CmdLineSubToken> paramNames = new ArrayList<CmdLineSubToken>();
        final Matcher matcher = pattern.matcher(str);
        int lhidx=0; // to track non-matching regions
        if(!matcher.find()) {
            // no matches
            paramNames.add(new CmdLineSubToken(str));
            return paramNames;
        }
        matcher.reset();
        while(matcher.find()) {
            int sidx = matcher.start(); // start index of match
            int eidx = matcher.end();   // end index of match
            if (lhidx<sidx) {
                // end index of last match is < start of next match, add 'non-matching' region
                paramNames.add(new CmdLineSubToken(str.substring(lhidx, sidx)));
            }
            lhidx=eidx;
            paramNames.add(new CmdLineSubToken(str.substring(sidx, eidx)));
        }
        if (lhidx<str.length()-1) {
            // end index of last match is < length of string, add 'non-matching' region
            paramNames.add(new CmdLineSubToken(str.substring(lhidx, str.length())));
        }
        return paramNames;
    }
        
    final String value;
    final boolean isLiteral;
    final String pname;
    public CmdLineSubToken(final String value) {
        this.value=value;
        if (value==null) {
            isLiteral=true;
        }
        else {
            isLiteral = ! (value.startsWith("<") && (value.endsWith(">")));
        }
        if (isLiteral) {
            pname=null;
        }
        else {
            pname=value.substring(1, value.length()-1);
        }
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj) {
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CmdLineSubToken other = (CmdLineSubToken) obj;
        return Objects.equal(value, other.value);
    }

}