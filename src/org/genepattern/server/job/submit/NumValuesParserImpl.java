package org.genepattern.server.job.submit;

/**
 * Initialize a NumValues instance from the numValues property in the manifest.
 * Examples:
 *     //min is set, max is not set
 *     numValues=0+
 *     numValues=1+
 *     numValues=2+
 *     
 *     //min and max are the same
 *     numValues=1
 *     numValues=2
 *     
 *     //a range of values
 *     numValues=0..1
 *     numValues=0..2
 *     numValues=1..4
 *     numValues=2..4
 *     
 * @author pcarr
 *
 */
public class NumValuesParserImpl implements NumValuesParser {
    public NumValues parseNumValues(final String numValues) throws Exception {
        if (numValues==null || numValues.trim().length()==0) {
            //return null if not set
            return null;
        }
        try {
            Integer min=null;
            Integer max=null;
            if (numValues.endsWith("+")) {
                int idx=numValues.indexOf("+");
                String num=numValues.substring(0, idx);
                min=Integer.parseInt(num);
                max=null;
            }
            else if (numValues.contains("..")) {
                int idx=numValues.indexOf("..");
                final String minStr=numValues.substring(0, idx).trim();
                final String maxStr=numValues.substring(idx+2).trim();
                min=Integer.parseInt(minStr);
                max=Integer.parseInt(maxStr);
            }
            else {
                min=Integer.parseInt(numValues.trim());
                max=min;
            }
            return new NumValues(min, max);
        }
        catch (NumberFormatException e) {
            String message="Error parsing numValues="+numValues;
            throw new Exception(message);
        }
    }
}

