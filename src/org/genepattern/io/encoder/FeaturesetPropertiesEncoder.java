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
 * FeaturesetPropertiesEncoder.java
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

import org.genepattern.data.DataObjector;
import org.genepattern.data.DefaultFeaturesetProperties;
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
public class FeaturesetPropertiesEncoder implements Encoder {
    /** Creates a new instance of FeaturesetPropertiesEncoder */
    protected FeaturesetPropertiesEncoder() {
        
    }
    
    // Encoder method signature
    /** encodes the data to the output stream
     * @param data the data object
     * @param out the output stream to write to
     * @throws IOException if a problem arises durring an I/O operation
     */
    public void write(final DataObjector data, final OutputStream out) throws IOException {
        write(data, new OutputStreamWriter(out));
    }
    /** gets the file extension for the specified object or null if wrong type
     * @param data the data object
     * @return String, the file extension for this format
     */
    public String getFileExtension(DataObjector data) {
        return FILE_EXTENSION;
    }
    
    /** returns true if this can handle encoding the specified DataObjector
     * @param data the data object
     * @return true if can encode
     */
    public boolean canEncode(DataObjector data) {
        return (data instanceof FeaturesetProperties);
    }
    /** all Encoder implementations should be singleton classes without state
     * @return DataEncoder, this classes singleton
     */
    public static Encoder instance() {
        return INSTANCE;
    }
    // end Encoder method signature
    
    // Helper methods
     /** encodes the data to the writer
      * @param data the data object
      * @param writer the writer to write data to
      * @throws IOException if a problem arises durring an I/O operation
      */
    public void write (final DataObjector data, final Writer writer) throws IOException {
        final FeaturesetProperties properties = (FeaturesetProperties)data;
        // the column index for where to store the row names or -1 if NA 
//    private int row_names_column = -1;
        // the column index where the row descriptions will go or -1 if NA
//    private int row_descr_column = -1;
        // determine the number of header lines
        final Map attrs = new java.util.HashMap();
        final Remark[] remarks = getRemarksAndAttributes(attrs, properties);
        //final int h_cnt = ((remarks == null || remarks.length == 0) ? attrs.size() : attrs.size() - 1) + getStdHeaderCount();
        final int h_cnt = attrs.size() + getStdHeaderCount(properties);
        try {
            final PrintWriter out = (writer instanceof PrintWriter) ? (PrintWriter)writer : new PrintWriter(new BufferedWriter(writer));
            
            //SDF 1.0
            writeMarkerLine(out); // SDF 1.0
            writeHeaderCount(out, h_cnt); // HeaderLines= int
            // each of the header lines
            final NameDescriptionIndices nd_inds =
                writeHeaders(out, attrs, remarks, properties);
            // write the body of the main data
            writeMainData(out, properties, nd_inds);
            
          //  System.out.println("Done encoding a properties file.");
            out.close();            
        } catch (IOException ex){
            throw new IOException("Error: while writing featureset properties  "
                +properties.getName()+":\n"+ex.getMessage());

        }
    }
    /** writes the line that marks this as a SDF/ODF file
     * @param out the writer to write data to
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeMarkerLine(final PrintWriter out) throws IOException {
        // required first line  
        // sdf/odf version_number  "SDF 1.0"
        out.print(MARKER_TAG);
        out.print(' ');
        out.println(CURRENT_SDF_VERSION);
    }
    /** records the number of header lines
     * @param out the writer to write data to
     * @param cnt the header count
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeHeaderCount(final PrintWriter out, final int cnt) throws IOException {
        out.print(this.KW_HEADER_LINES);
        out.println(cnt);
    }
    /** writes the headers and attributes
     * @param out the writer to write data to
     * @param attrs the data object's attributes
     * @param remarks an array of remarks
     * @param properties the featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     * @return NameDescriptionIndices
     */
    protected NameDescriptionIndices writeHeaders(final PrintWriter out, final Map attrs, final Remark[] remarks, final FeaturesetProperties properties) throws IOException {
        //FIXME need to get the remarks and insert them in the proper places
        writeModel(out, properties);              //        The model
        writeColumnHeaders(out, properties);      //        Column header row (Names)
        writeColumnTypes(out, properties);        //        data_type one for each column
        writeColumnsDescriptions(out, properties);//        Column description row
        
        final NameDescriptionIndices nd_ind =     //        which columns to put the names, desc
            writeRowNameDescriptions(out, properties);
                
        writeAttributes(out, attrs, properties);  //        attributes
        writeDataCount(out, properties);          //        num_rows of main data
        
        return nd_ind;
    }
    /** writes the notes or desciption
     * @param out the writer to write data to
     * @param note the comment text
     * @param properties the featureset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeComment(final PrintWriter out, final String note, final FeaturesetProperties properties) throws IOException {
        writeLine(KW_REMARK, note, out);
    }
    /** writes the row count
     * @param out the writer to write data to
     * @param properties he featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeDataCount(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        out.print(KW_DATA_LINES);
        out.print(delimiter);
        out.println(properties.getRowCount());
    }
    /** writes the model
     * @param out the writer to write data to
     * @param properties the featureset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeModel(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        writeLine(KW_MODEL, properties.getModel(), out);
    }
    /** writes the types of the columns
     * @param out the writer to write data to
     * @param properties the featureset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeColumnTypes(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        final int limit = properties.getColumnCount();
        if( limit <= 0 )
            return ;
        out.print(KW_COLUMN_TYPES);
        for(int i = 0; i < limit; i++) {
            out.print(delimiter);
            out.print((String)CLASS_TO_TYPE.get(properties.getColumnClass(i)));
        }
        out.println();
    }
    /** writes the column names
     * @param out the writer to write data to
     * @param properties the featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeColumnHeaders(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        out.print(KW_COLUMN_NAMES);
        final int limit = properties.getColumnCount();
      //  System.out.println("**** writing "+limit+" column headers");
        if( limit > 0 ) {
            for(int i = 0; i < limit; i++) {
                out.print(delimiter);
                out.print(properties.getColumnName(i));
            }
        } else if( properties instanceof DefaultFeaturesetProperties ) {
            final String[] cols = properties.getColumnNames(null);
            if( cols != null ) {
                writeObjArray(cols, out);
            }
        }
        out.println();
    }
    /** writes the column descriptions if any
     * @param out the writer to write data to
     * @param properties he featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeColumnsDescriptions(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        if(properties.hasColumnDescriptions()) {
            out.print(KW_COLUMN_DESCRIPTIONS);
            final int limit = properties.getColumnCount();
         //   System.out.println("**** writing "+limit+" column descriptions");
            if( limit > 0 ) {
                for(int i = 0; i < limit; i++) {
                    final String desc = properties.getColumnDescription(i);
                    out.print(delimiter);
                    out.print(desc.trim());
                }
            } else if( properties instanceof DefaultFeaturesetProperties ) {
                final String[] descs = properties.getColumnDescriptions(null);
                if( descs != null ) {
                    writeObjArray(descs, out);
                }
            }
        }
        out.println();
    }
    /** writes the rest of the values in the table
     * @param out the writer to write data to
     * @param properties he featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     * @return NameDescriptionIndices
     */
    protected NameDescriptionIndices writeRowNameDescriptions(final PrintWriter out, final FeaturesetProperties properties) throws IOException {
        int row = -1;
        NameDescriptionIndices nd_ind = null;
        if( properties.getRowCount() > 0 ) {
            if( properties.hasRowNames() ) {
                row++;
                if( nd_ind == null )
                    nd_ind = new NameDescriptionIndices();
                nd_ind.name_index = row;
                out.print(KW_ROW_NAMES_COLUMN);
                out.println(row);
            }
            if( properties.hasRowDescriptions() ) {
                row++;
                if( nd_ind == null )
                    nd_ind = new NameDescriptionIndices();
                nd_ind.descr_index = row;
                out.print(KW_ROW_DESCRIPTIONS_COLUMN);
                out.println(row);
            }
        } else { // no main data so write names and descriptions to an attribute
            if( properties.hasRowNames() ) {
                final String[] names = properties.getRowNames(null);
                out.print(KW_ROW_NAMES);
                writeObjArray(names, out);
                out.println();
            }
            if( properties.hasRowDescriptions() ) {
                final String[] descs = properties.getRowDescriptions(null);
                out.print(KW_ROW_DESCRIPTIONS);
                writeObjArray(descs, out);
                out.println();
            }
            nd_ind = NO_NAME_DESC;
        }
        return nd_ind;
    }
    /** writes the attributes if any
     * @param attrs the attributes
     * @param out the writer to write data to
     * @param properties he featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeAttributes(final PrintWriter out, final Map attrs, final FeaturesetProperties properties) throws IOException {
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
    
    /** writes the rest of the values in the table
     * @param nd_inds the name description index
     * @param out the writer to write data to
     * @param properties he featurset properties object
     * @throws IOException if a problem arises durring an I/O operation
     */
    protected void writeMainData(final PrintWriter out, final FeaturesetProperties properties, final NameDescriptionIndices nd_inds) throws IOException {
//        final boolean has_row_names = (nd_inds != null && nd_inds.name_index  >= 0);
//        final boolean has_row_descs = (nd_inds != null && nd_inds.descr_index >= 0);
        final int num_cols = properties.getColumnCount();
        final int num_rows = properties.getRowCount();
        for(int r = 0; r < num_rows; r++) {
//            // assumes that if has row names that it will be first
//            if( has_row_names ) {
//                out.print(properties.getRowName(r));
//                out.print(delimiter);
//            }
//            // also assumes that if has row descriptions that it will be next
//            if( has_row_descs ) {
//                out.print(properties.getRowDescription(r));
//                out.print(delimiter);
//            }
            for(int c = 0; c < num_cols; c++) {
                out.print(properties.getValueAt(r, c));
                out.print(delimiter);
            }
            out.println();
        }
        
    }
    /** writes the kwy word and value
     * @throws IOException if a problem arises durring an I/O operation
     */
    private void writeLine(final String kw, final String val, final PrintWriter out) throws IOException {
        out.print(kw);
        out.print(delimiter);
        out.println(val);
    }
    // info methods used by the writerXxx(...) methods
    
    /** gets the standard header count
     * @param properties properties the featurset properties object
     * @return the number of header lines in a simple header
     */
    protected int getStdHeaderCount(final FeaturesetProperties properties) {
        /*
        KW_REMARK                   // not counted
        KW_COLUMN_DESCRIPTIONS      // may not have
        KW_COLUMN_NAMES             // yes
        KW_COLUMN_TYPES             // maybe
        KW_DELIMITER                // no just use the default
        KW_DATA_LINES               // yes
        KW_ROW_DESCRIPTION_COLUMN   // maybe
        KW_ROW_NAMES_COLUMN         // maybe
        KW_MODEL                    // yes
         */
        int total = 3;
        if( properties.hasColumnDescriptions() )
            total++;
        if( properties.hasRowNames() )
            total++;
        if( properties.hasRowDescriptions() )
            total++;
        if( properties.getColumnCount() > 0 )//if 0 don't write the COLUMN_TYPES
            total++;
        return total;
    }
    /** seperates the remarks from the other attributes
     * @param map the attributes
     * @param properties he featurset properties object
     * @return Remark[], an array or remarks
     */
    protected Remark[] getRemarksAndAttributes(final Map map, final FeaturesetProperties properties) {
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
    private static final java.util.Map CLASS_TO_TYPE;
    /** the magic string label this stream as odf/sdf format */
    public static final String MARKER_TAG = "ODF";
    /** the major version number */
    public static final int SDF_VERSION_MAJOR = 1;
    /** the minor version number */
    public static final int SDF_VERSION_MINOR = 0;
    /** the version tag */
    public static final String CURRENT_SDF_VERSION = SDF_VERSION_MAJOR+"."+SDF_VERSION_MINOR;
    /** the file extension */
    public static final String FILE_EXTENSION = ".odf";
    /** the singleton instance of this */
    public static final FeaturesetPropertiesEncoder INSTANCE = new FeaturesetPropertiesEncoder();
    /** the delimiter used to seperate values */
    private final char delimiter = '\t'; // tab
//    /** the Feature or properties object */
//    public final FeaturesetProperties properties;
//    /** the column index for where to store the row names or -1 if NA */
//    private int row_names_column = -1;
//    /** the column index where the row descriptions will go or -1 if NA */
//    private int row_descr_column = -1;
    
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
    public static final String KW_ROW_DESCRIPTIONS_COLUMN;
    /** The index of the column where row names are */
    public static final String KW_ROW_NAMES_COLUMN;
    /** A list of row descriptions */
    public static final String KW_ROW_DESCRIPTIONS = "ROW_DESCRIPTIONS:";
    /** A list of row names */
    public static final String KW_ROW_NAMES = "ROW_NAMES:";
    /** The name of the model this data represents */
    public static final String KW_MODEL = "Model=";
    /** the no row names descriptions indicator */
    private static final NameDescriptionIndices NO_NAME_DESC = new NameDescriptionIndices();
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
        CLASS_TO_TYPE = new java.util.HashMap(limit * 3 / 2);
        for(int i = 0; i < limit; i++) {
            CLASS_TO_TYPE.put(FeaturesetProperties.SUPPORTED_CLASSES[i], FeaturesetProperties.SUPPORTED_TYPES[i]);
        }
        KW_ROW_DESCRIPTIONS_COLUMN = FeaturesetProperties.KW_ROW_DESCRIPTIONS_COLUMN;
        KW_ROW_NAMES_COLUMN = FeaturesetProperties.KW_ROW_NAMES_COLUMN;
    }
    
    // I N N E R   C L A S S E S
    /** simple object for returning a name index and a desciption index */
    private static class NameDescriptionIndices {
        public int name_index = -1;
        public int descr_index = -1;
    }
}
