/*
 * This software and its documentation are copyright 2002 by the
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
/*
 * AbstractPropertiesEncoder.java
 *
 * Created on January 17, 2003, 1:54 PM
 */

package org.genepattern.io.encoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Map;

import org.genepattern.data.FeaturesetProperties;
import org.genepattern.io.Remark;

import org.genepattern.io.*;

/**
 * Encodes the data in the Self Documenting Format (SDF) which is also called
 * OmniSci data format (ODF).  The older sdf file extension is deprecated since 
 * it is the same as a file format for molecules.  The new extension, .odf, will
 * be used for all newly created files.
 *
 * @author  kohm
 */
abstract public class AbstractPropertiesEncoder implements Encoder {
    
    /** Creates a new instance of AbstractPropertiesEncoder
     * @param properties the featureset properties
     */
    public AbstractPropertiesEncoder(final FeaturesetProperties properties) {
        this.properties = properties;
    }
    
    // abstract methods
//    /** gets the property definition or note */
//    abstract protected String getNote();
    /** the String that represents the type of Model For example:
     * "Prediction Results" "Gene Properties"  "Sample Properties" "Dataset"
     * @return String, text representation of the model
     */
    abstract protected String getModel();
    
    // Encoder method signature
    /** encodes the data to the output stream
     * @param out the output stream to write to
     * @throws IOException if exception occurs durring I/O
     */
    public void write(final OutputStream out) throws IOException {
        write(new OutputStreamWriter(out));
    }
    // Helper methods
    /** encodes the data to the writer
     * @param writer the writer to write text to
     * @throws IOException if exception occurs durring I/O
     */
    public void write (final Writer writer) throws IOException {
        //final FeaturesetProperties 
        // determine the number of header lines
        final Map attrs = new java.util.HashMap();
        final Remark[] remarks = getRemarksAndAttributes(attrs);
        final int h_cnt = ((remarks == null || remarks.length == 0) ? attrs.size() : attrs.size() - 1) + getStdHeaderCount();
        try {
            final PrintWriter out = new PrintWriter(new BufferedWriter(writer));
            
            // ODF/SDF 1.0
            writeMarkerLine(out); // SDF 1.0
            writeHeaderCount(out, h_cnt); // HeaderLines= int
            // each of the header lines
            writeHeaders(out, attrs, remarks);
            // write the body of the main data
            writeMainData(out);
            
          //  System.out.println("Done encoding a properties file.");
            out.close();            
        } catch (IOException e){
            e = new IOException("Error: while writing featureset properties  "+properties.getName()+":\n"+e.getMessage());
            e.fillInStackTrace();
            throw e;
        }
    }
    /** writes the SDF marker line
     * @param out the output stream to write a line to
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeMarkerLine(final PrintWriter out) throws IOException {
        // required first line  
        // ODF/SDF version_number  "SDF 1.0"
        out.print(MARKER_TAG);
        out.print(' ');
        out.println(CURRENT_SDF_VERSION);
    }
    /** records the number of header lines
     * @param out the output stream to write to
     * @param cnt the header count
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeHeaderCount(final PrintWriter out, final int cnt) throws IOException {
        out.print(this.KW_HEADER_LINES);
        out.println(cnt);
    }
    /** writes the headers and attributes
     * @param out the output stream to write to
     * @param attrs the header information
     * @param remarks an array of remarks to record
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeHeaders(final PrintWriter out, final Map attrs, final Remark[] remarks) throws IOException {
        //FIXME need to get the remarks and insert them in the proper places
        writeModel(out);              //        The model
        writeColumnHeaders(out);      //        Column header row (Names)
        writeColumnTypes(out);        //        data_type one for each column
        writeColumnsDescriptions(out);//        Column description row
        writeRowNameDescriptions(out);//        which columns to put the names, desc
        writeAttributes(out, attrs);  //        attributes
        writeDataCount(out);          //        num_rows of main data
    }
    /** writes the notes or desciption
     * @param out the output stream to write to
     * @param note the text of the remark
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeComment(final PrintWriter out, final String note) throws IOException {
        writeLine(KW_REMARK, note, out);
    }
    /** writes the row count
     * @param out he output stream to write to
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeDataCount(final PrintWriter out) throws IOException {
        out.print(KW_DATA_LINES);
        out.print(delimiter);
        out.println(properties.getRowCount());
    }
    /** writes the model
     * @param out he output stream to write to
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeModel(final PrintWriter out) throws IOException {
        writeLine(KW_MODEL, getModel(), out);
    }
    /** writes the types of the columns
     * @param out the output stream
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeColumnTypes(final PrintWriter out) throws IOException {
        final int limit = properties.getColumnCount();
        out.print(KW_COLUMN_TYPES);
        for(int i = 0; i < limit; i++) {
            out.print(delimiter);
            out.print((String)classToType.get(properties.getColumnClass(i)));
        }
        out.println();
    }
    /** writes the column names
     * @param out the output stream
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeColumnHeaders(final PrintWriter out) throws IOException {
        out.print(KW_COLUMN_NAMES);
        final int limit = properties.getColumnCount();
        for(int i = 0; i < limit; i++) {
            out.print(delimiter);
            out.print(properties.getColumnName(i));
        }
        out.println();
    }
    /** writes the column descriptions if any
     * @param out the output stream
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeColumnsDescriptions(final PrintWriter out) throws IOException {
        if(properties.hasColumnDescriptions()) {
            out.print(KW_COLUMN_DESCRIPTIONS);
            final int limit = properties.getColumnCount();
            for(int i = 0; i < limit; i++) {
                final String desc = properties.getColumnDescription(i);
                out.print(delimiter);
                out.print(desc.trim());
            }
        }
        out.println();
    }
    /** writes the rest of the values in the table
     * @param out the output stream
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeRowNameDescriptions(final PrintWriter out) throws IOException {
        int row = -1;
        if( properties.hasRowNames() ) {
            row++;
            this.row_names_column = row;
            out.print(KW_ROW_NAMES_COLUMN);
            out.println(row_names_column);
        }
        if( properties.hasRowDescriptions() ) {
            row++;
            this.row_descr_column = row;
            out.print(KW_ROW_DESCRIPTIONS_COLUMN);
            out.println(row_descr_column);
        }
    }
    /** writes the attributes if any
     * @param out the output stream
     * @param attrs the header information
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeAttributes(final PrintWriter out, final Map attrs) throws IOException {
        final int cnt = attrs.size();
        java.util.Set keys = attrs.keySet();
        for(java.util.Iterator iter = keys.iterator(); iter.hasNext();) {
            final Object key = iter.next();
            final Object value = attrs.get(key);
            final Class clss = value.getClass();
            if( clss.isArray() ) {
                writeArray(key.toString(), value, out);
            } else {
                out.print(key);
                out.print(delimiter);
                out.println(value);
            }
        }
    }
    /** writes the array to output
     * @param kw the key word
     * @param array the array of primitives or objects
     * @param out the writer to record data to
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected final void writeArray(final String kw, final Object array, final PrintWriter out) throws IOException {
        out.print(kw);
        if( array instanceof Object[] ) {
            writeObjArray((Object[])array, out);
        } else if( array instanceof int[] ) {
            writeIntArray((int[])array, out);
        } else if( array instanceof float[] ) {
            writeFloatArray((float[])array, out);
        } else if( array instanceof double[] ) {
            writeDoubleArray((double[])array, out);
        } else if( array instanceof boolean[] ) {
            writeBooleanArray((boolean[])array, out);
        } else
            throw new UnsupportedOperationException("Need to implement saving of arrays of type "+array.getClass()+"!");
        out.println();
    }
    /** writes the rest of the values in the table
     * @param out the output stream
     * @throws IOException if exception occurs durring I/O
     */
    protected void writeMainData(final PrintWriter out) throws IOException {
        final boolean has_row_names = (row_names_column >= 0);
        final boolean has_row_descs = (row_descr_column >= 0);
        final int num_cols = properties.getColumnCount();
        final int num_rows = properties.getRowCount();
        for(int r = 0; r < num_rows; r++) {
            // assumes that if has row names that it will be first
            if( has_row_names ) {
                out.print(properties.getRowName(r));
                out.print(delimiter);
            }
            // also assumes that if has row descriptions that it will be next
            if( has_row_descs ) {
                out.print(properties.getRowName(r));
                out.print(delimiter);
            }
            for(int c = 0; c < num_cols; c++) {
                out.print(properties.getValueAt(r, c));
                out.print(delimiter);
            }
            out.println();
        }
        
    }
    /** writes the kwy word and value */
    private void writeLine(final String kw, final String val, final PrintWriter out) throws IOException {
        out.print(kw);
        out.print(delimiter);
        out.println(val);
    }
    // info methods used by the writerXxx(...) methods
    
    /** gets the standard header count
     * @return returns the number of header lines
     */
    protected int getStdHeaderCount() {
        /*
        KW_REMARK                   // not counted
        KW_COLUMN_DESCRIPTIONS      // may not have
        KW_COLUMN_NAMES             // yes
        KW_COLUMN_TYPES             // yes
        KW_DELIMITER                // no just use the default
        KW_DATA_LINES               // yes
        KW_ROW_DESCRIPTION_COLUMN   // maybe
        KW_ROW_NAMES_COLUMN         // maybe
        KW_MODEL                    // yes
         */
        int total = 4;
        if( properties.hasColumnDescriptions() )
            total++;
        if( properties.hasRowNames() )
            total++;
        if( properties.hasRowDescriptions() )
            total++;
        return total;
    }
    /** seperates the remarks from the other attributes
     * @param map the map object to set attributes
     * @return Remark[], all the remarks
     */
    protected Remark[] getRemarksAndAttributes(final Map map) {
        java.util.List remarks = null;
        final Map attrs = properties.getAttributes();
        final java.util.Set keys = attrs.keySet();
        for(java.util.Iterator iter = keys.iterator(); iter.hasNext();) {
            final Object key = iter.next();
            final Object value = attrs.get(key);
            if( KW_REMARK.equals(key) ) {
                remarks = (java.util.List)value;
            } else {
                map.put(key, value);
            }
        } 
        if( remarks == null )
            return null;
        
        return (Remark[])remarks.toArray(new Remark[remarks.size()]);
    }
    /** 
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeObjArray(final Object[] objs, final PrintWriter out) throws IOException {
        final int limit = objs.length;
        for (int i = 0; i < limit; ++i) {
            out.print(delimiter);
            out.print(objs[i].toString().trim());
        }
    }
    /** 
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeIntArray(final int[] vals, final PrintWriter out) throws IOException {
        final int limit = vals.length;
        for (int i = 0; i < limit; ++i) {
            out.print(delimiter);
            out.print(vals[i]);
        }
    }
    /** 
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeFloatArray(final float[] vals, final PrintWriter out) throws IOException {
        final int limit = vals.length;
        for (int i = 0; i < limit; ++i) {
            out.print(delimiter);
            out.print(vals[i]);
        }
    }
    /** 
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeDoubleArray(final double[] vals, final PrintWriter out) throws IOException {
        final int limit = vals.length;
        for (int i = 0; i < limit; ++i) {
            out.print(delimiter);
            out.print(vals[i]);
        }
    }
    /** 
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeBooleanArray(final boolean [] vals, final PrintWriter out) throws IOException {
        final int limit = vals.length;
        for (int i = 0; i < limit; ++i) {
            out.print(delimiter);
            out.print(vals[i]);
        }
    }
    // fields
    /** maps the class to the type */
    private static final java.util.Map classToType;
    /** the magic string label this stream as sdf/odf format */
    public static final String MARKER_TAG = "ODF";
    /** the major version number */
    public static final int SDF_VERSION_MAJOR = 1;
    /** the minor version number */
    public static final int SDF_VERSION_MINOR = 0;
    /** the version tag */
    public static final String CURRENT_SDF_VERSION = SDF_VERSION_MAJOR+"."+SDF_VERSION_MINOR;
    /** the delimiter used to seperate values */
    private final char delimiter = '\t'; // tab
    /** the Feature or properties object */
    public final FeaturesetProperties properties;
    /** the column index for where to store the row names or -1 if NA */
    private int row_names_column = -1;
    /** the column index where the row descriptions will go or -1 if NA */
    private int row_descr_column = -1;
    
    //Key Words 
    //FIXME this should be put in a super class or interface of this and AbstractPropertiesDecoder
    /** Lines that begin with this character are considered comments or remark lines */
    public static final String KW_REMARK = "#";
    /** Defines the number of header line (not counting remarks) REQUIRED 2nd line */
    public static final String KW_HEADER_LINES = "HeaderLines=";
    /** The descriptions of each column */
    public static final String KW_COLUMN_DESCRIPTIONS = "COLUMN_DESCRIPTIONS:";
    /** The names of each column */
    public static final String KW_COLUMN_NAMES = "COLUMN_NAMES:";
    /** The types of each column if not defined defaults to String for each */
    public static final String KW_COLUMN_TYPES = "COLUMN_TYPES:";
    /** defines the characters that delimit values on a line */
    public static final String KW_DELIMITER = "DELIMITER:";
    /** the number of data lines that follow the header */
    public static final String KW_DATA_LINES = "DataLines=";
    /** The index of the column where row descriptions are */
    public static final String KW_ROW_DESCRIPTIONS_COLUMN = "RowDescriptionsColumn=" ;
    /** The index of the column where row names are */
    public static final String KW_ROW_NAMES_COLUMN = "RowNamesColumn=";
    /** A list of row descriptions */
    public static final String KW_ROW_DESCRIPTIONS = "ROW_DESCRIPTIONS:";
    /** The name of the model this data represents */
    public static final String KW_MODEL = "Model=";
//    /** */
//    public static final String KW_ = ;
//    /** */
//    public static final String KW_ = ;
    /** static initializer */
    static {
        final int limit = FeaturesetProperties.SUPPORTED_CLASSES.length;
        if(limit != FeaturesetProperties.SUPPORTED_TYPES.length)
            throw new ArrayIndexOutOfBoundsException("Internal error: unequal array lengths!\n"
                +"SUPPORTED_CLASSES["+limit+"] "
                +"SUPPORTED_TYPES["+FeaturesetProperties.SUPPORTED_TYPES.length+"]");
        classToType = new java.util.HashMap(limit * 3 / 2);
        for(int i = 0; i < limit; i++) {
            classToType.put(FeaturesetProperties.SUPPORTED_CLASSES[i], FeaturesetProperties.SUPPORTED_TYPES[i]);
        }
    }
}
