/*
 * This software and its documentation are copyright 1999 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */

package org.genepattern.io.parsers;

import java.io.*;
import java.text.*;
import java.util.*;

import org.genepattern.data.Dataset;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.util.*;

import org.genepattern.server.*;

import org.genepattern.data.*;
import org.genepattern.io.*;

/**
 * Can parse gct input streams and creates a Dataset
 * @author  kohm
 */
public class GctParser extends AbstractDatasetParser {
    
//    /** Creates a new instance of GctParser */
//    public GctParser(final InputStream input, final String name,final boolean lenient) throws IOException, org.genepattern.io.ParseException{
//        super(input, name, lenient, FILE_EXTENSIONS);
//    }
//    /** Creates a new instance of GctParser with system defined leniency */
//    public GctParser(final InputStream input, final String name) throws IOException, org.genepattern.io.ParseException{
//        super(input, name, FILE_EXTENSIONS);
//    }
    /** Creates a new instance of GctParser with system defined leniency */
    public GctParser() {
        super(IS_LENIENT, FILE_EXTENSIONS);//this(null, null);
    }
    
    /** creates a new instance of a Concrete implementation of AbstractInnerParser
     * @param reader the line reader
     * @return AbstractInnerParser
     */
    protected AbstractInnerParser createInnerParser(final LineReader reader) {
        return new InnerParser(IS_LENIENT, reader);
    }
	 
	  /** reads the header lines and creates a summary
     * @param in the input stream
     * @throws IOException if an error occures durring an i/o operation
     * @throws org.genepattern.io.ParseException if there is a problem with the format of the data
     * @return SummaryInfo, the summary of some of the attributes of this data
     */
    public SummaryInfo createSummary(final InputStream in) throws IOException, java.text.ParseException {
        final InnerParser parser = (InnerParser) createInnerParser(new LineReader(in));
        Exception exception = null;
		  Map map = new HashMap();
        try {
            //total = 0; // reset
				map.put("Size=", (long)(Math.ceil(in.available()/1024.0)) + " KB");
            parser.getSummary(map);
        } catch (java.text.ParseException ex) {
            exception = ex;
            REPORTER.logWarning(getClass()+" cannot decode stream "+ex.getMessage());
        } catch (NumberFormatException ex) {
            exception = ex;
            REPORTER.logWarning(getClass()+" cannot decode stream "+ex.getMessage());
        } catch (IOException ex) {
            System.err.println("createSummary error:\n");
            ex.printStackTrace();
            throw ex;
        }
        if( exception != null )
            return new SummaryError(null, exception, this, map, new HashMap());
        return new DefaultSummaryInfo(map, new HashMap(), Dataset.DATA_MODEL);
    }
	 
    // fields
    /** indicated a version 1.1 gct file format */    
    public static final String VERSION_1_TOKEN = "#1.1";
    /** indicated a version 1.2 (latest) gct file format */    
    public static final String VERSION_2_TOKEN = "#1.2";
    /** the file extensions String array */
    private static final String[] FILE_EXTENSIONS = new String[] {"gct"};
    
    // I N N E R   C L A S S E S
    /** This is the concrete implementation of the AbstractInnerParser */
    protected final static class InnerParser extends AbstractInnerParser {
    
	
	 
        InnerParser(final boolean is_lenient, final LineReader reader) {
            super(is_lenient, reader);
        }
		  
		  protected void getSummary(Map map) throws IOException, java.text.ParseException, NumberFormatException {
            final LineReader reader = this.reader;
            lines_read = 0;
            // this is trimmed so eliminates problems of whitespace preventing equality
            reader.setExpectEOF(false);
            String curLine = reader.readLine();
            lines_read++;
            
            // version line
            int fileFormat;
            if (curLine.equals(VERSION_1_TOKEN))
                fileFormat = 1;
            else if (curLine.equals(VERSION_2_TOKEN))
                fileFormat = 2;
            else // e.g. if there is no version line
                fileFormat = 0;
            
            if (fileFormat > 0) {
                // only read in a new line if the file had a version string
                curLine = reader.readLine().trim();
                lines_read++;
            }
            
            // 1st header line: <numRows> <tab> <numCols>
            //   put this first so we can allocate arrays for rows and columns before
            //   reading them in
            final String theDelim = "\t";
            
            final int[] hdrInts = new int[2];
            java.util.StringTokenizer st = new java.util.StringTokenizer(curLine, theDelim);
            final int num_tok = st.countTokens();
            if(num_tok > 2) {
                REPORTER.logWarning("The line "+lines_read
                +" should contain two numbers seperated by a tab.\n"
                +"Name: "+dataset_name+"\nline: "+curLine);
            }
            
            int numRows, numCols;
            try {
                for(int i = 0; i < 2 && i < num_tok; i++)
                    hdrInts[i] = Integer.parseInt(st.nextToken().trim());
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("Expecting numbers on line "
                +lines_read+" with format:\n"
                +"number_rows<tab>number_columns\n"
                +getExceptionMessageTrailer());
            }
            
            if (hdrInts[0] > 0) {
                numRows = hdrInts[0];
            } else {
                throw new java.text.ParseException("Missing <rows> value"
                +getExceptionMessageTrailer(), 0);
            }
            
            if (hdrInts[1] > 0) {
                numCols = hdrInts[1];
            } else throw new java.text.ParseException("Missing <columns> value "+getExceptionMessageTrailer(), 0);
				map.put("Rows=", "" + numRows);
				map.put("Columns=", "" + numCols);
		  }
	 
        /** reads the header information and if the primary or seconday maps ar not null saves the attributes
         * @param primary where the primary summay info is stored
         * @param secondary where the summay info of secondary importance is stored
         * @throws org.genepattern.io.ParseException if a format problem was detected in the data
         * @throws NumberFormatException if couldn't parse a number where a number was expected
         * @throws IOException if error occurs during an I/O operation
         * @return File if a temp file is needed otherwise null
         */
        protected File readHeader(final Map primary, final Map secondary) throws java.text.ParseException, NumberFormatException, IOException  {
            final LineReader reader = this.reader;
            lines_read = 0;
            // this is trimmed so eliminates problems of whitespace preventing equality
            reader.setExpectEOF(false);
            String curLine = reader.readLine();
            lines_read++;
            
            // version line
            int fileFormat;
            if (curLine.equals(VERSION_1_TOKEN))
                fileFormat = 1;
            else if (curLine.equals(VERSION_2_TOKEN))
                fileFormat = 2;
            else // e.g. if there is no version line
                fileFormat = 0;
            if(secondary != null) secondary.put("GCT version=", new Integer(fileFormat));
            
            if (fileFormat > 0) {
                // only read in a new line if the file had a version string
                curLine = reader.readLine().trim();
                lines_read++;
            }
            
            // 1st header line: <numRows> <tab> <numCols>
            //   put this first so we can allocate arrays for rows and columns before
            //   reading them in
            final String theDelim = "\t";
            
            final int[] hdrInts = new int[2];
            java.util.StringTokenizer st = new java.util.StringTokenizer(curLine, theDelim);
            final int num_tok = st.countTokens();
            if(num_tok > 2) {
                REPORTER.logWarning("The line "+lines_read
                +" should contain two numbers seperated by a tab.\n"
                +"Name: "+dataset_name+"\nline: "+curLine);
            }
            
            int numRows, numCols;
            try {
                for(int i = 0; i < 2 && i < num_tok; i++)
                    hdrInts[i] = Integer.parseInt(st.nextToken().trim());
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("Expecting numbers on line "
                +lines_read+" with format:\n"
                +"number_rows<tab>number_columns\n"
                +getExceptionMessageTrailer());
            }
            
            if (hdrInts[0] > 0) {
                numRows = hdrInts[0];
                if(primary != null) primary.put("Rows=", new Integer(numRows));
            } else {
                throw new java.text.ParseException("Missing <rows> value"
                +getExceptionMessageTrailer(), 0);
            }
            
            if (hdrInts[1] > 0) {
                numCols = hdrInts[1];
                if(primary != null) primary.put("Columns=", new Integer(numCols));
            } else throw new java.text.ParseException("Missing <columns> value "+getExceptionMessageTrailer(), 0);
            
            setRowColumnCounts(numRows, numCols);
            
            if (fileFormat >= 2) {
                // columns names: (required in Version2 and above, starts with '#')
                // # <"Name"> <tab> <"Desc"> <tab> <col 0 name> <tab> <col 1 name> <tab> <...>
                curLine = reader.readLine();
                lines_read++;
                //System.out.println("Current line:\n"+curLine);
                
                st = new java.util.StringTokenizer(curLine, theDelim);
                if(st.nextToken().trim().equals("#"))  // pop the "Name" column label or "#"
                    st.nextToken(); // pop "name"
                String desc = st.nextToken();   // pop the "Description" column label
                //System.out.println("Desc: "+desc);
                
                final String[] strArr = column_labels;
               // if(primary != null) primary.put("COLUMN_NAMES:", strArr);
                final int tok_count = st.countTokens();
                if(tok_count != strArr.length) {
                    throw new java.text.ParseException((tok_count > strArr.length ? "More" : "Less")
                    +" column names("+st.countTokens()
                    +") than number of columns specified("+strArr.length+")\n"
                    +"Example format: Name <TAB> Description <TAB> column 0 name <TAB> col 1 name <TAB> cancer sample 02"
                    +getExceptionMessageTrailer(), 0);
                }
                
                for (int colInd = 0; st.hasMoreTokens(); colInd++) {
                    strArr[colInd] = st.nextToken();
                }
                
            } else { // the column labels are filled with nulls
                java.util.Arrays.fill(column_labels, "C");// they must not be null for BPOG
            }
            reader.setExpectEOF(true);
            return null;
        }
        
        /** the start number for the first column
         * @return the column index where the data begins
         */
        protected final int getStartColumn() {
            return 1;
        }
        /** get the max number of tokens exzpected on a line
         * @return int max tokens/line
         */
        protected final int getMaxNumEntriesPerLine() {
            return (2 + this.column_count);
        }
        
        /** processes each line
         * @param row the row index
         * @param tokens array of tokens from the line
         * @param start start column
         * @param end end column
         * @throws NumberFormatException if a float was not parsable
         */
        protected final void processDataTokens(final int row, final String[] tokens, final int start, final int end) throws NumberFormatException{
            final int r = row -1;
            int c = 0;
            int i = start;
            try {
                for(; i <= end; i++) {
                    //System.out.println("limit="+limit+" c="+c+" i="+i);
                    setDataAt(r, c++, parse(tokens[i], r, c));
                }
            } finally { // for debug purposes (current_column is used in the Exception catch block)
                current_column = i;
            }
        }
        
        /** final check to make sure the data is ok
         * @param row the row index where the end of file occured
         */
        protected final void processEOF(int row) {
        }
        
        /** returns true if the label column is before the description column
         * @return true if label is before description column
         */
        protected final boolean isLabelFirst() {
            return true;
        }
    }// end InnerParser
    
}
