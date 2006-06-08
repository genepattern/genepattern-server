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
 * Contains data constants
 * 
 * @author Joshua Gould
 * 
 */
public class ExpressionConstants {
    /** Description key */
    public static final String DESC = "description";

    /** Calls key */
    public static final String CALLS = "calls";

    /** Affy Present Call key */
    public static final String PRESENT = "P";

    /** Affy Absent Call key */
    public static final String ABSENT = "A";

    /** Affy Marginal Call key */
    public static final String MARGINAL = "M";

    /** Chromosome key */
    public static final String CHROMOSOME = "Chromosome";

    /** Physical Position key */
    public static final String PHYSICAL_POSITION = "PhysicalPosition";

    private ExpressionConstants() {
    }

}
