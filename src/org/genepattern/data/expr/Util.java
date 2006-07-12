/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.data.expr;

/**
 * 
 * @author Joshua Gould
 * 
 */
public class Util {

    /**
     * 
     * @param data
     * @param name
     * @return
     */
    public static boolean containsData(IExpressionData data, String name) {
        for (int i = 0, size = data.getDataCount(); i < size; i++) {
            if (data.getDataName(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param data
     * @param name
     * @return
     */
    public static boolean containsRowMetadata(IExpressionData data, String name) {
        for (int i = 0, size = data.getRowMetadataCount(); i < size; i++) {
            if (data.getRowMetadataName(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param data
     * @param name
     * @return
     */
    public static boolean containsColumnMetadata(IExpressionData data,
            String name) {
        for (int i = 0, size = data.getColumnMetadataCount(); i < size; i++) {
            if (data.getColumnMetadataName(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

}
