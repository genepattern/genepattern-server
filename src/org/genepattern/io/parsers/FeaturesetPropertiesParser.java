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
 /*
 * FeaturesetPropertiesParser.java
 *
 * Created on January 20, 2003, 5:10 PM
 */

package org.genepattern.io.parsers;

import java.io.*;
import java.text.*;
import java.util.*;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.data.Dataset;
import org.genepattern.data.DefaultFeaturesetProperties;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.data.SomProperties;
import org.genepattern.data.Template;
import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.PrimeAnnotationFactory;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
import org.genepattern.io.encoder.FeaturesetPropertiesEncoder;
import org.genepattern.util.*;
//import java.util.zip.*;

import org.genepattern.server.*;


import org.genepattern.data.*;
import org.genepattern.data.*;
//import edu.mit.genome.util.ReusableStringTokenizer;
import org.genepattern.io.*;

/**
 * This can parse data that represents a FeaturesetProperties object.
 * @author  kohm
 */
public class FeaturesetPropertiesParser extends AbstractDataParser {
    
    /** Creates a new instance of FeaturesetPropertiesParser */
    public FeaturesetPropertiesParser() {
        super(new String[] {"odf", "sdf"/*deprecated use odf*/});
    }
    
//    // testing
//    public static final void main(final String[] args) throws Exception {
//        final String filename = "/Users/kohm/data/res_files/ucGCM_9_15000_All_SOM_2.gct__.gpp";
//        FeaturesetProperties the_props;
//        try {
//            File gpp_file = new File(filename);
//            System.out.println("reading file:"+gpp_file);
//            FileInputStream in = new FileInputStream(gpp_file);
//            //FeaturesetPropertiesParser decoder = new FeaturesetPropertiesParser(in, "Test_props");
//            FeaturesetPropertiesParser decoder = new FeaturesetPropertiesParser();
//            //the_props = decoder.getFeaturesetProperties();
//            the_props = (FeaturesetProperties)decoder.parse(in, "Test Props");
//            in.close();
//        } catch (IOException ex) {
//            throw new RuntimeException("Could not read the properties file, "+filename+"!\n"+ex);
//        }
//        //display it
////        final edu.mit.genome.gp.ui.odfviewer.AttributesViewer viewer = new edu.mit.genome.gp.ui.odfviewer.AttributesViewer();
////        viewer.setAttributes(the_props.getAttributes());
//        System.out.println("Display the FeaturesetProperties:");
//        javax.swing.JFrame frame = new javax.swing.JFrame("Prediction Results");
//        java.awt.Container container = frame.getContentPane();
////        edu.mit.genome.gp.ui.odfviewer.PropertiesTable table = 
////                new edu.mit.genome.gp.ui.odfviewer.PropertiesTable(the_props);
//        edu.mit.genome.gp.ui.odfviewer.DataPropertiesViewer viewer = 
//                new edu.mit.genome.gp.ui.odfviewer.DataPropertiesViewer(the_props);
//        
//        container.add(viewer, java.awt.BorderLayout.NORTH);
//        container.add(viewer, java.awt.BorderLayout.CENTER);
//        frame.setSize(200, 300);
//        frame.pack();
//        frame.show();
//        frame.addWindowListener(new java.awt.event.WindowAdapter() {
//            public void windowClosing(java.awt.event.WindowEvent evt) {
//                System.exit(0);
//            }
//        });
//    }
    
    // DataParser interface signature methods
    
    /** determines if the data in the input stream can be decoded by this implementation
     * @param in  the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @return true if this implementation can decode the data in the InputStream
     *
     */
    public boolean canDecode(final InputStream in) throws IOException {
        final InnerParser parser = new InnerParser(new LineReader(in));
        try {
            parser.readVersion();            //   "ODF 1.0"/"SDF 1.0"
            parser.readHeaderCount();
            return true;
        } catch (java.text.ParseException ex) {
          //  System.out.println("canDecode(InputStream): "+getClass()+" cannot decode stream "+ex.getMessage());
        } 
        return false;
    }
    
    /** Parses the InputStream  to create a DataObjector instance with the specified name
     * @return the resulting data object
     * @param name the name of the resulting data object
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     */
    public DataObjector parse(final InputStream in, final String name) throws IOException, java.text.ParseException {
		 
        final InnerParser parser = new InnerParser(new LineReader(in));
        parser.fsp_name = name;
        parser.parseStream();
        
        final FeaturesetProperties fs = parser.getFeaturesetProperties();
        final DataModel model = DataModel.findModel(fs.getModel());
        final FeaturesetPropertiesConverter converter = getConverter(model);
        return converter.createDataObject(fs);
    }
    /** reads the header lines and creates a summary
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     * @return SummaryInfo
     */
    public SummaryInfo createSummary(final InputStream in) throws IOException, java.text.ParseException {
		  final InnerParser parser = new InnerParser(new LineReader(in));
		  parser.storeValues = false;
		  final Map primary = new HashMap();
		  primary.put("Size=", (long)(Math.ceil(in.available()/1024.0)) + " KB");
        Exception exception = null;
        try {
            parser.readVersion();            //        SDF 1.0
            final int cnt = parser.readHeaderCount();
            parser.getSummary(cnt, primary);//        headers and remarks
        } catch (java.text.ParseException ex) {
            //reporter.logWarning(getClass()+" cannot decode stream "+ex.getMessage());
            exception = ex;
        } catch (IOException ex) {
            exception = ex;
            //reporter.logWarning(getClass()+" cannot decode stream "+ex.getMessage());
        }
		 
        final DataModel model = findModel(primary);
		  if(model!=Dataset.DATA_MODEL) {
			  primary.remove("DataLines");
			  primary.remove("Columns");
		  } else {
			  Object obj = primary.remove("DataLines");
			  primary.put("Rows", obj);
		  }
		  if( exception != null )
			  return new SummaryError(null, exception, this, primary, new HashMap()); 
			 
		  return new DefaultSummaryInfo(primary, new HashMap(), model);
    }
	 
    /** reads the header lines and returns them as a String
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws ParseException if there is a problem with the format or content of the data
     * @return String the header
     */
    public String getFullHeader(InputStream in) throws IOException, java.text.ParseException {
        final StringBuffer buf = new StringBuffer();
        final InnerParser parser = new InnerParser(new LineRecorder(in, buf));
        try {
            parser.readVersion();            //        SDF 1.0
            parser.readHeaderCount();
        } catch (java.text.ParseException ex) {
            System.err.println("While getting the full header "+getClass()
            +" cannot decode stream. Error:\n"+ex.getMessage());
        }
        return buf.toString();
    }
    
    // helpers
    
    /** finds the data model from the properties
     * @param attrs the attributes
     * @return DataModel
     */
    protected DataModel findModel(final Map attrs) {
        Object value = attrs.get("model");
        if( value == null )
            value = attrs.get("model=");
        if( value == null )
            value = attrs.get("Model");
        if( value == null )
            value = attrs.get("Model=");
      //  System.out.println("findModel="+value);
        if( value != null ) {
            //System.out.println(" class="+value.getClass());
            if( value instanceof DataModel ) {
                return (DataModel)value;
            } else if( value instanceof String ) {
                final String name = ((String)value).trim();
                if( name.length() > 0 ) {
                    final DataModel model = DataModel.findModel(name);
                    
                    return (model == null) ? new DataModel(name) : model;
                }
            }
        }
        return DataModel.UNKNOWN_MODEL;
    }
    /** gets the appropriet FeaturesetPropertiesConverter
     * @param model the data model
     * @return FeaturesetPropertiesConverter
     */
    protected static final FeaturesetPropertiesConverter getConverter(final DataModel model) {
        final FeaturesetPropertiesConverter converter = (FeaturesetPropertiesConverter)MODEL_TO_CONVERTER.get(model);
        return ( (converter != null) ? converter : FeaturesetPropertiesConverter.PASSTHOUGH_CONVERTER );
    }
    /** Helper that fills the list with tokens. If a delimiter is found
     * and the next token is also a delimter returns the empty String
     * @param list the list to populate with strings
     * @param toke the reusable string tokenizer that has a string to parse
     * @return a List the same one as in the params
     */
    protected static List populateWithTokens(final List list, final ReusableStringTokenizer toke) {
        final boolean ignore_trailing_delimiters = false;
        final String empty = "";
        boolean had_del = false;
        final int limit = toke.countTokens();
        for(int i = 0; i < limit; i++) {//while(toke.hasMoreElements()) {
            final String value = toke.nextToken().trim();
            if( value.length() > 0 ){ // this assumes that any returned
                list.add(value);      // delimiter is a whitespace character
                had_del = false;
            } else {
                if( (i + 1) < limit ) {//if( toke.hasMoreElements() ) {
                    if( had_del )              // already had a delimter
                        list.add(empty);       // add a marker for this
                } else {
                    if(!ignore_trailing_delimiters) // final token that is a
                        list.add(empty);            // delimiter
                    if( had_del )
                        list.add(empty);
                }
                had_del = true;
            }
        }
        return list;
    }
    // fields
    /** maps the string representations to the types */
    private static final Map TYPE_TO_CLASS;
    /** the delimiters */
    private static final String DELIMITERS = "\t\n\r\f";
    /** the delimiters that seperate a keyword from its' value */
    private static final char[] EQUALS_COLON = new char[] {'=', ':'};
    /**
     * the KeywordParsers keyed by the lower case keyword minus its' delimiter
     * Note subclasses must not modify the contents of this Map either by adding 
     * or taking away keys and or values.
     *
     * @see getParser(String kw)
     */
    private static final Map PARSERS = new HashMap();
    /** the parser for unknown keywords */
    private static final DefaultParser DEFAULT_PARSER = new DefaultParser();
    /** the default model type String */
    public static final String DEFAULT_MODEL_TYPE = "FeaturesetProperties";
    /** maps DataModel objects to FeaturesetPropertiesConverter instances */
    protected static final Map MODEL_TO_CONVERTER = new HashMap();
    
    /** static initializer */
    static  {
        // populate the MODEL_TO_CONVERTER mappings
        MODEL_TO_CONVERTER.put(Dataset.DATA_MODEL, DatasetConverter.instance());
        MODEL_TO_CONVERTER.put(Template.DATA_MODEL, TemplateConverter.instance());
        
        //Create and populate the TYPE_TO_CLASS Mappings
        final int limit = FeaturesetProperties.SUPPORTED_CLASSES.length;
        if(limit != FeaturesetProperties.SUPPORTED_TYPES.length)
            throw new ArrayIndexOutOfBoundsException("Internal error: unequal array lengths!\n"
                +"SUPPORTED_CLASSES["+limit+"] "
                +"SUPPORTED_TYPES["+FeaturesetProperties.SUPPORTED_TYPES.length+"]");
        TYPE_TO_CLASS = new java.util.HashMap(limit * 3 / 2);
        for(int i = 0; i < limit; i++) {
            TYPE_TO_CLASS.put(FeaturesetProperties.SUPPORTED_TYPES[i], FeaturesetProperties.SUPPORTED_CLASSES[i]);
        }
        
        // Create and map the instances of AbstractKeywordParser with the lowercase 
        // keywords minus their delimiters
        // CONSIDER: Could change this to:
        // parser = StringAssignmentParser(FeaturesetPropertiesEncoder.DW_MODEL, getFieldFor("model_type"));
        // PARSERS.put(parser.getLowerCaseKeyword(), parser);
        //
        // parser = StringListParser(FeaturesetPropertiesEncoder.KW_COLUMN_NAMES, getFieldFor("column_names"));
        // PARSERS.put(parser.getLowerCaseKeyword(), parser);
        //
        // parser = IntAssignmentParser(FeaturesetPropertiesEncoder.KW_DATA_LINES, getFieldFor("data_rows_count"));
        //...
        // parser = ObjectListParser(FeaturesetPropertiesEncoder.KW_COLUMN_TYPES, getFieldFor("column_classes")) {
        //    protected Object transformToken(final String token, final InnerParser fpp) {
        //        return fpp.parseClassRep(token);
        //    }
        // };
        AbstractKeywordParser parser = new ColumnDescriptionsParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new ColumnHeadersParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new ColumnTypesParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new DelimiterParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new RowCountParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new ModelParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new RowHeadersParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
        parser = new RowDescriptionsParser();
        PARSERS.put(parser.getLowerCaseKeyword(), parser);
        
    }
    
    // I N N E R   C L A S S E S
    
    /** the main parser */
    protected static class InnerParser {
		 boolean  storeValues = true;
		 static java.util.Set keywords = new java.util.HashSet();
       static {
			 // odf files: SOMCluster, PredictionResults, GeneList, PredictionFeatures
			keywords.add("model");
			keywords.add("model=");
			keywords.add("Model");
			keywords.add("Model=");
			
			keywords.add("DataLines");
			
			//PredictionFeatures
			keywords.add("NumFeatures");
			
			// Gene List
			keywords.add("GeneName"); 
			
			// Prediction Results
			keywords.add("NumErrors"); 
			keywords.add("NumCorrect"); 
			
		 }
        /** coonstuctor */
        InnerParser(final LineReader reader) {
            this.reader = reader;
            this.toke = new org.genepattern.util.ReusableStringTokenizer();
            line_parser = new LineParser(reader);
            attributes = new HashMap();
        }
        
        // public accessors
        /** returns the created DefaultFeaturesetProperties
         * @return FeaturesetProperties
         */
        public FeaturesetProperties getFeaturesetProperties() {
            return this.props;
        }
        // the primary internal methods
        /** reads it all in
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void parseStream() throws IOException, java.text.ParseException{
            final LineReader reader = this.reader;
            try {
                readVersion();            //        SDF 1.0
                final int header_cnt = readHeaderCount();
                readHeaders(header_cnt);//        headers and remarks
                //FIXME may not have the data_rows_count and will have to do something
                // beside ArrayOArrays...
                //FIXME may not have defined the column_classes should default to
                // an array of all String classes
                //FIXME should issue error if havn't defined any of the column keyword
                // lists that determine the column_count (i.e. column_count == 0)
                if( column_classes == null )
                    column_classes = new Class[0];
                arrays = new ArrayOArrays(data_rows_count, column_classes);
                readAllRows();            //        rest of the data - main data
                //System.out.println("Done decoding a properties file.");
                reader.close();
                
                final String the_name = createName(attributes);
                
                props = createFeaturesetProperties(the_name, getModel(), column_labels,
                    column_descriptions, column_classes, arrays,
                    attributes);
            } catch (IOException ex){
                ex.printStackTrace();
                throw new IOException("Error: while reading featureset properties \""
                +fsp_name+"\":\n"+ex.getMessage());
            }
        }
        /** reads the version tag
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void readVersion() throws IOException, java.text.ParseException {
            final String line = reader.readLine();
            //System.out.println("readVersion: "+line);
             if( line == null )
                throw new java.text.ParseException("Not a SDF/ODF data stream!\nFirst line: null", 0);
            toke.resetTo(line, ReusableStringTokenizer.WHITESPACE, false);
            // odf
            if( !toke.hasMoreTokens() )
                throw new java.text.ParseException("Not a SDF/ODF data stream!\nFirst line: \""+line+"\"", 0);
            final String magic = toke.nextToken().trim();
            //FIXME digest the line to see if it is a odf with version, no space i.e. odf1.0
            if( !magic.equalsIgnoreCase(FeaturesetPropertiesEncoder.MARKER_TAG)  && !magic.equalsIgnoreCase("sdf") )
                throw new java.text.ParseException("Not a SDF/ODF data stream!\nFirst line: \""+line+"\"", 0);
            // Version number
            // FIXME digest the line to see if it is a lesser version that could still be read
            if( !toke.hasMoreTokens() )
                throw new java.text.ParseException("Not a supported SDF/ODF version!\nFirst line: \""+line+"\"", 0);
            final String version = toke.nextToken().trim();
            if( !version.equals(FeaturesetPropertiesEncoder.CURRENT_SDF_VERSION) ) {
                throw new java.text.ParseException("Not a supported SDF/ODF version!\n"
                +"Current version = "+FeaturesetPropertiesEncoder.CURRENT_SDF_VERSION
                +"\nFirst line: \""+line+"\"", 0);
            }
        }
        
        /** reads the row count
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         * @return int the header count
         */
        protected int readHeaderCount() throws IOException, java.text.ParseException {
            reader.readLine(); // goes into current.line
            line_parser.parseLine(EQUALS_COLON);
            line_parser.checkSameKeyword(FeaturesetPropertiesEncoder.KW_HEADER_LINES);
            //System.out.println("header count line= "+reader.getCurrentLine());
            
            try {
                header_lines_count = Integer.parseInt(line_parser.value.trim());
            } catch (NumberFormatException ex) {
                throw new java.text.ParseException("Cannot convert \""+line_parser.value+"\" to a number!", reader.getTotalLineCount());
            }
            return header_lines_count;
        }
        /** reads the headers
         * @param header_cnt the number of header lines
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void readHeaders(final int header_cnt) throws IOException, java.text.ParseException {
            readHeaders(header_cnt, null);
        }
        /** reads the headers
         * @param header_cnt the number of header lines
         * @param primary where the primary attributes will be stored
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void readHeaders(final int header_cnt, final Map primary) throws IOException, java.text.ParseException {
            reader.setExpectEOF(false);
            try {
                if( primary == null ) {
                    for(int i = 0; i < header_cnt; i++) {
                        final String kw = parseKeyword();
                        final AbstractKeywordParser parser = getParser(kw);
                        parser.parse(line_parser.value, this);
                    }
                } else { // some thing as above loop but record values
                    for(int i = 0; i < header_cnt; i++) {
                        final String kw = parseKeyword();
                        final AbstractKeywordParser parser = getParser(kw);
                        final Object value = parser.parse(line_parser.value, this);
                        if( line_parser.value.length() < 1000 ) // don't keep big stuff for summary
                            primary.put(kw, value);
                    }
                }
            } catch (java.text.ParseException ex) {
                ex.printStackTrace();
                throw new java.text.ParseException("Could not parse the headers!\n"+ex,
                ex.getErrorOffset());
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new IOException("Could not parse the headers!\n"+ex);
            } finally {
                reader.setExpectEOF(true);
            }
            
        }
		  
		   /** reads the headers
         * @param header_cnt the number of header lines
         * @param primary where the primary attributes will be stored
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void getSummary(final int header_cnt, final Map map) throws IOException, java.text.ParseException {
            reader.setExpectEOF(false);
            try {
                for(int i = 0; i < header_cnt; i++) {
						 	final String kw = parseKeyword();
							final AbstractKeywordParser parser = getParser(kw);
							final Object value = parser.parse(line_parser.value, this);
							if(kw.equals("COLUMN_NAMES")) {
								if(value instanceof String[]) {
									String[] s = (String[]) value;
									int dataColumns = s.length-2; // assume file has row names and row descriptions columns
									map.put("Columns", ""+dataColumns); 
								}
							}
							if(keywords.contains(kw)) {
								map.put(kw, value);
							}
                }
            } catch (java.text.ParseException ex) {
                ex.printStackTrace();
                throw new java.text.ParseException("Could not parse the headers!\n"+ex,
                ex.getErrorOffset());
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new IOException("Could not parse the headers!\n"+ex);
            } finally {
                reader.setExpectEOF(true);
            }
            
        }
		  
        /** helper - parses for keyword and handles remarks
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         * @return String the key word
         */
        protected String parseKeyword() throws IOException, java.text.ParseException {
            reader.readLine();
            if( line_parser.isRemark(FeaturesetPropertiesEncoder.KW_REMARK) ) {
                // need to store intermediate results in this method for recursion to work
                final String value = line_parser.value;
                final String kw = parseKeyword(); // recursion occures here
                addRemark(value, kw);
                return kw;
            }
            // otherwise parse a regular line
            line_parser.parseLine(EQUALS_COLON);
            return line_parser.keyword;
        }
        /** adds the remark to the list of remarks
         * @param text the remark's text
         * @param kw the keyword this remark is just after
         */
        protected void addRemark(final String text, final String kw) {
            //FIXME add immediate reporting if report_warning is true
            //System.out.println("Remark: "+text+"\njust before "+kw);
        }
        /** calculates the real number of columns given the row names and descriptions
         * may be in the main mbody
         */
        private int calcColumnCount() {
            final String rnc = FeaturesetPropertiesEncoder.KW_ROW_NAMES_COLUMN;
            final String rnd = FeaturesetPropertiesEncoder.KW_ROW_DESCRIPTIONS_COLUMN;
            final boolean has_row_names = ( attributes.containsKey(rnc.substring(0, rnc.length() - 1)) );
            final boolean has_row_descs = ( attributes.containsKey(rnd.substring(0, rnd.length() - 1)) );
            int cnt = column_count;
            if( has_row_names )
                cnt++;
            if( has_row_descs )
                cnt++;
            return cnt;
        }
        /** reads the rest of the values in the table
         * @throws IOException if a problem occurs due to an I/O operation
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void readAllRows() throws IOException, java.text.ParseException {
            //final int num_cols = calcColumnCount();
            final int num_cols = column_count;
            final int num_rows = data_rows_count;
            for(int r = 0; r < num_rows; r++) {
                final String line = reader.readLine();
                toke.resetTo(line, "\t", true); // get all tokens and delimiters
                final List list = populateWithTokens(new ArrayList(toke.countTokens()), toke);
                final int cnt = list.size();
                
                if(cnt != num_cols) {
                    System.err.println("Error line tokens:");
                    for(int k= 0; k < cnt; k++) //while(toke.hasMoreTokens())
                        System.err.print("\""+list.get(k)+"\" ");
                    throw new java.text.ParseException("Expecting "+num_cols+" data tokens found "
                    +cnt+" on line "+reader.getLineCount()+"!\n"+reader.getCurrentLine(), 0);
                }
                //System.out.println("Reading Column:");
                for(int c = 0; c < cnt; c++) {
                    arrays.set(((String)list.get(c)).trim());
                }
            }
            //System.out.println("readAllRows: done");
        }
        // helper methods
        /** creates the name if possible otherwise use specified name from parse(...) */
        private String createName(final Map attrs) {
            final String[] NAME_KEYS = new String[] {
                "name", "name=", "Name", "Name=", "NAME", "NAME="};
            String the_name = null;
            final int limit = NAME_KEYS.length;
            for(int i = 0; i < limit && the_name == null; i++) {
                the_name = (String)attrs.get(NAME_KEYS[i]);
            }
            if( the_name == null )
                return fsp_name;
            return the_name;
        }
//        /**
//         *
//         */
//        protected final String readLine(final BufferedReader reader) throws IOException {
//            final String line = reader.readLine();
//            current.line = line;
//            current.total_num++;
//            if(line == null) {
//                throw new java.io.EOFException("Error: Not expecting to reach end"
//                +" of data at line " + current.total_num
//                +( (data_rows_count == 0) ? "!": "should be " //FIXME count is wrong!
//                +(header_lines_count + data_rows_count - current.num)+" lines more!" ) );
//            }
//            
//            final String the_line = line.trim();
//            if( the_line.length() == 0 ) {// skip blank lines
//                return readLine(reader);
//            }
//            current.num++; // only counts when line not skipped
//            return the_line;
//        }
        /** parses the String representations of the supported classes and returns the
         * corresponding Class
         * @param rep the class represention
         * @return Class
         */
        protected final Class parseClassRep(final String rep) {
            final Class clss = (Class)TYPE_TO_CLASS.get(rep);
            if(clss == null)
                throw new java.util.NoSuchElementException("No type defined for \""+rep+"\"!");
            return clss;
        }
        /** returns a parser for the values associated with this keyword
         * This could be overriden by subclass to expand the number of AbstractKeywordParser
         * classes available for handling new Key Words that this doesn't know about
         *
         * The overriding method should finaly call super.getParser(kw) if it couldn't find
         * the keyword.  Example code for subclass:
         * protected AbstractKeywordParser getParser(final String kw) {
         *   final AbstractKeywordParser parser = (AbstractKeywordParser)ADDITIONAL_PARSERS.get(kw.toLowerCase());
         *   if( parser == null )
         *       return super.getParser(kw);
         *   return parser;
         * }
         * @param kw the key word
         * @return AbstractKeywordParser
         */
        protected AbstractKeywordParser getParser(final String kw) {
            final AbstractKeywordParser parser = (AbstractKeywordParser)PARSERS.get(kw.toLowerCase());
            if( parser == null )
                return DEFAULT_PARSER;
            return parser;
        }
        /** sets the model
         * @param model_type the model
         */
        protected void setModel(final String model_type) {
            this.model_type = model_type;
        }
        /** returns the model type if model_type is null returns default
         * @return String the <CODE>String</CODE> representation of the model
         */
        protected String getModel() {
            if( model_type == null )
                model_type = DEFAULT_MODEL_TYPE;
            return model_type;
        }
        /** creates the FeaturesetProperties object from the parsed data
         * subclasses can override this to produce more specific DataObject instances
         * @param name name of the data object
         * @param model the string representation of the data model
         * @param column_labels array of column names
         * @param column_descriptions array of column descriptions
         * @param column_classes array of column classes
         * @param arrays the array of arrays of the main data
         * @param attributes the attributes
         * @return FeaturesetProperties
         */
        protected FeaturesetProperties createFeaturesetProperties(final String name,
        final String model, final String[] column_labels, final String[] column_descriptions,
        final Class[] column_classes, final ArrayOArrays arrays, final Map attributes) {
//            PropertiesFactory factory = (PropertiesFactory)model_factory.get(model);
//            if( model != null ) {
//                return factory.createDataObjectProperties(name, model, column_labels,
//                    column_descriptions, column_classes, arrays, attributes);
//            }
            //FIXME use above code instead of long list of if ... else if
            if( model.equals(SomProperties.DATA_MODEL.value) )
                return new SomProperties(name, column_labels,
                column_descriptions, row_labels, row_descriptions, attributes);
            // default
            return new DefaultFeaturesetProperties(name, model, column_labels,
                column_descriptions, row_labels, row_descriptions, column_classes, arrays, attributes);
        }
        
        // fields
           
        /** name of the DefaultFeaturesetProperties */
        private String fsp_name;
        /** the current line */
        //private final CurrentLine current;
        /** the attributes of the FeaturesetProperties */
        private final Map attributes;
        /** this can parse a line and check for problems loggin warnings etc */
        private final LineParser line_parser;
        /** reads lines keeps track of lie count */
        private final LineReader reader;
        /** the tokenizer */
        private final ReusableStringTokenizer toke;
        /** the number of data lines */
        private int data_rows_count;
        /** the keyword that was used to set the data_rows_count */
        private String data_rows_count_setter;
        /** the total number of header lines */
        private int header_lines_count;
        /** the classes that define the types of data in each column */
        private Class[] column_classes;
        /** the number of columns */
        private int column_count;
        /** the keyword that was used to set the column_count */
        private String column_count_setter;
        /** the column headers */
        private String[] column_labels;
        /** the columns descriptions */
        private String[] column_descriptions;
        /** the row headers */
        private String[] row_labels;
        /** the columns descriptions */
        private String[] row_descriptions;
        /** ArrayOArrays is needed to construct a valid DefaultFeaturesetProperties */
        private ArrayOArrays arrays;
        /** the type of model which could be "Dataset", "Prediction Results", etc. */
        private String model_type;
        /** the DefaultFeaturesetProperties object */
        private FeaturesetProperties props;
    
    } // end InnerParser
    /** handles parsing the line into components */
    static class LineParser {
     
        /** constructor */
        LineParser(final LineReader current) {
            this.current = current;
        }
        
        /** parses the line for the key word using the delimiters to separate
         * kw from value
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void parseLine(final char[] dels) throws java.text.ParseException {
            final String line = current.getCurrentLine();
            int index = -1;
            char del = 0;
            final int limit = line.length();
            final int cnt = dels.length;
            loop: for(int i = 0; i < limit; i++) {
                final char letter = line.charAt(i);
                for(int j = 0; j < cnt; j++) {
                    if(letter == dels[j]) {
                        del = letter;
                        index = i;
                        break loop;  // break out of both loops
                    }
                }
            }
            if(index == -1) {
                throw new java.text.ParseException("Could not identify the key word using '"
                    + new String(dels)+"' delimiters!\n"
                    + "line ("+current.getTotalLineCount()+"): "+line, current.getTotalLineCount()); 
            }
            this.delimiter = del;
            this.keyword   = line.substring(0, index).trim();
            // don't trim the value it might have whitespace delimiters at the beginning
            this.value     = line.substring(index + 1, line.length());
        }
        /** checks that the current keyword is similar to the specified one
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void checkSameKeyword(final String kw) throws java.text.ParseException {
            final int len = kw.length() - 1;
            final String minus_del = kw.substring(0, len);
            if( !(minus_del.equalsIgnoreCase(keyword)) ) { // Error if not similar
                throw new java.text.ParseException("Error: Keyword \""+keyword
                    +"\" is not similar to the expected \""+minus_del+"\"!",
                    current.getTotalLineCount());
            }
            final char best = kw.charAt(len);
            if( !(delimiter == best) ) { // has same delimiter
                addWarning("Keyword \""+keyword+"\" should be delimited with a '"
                    +best+"' i.e. \""+kw+"\"");
            }
            if( !(minus_del.equals(keyword)) ) { // exact match
                addWarning("Case does not match: \""+keyword+"\" should be \""
                    +minus_del+"\"!");
            }
        }
        /** adds a warning to the list of warnings */
        protected void addWarning(final String text) {
            // FIXME more explicit with line and total_num
            warnings.add(text);
        }
        /** returns true if a remark was just parsed */
        protected boolean isRemark(final String remark_kw) {
            final String line = current.getCurrentLine();
            if( line == null )
                return false;
            if( line.startsWith(remark_kw) ) {
                this.keyword   = remark_kw;
                this.delimiter = 0;
                this.value     = line.substring(remark_kw.length() - 1, line.length()).trim();
                return true;
            }
            return false;
        }
        
        // fields
        
        /** a copy of the ref to CurrentLine */
        private final LineReader current;
        /** the list of warnings */
        private final List warnings = new ArrayList();
        /** the keyword part of the line */
        public String keyword;
        /** the delimiter character */
        public char delimiter;
        /** the rest of the line */
        public String value;
    } // end LineParser
    
    /** abstract class for parsing values */
    abstract static protected class AbstractKeywordParser {
        /** constructor
         * @param kw the key word associated with this parser
         */
        protected AbstractKeywordParser(final String kw) {
            this.lowercase_kw = kw.substring(0, kw.length() - 1).toLowerCase();
            this.kw           = kw;
        }
        
        /** returns the lower case keyword without the delimiter
         * @return String
         */
        public String getLowerCaseKeyword() {
            return lowercase_kw;
        }
        /** gets the normal key word with it normal case and delimiter
         * @return String
         */
        public String getKeyword() {
            return kw;
        }
        // abstract methods
        /** parses the value
         * @param value the value to parse
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         * @return Object whatever the value is supposed to become
         */
        abstract public Object parse(String value, InnerParser fpp) throws java.text.ParseException;
        // field
        /** the lowercase keyword minus the delimiter */
        private final String lowercase_kw;
        /** the regular keyword no change in case and has the delimiter */
        private final String kw;
    }// end AbstractKeywordParser
    
    /** This should be subclasses by those classes that need to parse multiple values */
    abstract protected static class MultiValueParser extends AbstractKeywordParser {
        
        /** Constructor for the parser of arrays
         * @param kw the key word associated with this parser
         */        
        protected MultiValueParser(final String kw) {
            super(kw);
        }
        // abstract methods
        /** sets the appropriet array in FeaturesetPropertiesParser
         * @param values the array to set
         * @param fpp the inner parser
         */
        abstract protected void setArray(Object[] values, InnerParser fpp);
        // row or column dependent abstract methods
        /** throw the Exception with the proper information
         * @param cnt what the wrong count was
         * @param fpp inner parser
         * @throws ParseException to indicate that there is a problem with the format or content of the data
         */
        abstract protected void createException(final int cnt, final InnerParser fpp) throws java.text.ParseException;
        /** gets what the expected count should be or 0 if not set yet
         * @param fpp the inner parser
         * @return int the expected count
         */
        abstract protected int getCount(final InnerParser fpp) ;
        /** should be implemented to check and/or sets the row or column count.
         * @param cnt the initial count
         * @param setter_kw the key word that set the count
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        abstract protected void setCount(final int cnt, final String setter_kw, final InnerParser fpp) throws java.text.ParseException;
        
        // helper methods
        
        /** breaks up the string into components
         * @param value the text that represents an array
         * @param fpp the inner parser
         * @param limit the expected number of elements in array
         * @throws ParseException if there is a problem with the format or content of the data
         * @return Object[] the array
         */
        protected Object[] breakString(final String value, final InnerParser fpp, final int limit) throws java.text.ParseException {
            final ReusableStringTokenizer toke = fpp.toke;
            //toke.resetTo(value, DELIMITERS, true);// get both tokens and delimiters
            toke.resetTo(value, "\t", true);// get both tokens and delimiters
            //System.out.println("Initial number of tokens+delims="+toke.countTokens());
            final List list = populateWithTokens(new ArrayList(toke.countTokens()), toke);
            //System.out.println("The list:");
            //edu.mit.genome.debug.FieldDumper.printArray(list.toArray(), System.out, "'   '");
            final int cnt = list.size();
            if(cnt != 0 && limit != 0 && cnt != limit) {
                //System.out.println("tab count="+StringUtils.getNumOccurances(value, "\t"));
                //System.out.println("value='"+StringUtils.replaceAll(value,"\t","<TAB>")+"'");
                createException(cnt, fpp);
            }
            final Object[] values = createArray(cnt);
            for(int i = 0; i < cnt; i++) {
                values[i] = transformToken((String)list.get(i), fpp);
            }
            return values;
        }
        
        /** creates an array of String unless overridden by subclass
         * @param cnt the size of the array
         * @return Object[]  the array
         */
        protected Object[] createArray(final int cnt) {
            return new String[cnt];
        }
        /** transforms the token into another Object (just returns the String here)
         * can be overriden by subclasses to return a Class, Integer, etc
         * @param token the string value
         * @param fpp the inner parser
         * @return Object the transformed value
         */
        protected Object transformToken(final String token, final InnerParser fpp) {
            return token;
        }
        /** parses the value
         * @param value the value to parse
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         * @return Object
         */
        public Object parse(final String value, final InnerParser fpp) throws java.text.ParseException {
            final String name = getClass().getName();
            //System.out.println(name.substring(name.lastIndexOf('.'))+"'s value \""+value+"\"");
            final Object[] values = breakString(value, fpp, getCount(fpp));
            setCount(values.length, fpp.line_parser.keyword, fpp);
            setArray(values, fpp);
            return values;
        }
        
    } //End MultiValueParser
    /** the column specific MultiValueParser implementation */
    abstract protected static class MultiValueCParser extends MultiValueParser{
        
        /** Constructor
         * @param kw the key word associated with this parser
         */        
        protected MultiValueCParser(final String kw) {
            super(kw);
        }
        
        /**
         * throw the Exception with the proper information
         * Default is to throw an exception if the cnt is not equal to the column_count
         * this behavoiur can be overriden by subclasses if needed
         * @param cnt the count
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format of the data
         */
        protected void createException(int cnt, InnerParser fpp) throws java.text.ParseException {
            throw new java.text.ParseException("There are not the expected number ("
                +fpp.column_count+") of "+getKeyword()+" but "
                +cnt + "! Line:\n" + fpp.reader.getCurrentLine()
                +"\nNote that the original count was set by "+fpp.column_count_setter,
                fpp.reader.getTotalLineCount());
        }
        /** gets what the expected count should be or 0 if not set yet
         * Currently gets the column_count but this could be overrideen by subclasses
         * @param fpp the inner parser
         * @return int the expected count
         */
        protected int getCount(final InnerParser fpp) {
            return fpp.column_count;
        }
        /** default implementation checks and/or sets the column count.
         * Should be overridden if different behavior is desired
         * @param cnt the initial count
         * @param setter_kw the key word that set the count
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void setCount(final int cnt, final String setter_kw, final InnerParser fpp) throws java.text.ParseException{
            if( fpp.column_count <= 0 ) { // unset so set the value
                fpp.column_count = cnt;
                fpp.column_count_setter = setter_kw;
            } else if( cnt != 0 && fpp.column_count != cnt ) { // error something is wrong
                throw new java.text.ParseException("There are not the expected number ("
                    +fpp.column_count +") of column headers but "+setter_kw+"="+cnt+"!\n"
                    +"The column_count was originally set by "
                    +fpp.column_count_setter, fpp.reader.getTotalLineCount());
            } // otherwise nothing to do
        }
    }// End MultiValueCParser
    /** the row specific MultiValueParser implementation */
    abstract protected static class MultiValueRParser extends MultiValueParser{
        
        /** Constructor
         * @param kw the key word associated with this parser
         */        
        protected MultiValueRParser(final String kw) {
            super(kw);
        }
        
        /**
         * throw the Exception with the proper information
         * Default is to throw an exception if the cnt is not equal to the data_rows_count
         * this behavoiur can be overriden by subclasses if needed
         * @param cnt the wrong count
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void createException(int cnt, InnerParser fpp) throws java.text.ParseException {
            throw new java.text.ParseException("There are not the expected number ("
                +fpp.data_rows_count+") of "+getKeyword()+" but "
                +cnt + "! Line:\n" + fpp.reader.getCurrentLine()
                +"\nNote that the original count was set by "+fpp.data_rows_count_setter,
                fpp.reader.getTotalLineCount());
        }
        /** gets what the expected count should be or 0 if not set yet
         * Currently gets the data_rows_count but this could be overriden by subclasses
         * @param fpp the inner parser
         * @return int the expected count
         */
        protected int getCount(final InnerParser fpp) {
            return fpp.data_rows_count;
        }
        /** default implementation checks and/or sets the row count.
         * Should be overridden if different behavior is desired
         * @param cnt the initial count
         * @param setter_kw the key word that caused the setting of the count
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void setCount(final int cnt, final String setter_kw, final InnerParser fpp) throws java.text.ParseException{
            if( fpp.data_rows_count <= 0 ) { // unset so set the value
                fpp.data_rows_count = cnt;
                fpp.data_rows_count_setter = setter_kw;
            } else if( fpp.data_rows_count != cnt) { // error something is wrong
                throw new java.text.ParseException("There are not the expected number ("
                    +fpp.data_rows_count +") of column headers but "+cnt+"!\n"
                    +"The data_rows_count was originally set by "
                    +fpp.data_rows_count_setter, fpp.reader.getTotalLineCount());
            } // otherwise nothing to do
        }
    }// End MultiValueRParser
    /** understands the row count values */
    private static class RowCountParser extends AbstractKeywordParser {
        
        RowCountParser() {
            this(FeaturesetPropertiesEncoder.KW_DATA_LINES);
        }
        private RowCountParser(final String kw) {
            super(kw);
        }
        
        /** parses the value
         * @throws ParseException if there is a problem with the format or content of the data
         */
        public Object parse(String value, InnerParser fpp) throws java.text.ParseException {
            //System.out.println("RowCountParser value=("+value+")");
            try {
                fpp.data_rows_count = Integer.parseInt(value.trim());
            } catch (NumberFormatException ex) {
                throw new java.text.ParseException("Cannot convert \""+value+"\" to a number!",
                    fpp.reader.getTotalLineCount());
            }
            //System.out.println("readRowCount: "+fpp.data_rows_count);
            return new Integer(fpp.data_rows_count);
        }
        
    }// end RowCountParser
    
    /** parses column types */
    private static class ColumnTypesParser extends MultiValueCParser {
        
        ColumnTypesParser() {
            this(FeaturesetPropertiesEncoder.KW_COLUMN_TYPES);
        }
        private ColumnTypesParser(final String kw) {
            super(kw);
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser  */
        protected void setArray(final Object[] values, final InnerParser fpp) {
            fpp.column_classes = (Class[])values;
            
        }
        /** creates an array of Class */
        protected Object[] createArray(final int cnt) {
            return new Class[cnt];
        }
        /** transforms the token into another Object (just returns the String here)
         * can be overriden by subclasses to return a Class, Integer, etc
         */
        protected Object transformToken(final String token, final InnerParser fpp) {
            return fpp.parseClassRep(token);
        }
        
    }// end ColumnTypesParser
    
    /** parses column headers */
    private static class ColumnHeadersParser extends MultiValueCParser {
        
        ColumnHeadersParser() {
            this(FeaturesetPropertiesEncoder.KW_COLUMN_NAMES);
        }
        private ColumnHeadersParser(final String kw) {
            super(kw);
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser  */
        protected void setArray(Object[] values, InnerParser fpp) {
            fpp.column_labels = (String[]) values;
        }
        
    }// end ColumnHeadersParser
    
    /** parses column descriptions */
    private static class ColumnDescriptionsParser extends MultiValueCParser {
        
        ColumnDescriptionsParser() {
            this(FeaturesetPropertiesEncoder.KW_COLUMN_DESCRIPTIONS);
        }
        private ColumnDescriptionsParser(final String kw) {
            super(kw);
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser  */
        protected void setArray(Object[] values, InnerParser fpp) {
            fpp.column_descriptions = (values != null && values.length > 0) ?
                (String[]) values : (String[])null;
        }
        
    }// end ColumnDescriptionsParser
    
    /** Parses the DELIMITER */
    private static class DelimiterParser extends AbstractKeywordParser {
        
        DelimiterParser() {
            this(FeaturesetPropertiesEncoder.KW_DELIMITER);
        }
        private DelimiterParser(final String kw) {
            super(kw);
        }
        
        /** parses the value
         * @throws ParseException if there is a problem with the format or content of the data
         */
        public Object parse(final String value, final InnerParser fpp) throws java.text.ParseException {
            System.err.println("Parser delimiter not implemented yet");
            throw new UnsupportedOperationException("Not implemented yet!");
        }
        
    }// end DelimiterParser
    
    /** Parses the model */
    private static class ModelParser extends AbstractKeywordParser {
        
        ModelParser() {
            this(FeaturesetPropertiesEncoder.KW_MODEL);
        }
        private ModelParser(final String kw) {
            super(kw);
        }
        
        /** parses the value
         * @throws ParseException if there is a problem with the format or content of the data
         */
        public Object parse(final String value, final InnerParser fpp) throws java.text.ParseException {
            fpp.setModel(value);
            return value;
        }
        
    }// end ModelParser
    /** parses row headers */
    private static class RowHeadersParser extends MultiValueRParser {
        
        RowHeadersParser() {
            this(FeaturesetPropertiesEncoder.KW_ROW_NAMES);
        }
        private RowHeadersParser(final String kw) {
            super(kw);
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser  */
        protected void setArray(Object[] values, InnerParser fpp) {
            fpp.row_labels = (String[]) values;
        }
        
    }// end RowHeadersParser
    /** parses row descriptions */
    private static class RowDescriptionsParser extends MultiValueRParser {
        
        RowDescriptionsParser() {
            this(FeaturesetPropertiesEncoder.KW_ROW_DESCRIPTIONS);
        }
        private RowDescriptionsParser(final String kw) {
            super(kw);
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser  */
        protected void setArray(Object[] values, InnerParser fpp) {
            fpp.row_descriptions = (values != null && values.length > 0) ?
                (String[]) values : (String[])null;
        }
        
    }// end RowDescriptionsParser
    /** assigns a value to the associated keyword */
    abstract protected static class AssignmentParser extends AbstractKeywordParser {
        AssignmentParser(final String kw) {
            super(kw);
            final char del = EQUALS_COLON[0];
            if( kw.charAt(kw.length() - 1) != del )
                throw new IllegalStateException("Wrong key word for an assignment!\n Should end in a"+del+".");
        }
        
        /** parses the value
         * @param value he string to parse
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         * @return Object
         */
        public Object parse(final String value, final InnerParser fpp) throws java.text.ParseException {
            setValue(value, fpp);
            return value;
        }
        
        // abstact methods
        
         /** sets the value
          * @param value the value to set
          * @param fpp the inner parser
          * @throws ParseException if there is a problem with the format or content of the data
          */
        abstract protected void setValue(final String value, final InnerParser fpp) throws java.text.ParseException;
    } // end AssignmentParser
    /** Assigns the parsed String value
     * subclasses must implement setValue(String);
     */
    abstract protected static class StringAssignmentParser extends AssignmentParser {
        
        StringAssignmentParser(final String kw) {
            super(kw);
        }

        /** sets the value
         * @param value the value to set
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void setValue(String value, InnerParser fpp) throws java.text.ParseException {
            setValue(value);
        }        
        // abstract methods
        
        /** sets the String value
         * @param value the text to set
         */
        abstract protected void setValue(final String value);
        
    } // end StringAssignmentParser
    /** assigns a parsed int value */
    abstract protected static class IntAssignmentParser extends AssignmentParser {
        IntAssignmentParser(final String kw) {
            super(kw);
        }
        
        /** sets the value
         * @param value the value to set
         * @param fpp the inner parser
         * @throws ParseException if there is a problem with the format or content of the data
         */
        protected void setValue(final String value, final InnerParser fpp) throws java.text.ParseException {
            try {
               final int i = Integer.parseInt(value.trim());
               setValue(i);
            } catch (NumberFormatException ex) {
                throw new java.text.ParseException("Could not set the "+getKeyword()+" value!"
                    +"\""+value+"\" is not an integer.", fpp.reader.getTotalLineCount());
            }
        }
        
        // abstract methods
        
        /** sets the int value
         * @param i the int value to set
         */
        abstract protected void setValue(final int i);
        
    } // end IntAssignmentParser
    /** assigns a list of parsed values */
    abstract protected static class ListRowParser extends MultiValueRParser {
        ListRowParser(final String kw) {
            super(kw);
            final char del = EQUALS_COLON[1];
            if( kw.charAt(kw.length() - 1) != del )
                throw new IllegalStateException("Wrong key word for an assignment!\n Should end in a"+del+".");
        }
        
        // abstact methods
        
        /** sets the array */
        //abstract protected void setArray(final String value, final InnerParser fpp) throws ParseException;
    } // end AssignmentParser
        
    /** This is the default parser which will capture any key words and values
     * that are not defined
     */
    private static final class DefaultParser extends MultiValueParser {
        
        DefaultParser() {
            this(" "); // not a keyword
        }
        private DefaultParser(final String kw) {
            super(kw);
        }
        
        // overridden methods
        
        /**
         * default implementation checks and/or sets the column count.
         * This is overridden to do nothing
         */
        protected void setCount(final int cnt, final String setter_kw, final InnerParser fpp) {
            // not implemlemented
        }
        /** gets what the expected count should be or 0 if not set yet
         * Overridden to return 0.
         */
        protected int getCount(final InnerParser fpp) {
            return 0;
        }
        /** parses the value overrides the super
         * Since the keyword is undefined cannot verify that the proper delimiter
         * was used; nor correct for the improper use of '=' when ':' should have
         * been used to specify a list.
         * @throws ParseException if there is a problem with the format or content of the data
         */
        public Object parse(final String value, final InnerParser fpp) throws java.text.ParseException {
            final char del = fpp.line_parser.delimiter;
            final String key = fpp.line_parser.keyword;
            //System.out.println("DefaultParser's key \""+key+"\" del '"+del+"' value \""+value+"\"");
            if( del == EQUALS_COLON[0] ) { // equals implies one value
                //System.out.println("assignment");
					 if(fpp.storeValues) {
						 fpp.attributes.put(key, value);
					 }
                return value;
            } else if(del == EQUALS_COLON[1] ) { // many values
                //System.out.println("list");
                final Object[] obs = breakString(value, fpp, 0);
					 if(fpp.storeValues) {
						 fpp.attributes.put(key, obs);
					 }
                return obs;
            } else { // assume a remark
               // System.out.println("remark (should these be handled here?)");
                fpp.addRemark(value, null);
                return value;// is this right?
            }
        }
        
        // implemented abstract methods
        
        /** throw the Exception with the proper information
         * This is overridden to do nothing
         */
        protected void createException(int cnt, InnerParser fpp) throws java.text.ParseException {
            // not implemented
        }
        
        /** sets the appropriet array in FeaturesetPropertiesParser
         * This is overridden to do nothing
         */
        protected void setArray(Object[] values, InnerParser fpp) {
            // not implemented
        }
        
    }// end DefaultParser
    //////
//    /**  */
//    private static class  extends AbstractKeywordParser {
//        
//        () {
//            this(FeaturesetPropertiesEncoder.);
//        }
//        private (final String kw) {
//            super(kw);
//        }
//        
//        /** parses the value  */
//        public void parse(final String value, final InnerParser fpp) throws ParseException {
//
//        }
//        
//    }// end 

}
