package org.genepattern.server.job.input;

/**
 * Created by nazaire on 9/1/15.
 */
public class RangeValuesParser
{
    public RangeValues<Double> parseRange(String rangeValues) throws Exception
    {
        if (rangeValues==null || rangeValues.trim().length()==0) {
            //return null if not set
            return null;
        }
        try {
            Double min=null;
            Double max=null;
            if (rangeValues.endsWith("+")) {
                int idx=rangeValues.indexOf("+");
                String num=rangeValues.substring(0, idx);
                min=Double.parseDouble(num);
                max=null;
            }
            else if (rangeValues.endsWith("-")) {
                int idx=rangeValues.lastIndexOf("-");
                String num=rangeValues.substring(0, idx);
                min=null;
                max=Double.parseDouble(num);
            }
            else if (rangeValues.contains("..")) {
                int idx=rangeValues.indexOf("..");
                final String minStr=rangeValues.substring(0, idx).trim();
                final String maxStr=rangeValues.substring(idx+2).trim();
                min=Double.parseDouble(minStr);
                max=Double.parseDouble(maxStr);
            }
            else {
                min=Double.parseDouble(rangeValues.trim());
                max=min;
            }

            return new RangeValues<Double>(min, max);
        }
        catch (NumberFormatException e) {
            String message="Error parsing range="+rangeValues;
            throw new Exception(message);
        }
    }
}
