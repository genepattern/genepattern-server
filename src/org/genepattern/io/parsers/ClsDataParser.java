/*
 * ClsDataParser.java
 *
 * Created on August 21, 2002, 10:16 PM
 */

package org.genepattern.io.parsers;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.genepattern.data.DataObjector;
import org.genepattern.data.KlassTemplate;
import org.genepattern.data.Template;
import org.genepattern.io.DefaultSummaryInfo;
import org.genepattern.io.SummaryError;
import org.genepattern.io.SummaryInfo;
/**
 * Can parse cls like input streams and creates a Template.
 * @author  kohm
 */
public class ClsDataParser extends AbstractDataParser {
    
    /** Creates a new instance of ClsDataParser */
    public ClsDataParser() {
        super(new String[] {"cls"});
        tmp_parser = new InnerParser(new LineReader());
    }
//    /** Creates a new instance of ClsDataParser */
//    public ClsDataParser(final InputStream input, final String cls_name) throws IOException, org.genepattern.io.ParseException{
//        super(new String[] {"cls"});
//        this.REPORTER = GenePattern.getReporter();
//        if( input != null ) {
//            final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
//            parse(reader, cls_name);
//        }
//    }
//    /** Creates a new instance of ClsDataParser */
//    public ClsDataParser() throws IOException, org.genepattern.io.ParseException {
//        this(null, null);
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
            //parser.processHeader();
            parser.parse("test");
            return true;
        } catch (java.text.ParseException ex) {
            //System.out.println(getClass()+" cannot decode stream "+ex.getMessage());
        } catch (EOFException ex) {
            
        }
        return false;
    }
    
    /** Parses the InputStream  to create a DataObjector instance with the specified name
     * @return the resulting data object
     * @param name the name of the resulting Template object
     * @param in the input stream
     * @throws IOException if a problem occurs due to an I/O operation
     * @throws org.genepattern.io.ParseException if there is a problem with the format or content of the data
     */
    public DataObjector parse(InputStream in, String name) throws IOException, java.text.ParseException {
        final InnerParser parser = new InnerParser(new LineReader(in));
        return parser.parse(name);
        //return parser.getTemplate();
    }
    
//    /** gets the classifiers by prompting the user */
//    private final void getClassifiersFromUser(int[] classIndices) {
//        ClassifierDialog dialog = new ClassifierDialog(null, classIndices);
//        dialog.show();
//        names = dialog.getClassLabels();
//    }
//    /** creates a classifier line from the names */
//    private final String createClassifiersLine() {
//        StringBuffer sb = new StringBuffer("#");
//        for(int i = 0, limit = names.length; i < limit; i++) {
//            sb.append (" ");
//            sb.append (names[i]);
//        }
//        return sb.toString ();
//    }
//    /** creates a new file based on the old filename */
//    private final File createNewName(File file) {
//        String old_name = file.getAbsolutePath ();
//        int ext = old_name.lastIndexOf (EXTENSION);
//        String new_name = old_name.substring(0, ext) + "_fixed";
//        File new_file = new File(new_name + EXTENSION);
//        for(int i = 1; new_file.exists (); i++){ 
//            new_file = new File(new_name + i + EXTENSION);
//        }
//        return new_file;
//    }
//    /** writes the three lines of the class file */
//    private final void saveClassifer (String first, String second, String third, File file) throws IOException{
//        PrintWriter out = new PrintWriter (new BufferedWriter (new FileWriter (file)));
//        out.println (first);
//        out.println (second);
//        out.println (third);
//        out.close ();
//        CMJAUI.showInfo(null, "Fixed version of class file saved as:\n"+file);
//    }
    /** reads the header lines and creates a summary
     * @param in input stream
     * @throws IOException if error durring I/O operation
     * @throws org.genepattern.io.ParseException if format problem with data
     * @return Summaryinfo
     */
    public SummaryInfo createSummary(final InputStream in) throws IOException, java.text.ParseException {
        final LineReader reader = new LineReader(in);
        final InnerParser parser = new InnerParser(reader);
        final Map primary = new HashMap(6);
        Exception exception = null;
        try {
			  	primary.put("Size=", (long)(Math.ceil(in.available()/1024.0)) + " KB");
            parser.processHeader(reader.readLine(), primary);
        } catch (java.text.ParseException ex) {
            exception = ex;
            REPORTER.logWarning(getClass()+" cannot decode stream "+ex.getMessage());
        } catch (IOException ex) {
             exception = ex;
            REPORTER.logWarning(getClass()+" cannot read stream "+ex.getMessage());
        }
        if( exception != null )
            return new SummaryError(null, exception, this, primary, null);
        return new DefaultSummaryInfo(primary, null, Template.DATA_MODEL);
    }
    
    /** reads the header lines and returns them as a String
     * @param in input stream
     * @throws IOException if error durring I/O operation
     * @throws org.genepattern.io.ParseException if format problem with data
     * @return String the header
     */
    public String getFullHeader(InputStream in) throws IOException, java.text.ParseException {
        final StringBuffer buf = new StringBuffer();
        final InnerParser parser = new InnerParser(new LineRecorder(in, buf));
        try {
            //parser.processHeader();
            parser.parse("test");
        } catch (java.text.ParseException ex) {
            System.err.println("While getting the full header "+getClass()
                        +" cannot decode stream. Error:\n"+ex.getMessage());
        }
        return buf.toString();
    }
    
    // fields
    /** scratch inner parser that should be useable for canDecode() and getSummaryInfo() */
    private final InnerParser tmp_parser;
    
    // I N N E R   C L A S S E S
    /** the real parser behind the curtains */
    protected static class InnerParser {
        
        /** The inner parser that does most of the work.
         * @param reader the line reader
         */        
        protected InnerParser(final LineReader reader) {
            this.reader = reader;
        }
        
//        /** gets the Template */
//        public final Template getTemplate() {
//            return template;
//        }
        Template parse(final String cls_name) throws IOException, java.text.ParseException, RuntimeException {
            String header_line, classifier_line, data_line;
            int[] classIndices = null;
            String[] names = null;
            int[] values = null;
            try {
                // header line: <num_data> <tab> <num_classes> <tab> <numAssignments>*
                header_line = reader.readLine();
                
                final Integer[] header_values = processHeader(header_line);
                final int num_items = header_values[0].intValue(), num_classes = header_values[1].intValue();
                values = new int[num_items];
                classifier_line = reader.readLine();
                
                if(isClassifier(classifier_line)) {
                    names = processClassifier(classifier_line, num_classes);
                    data_line = reader.readLine();
                    processData(data_line, num_classes, values, num_items);
                } else { // assume classifier line was skipped (second line) so try it as data
                    data_line = classifier_line;
                    try {
                        classIndices = processData(data_line, num_classes, values, num_items);
                        names = createNames(classIndices);
                        // the rest is done after the three main catch statements below
                    } catch (NumberFormatException ex) {
                        // It was discovered that the data line didn't have numbers
                        // so it's assumed that each datum is a class label
                        names = processDataAsArbitraryStrings(data_line, num_classes, values, num_items);
                    }
                }
                
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                String msg = "Classifier file error: " + e.getMessage()
                + "\nreading " + cls_name + " line: \""+reader.getCurrentLine()+"\"";
                //REPORTER.showError(msg);
                throw e;
            } catch (java.text.ParseException e) {
                String msg = "Classifier file error: " + e.getMessage()
                + "\nreading " + cls_name + " line: \""+reader.getCurrentLine()+"\"";
                //REPORTER.showError(msg);
                throw e;
            } catch (RuntimeException e) {
                String msg = "Classifier file error: " + e.getMessage()
                + "\nreading " + cls_name + " line: \""+reader.getCurrentLine()+"\"";
                //REPORTER.showError(msg);
                throw e;
            }
            //FIXME get this working again...
            //        if(classIndices != null) { //so the new cls file can be written
            //            try {
            //                getClassifiersFromUser (classIndices);// and get the classifiers from the user
            //                classifier_line = createClassifiersLine ();
            //                File new_file = createNewName (fFile);
            //                saveClassifer (header_line, classifier_line, data_line, new_file);
            //            } catch (IOException e) { // don't rethrow exception because main operation completed
            //                String msg = "Error Writting Classifier file:\n" + e.getMessage ();
            //                REPORTER.showError (null, msg);
            //                REPORTER.logWarning (msg); // need to log it
            //            }
            //        }
            return new KlassTemplate(cls_name, names, values);
        }
        private final Integer[] processHeader(final String header_line) throws IOException, java.text.ParseException{
            return processHeader(header_line, null);
        }
        private final Integer[] processHeader(final String header_line, final Map primary) throws IOException, java.text.ParseException{
            if( header_line == null )
                throw new java.text.ParseException("CLS Format error\nNo header line",0);
            final Integer[] hdrInts = new Integer[3];            
            StringTokenizer tok = new StringTokenizer(header_line, " \t");
            final int num = tok.countTokens();
            if( num != 3 ) {
                throw new java.text.ParseException("CLS Format error\nHeader line needs three numbers!\n"
                    +"\""+header_line+"\"", 0);
            }
            try {
                for(int i = 0; i < num; i++) {
                    hdrInts[i] = Integer.valueOf(tok.nextToken().trim());//.parseInt(tok.nextToken());
                   // System.out.println(hdrInts[i]);
                }
            } catch (NumberFormatException e) {
                throw new java.text.ParseException("CLS Format error Header line element '"+e.getMessage()+"' is not a number!", 0);
            } finally {
                if( primary != null ) {
                    Object value = hdrInts[0];
                    if( value != null ) {
                        primary.put("Data points=", value);
                    }
                    value = hdrInts[1];
                    if( value != null ) {
                        primary.put("Classes=", value);
                    }
                    value = hdrInts[2];
                  //  if( value != null ) {
                    //    primary.put("num_assignments=", value);
                   // }
                }
            }
           // System.out.println("header ints from line:\n"+header_line);
          //  for(int i = 0; i < num; i++) {
           //     System.out.println(hdrInts[i]);
          //  }
            int num_data, num_classes, num_assignments;
            int value = hdrInts[0].intValue();
            if (value > 0) {
                num_data = value;
            } else
                throw new java.text.ParseException("CLS Format error\nHeader line missing first number, num data", 0);
            value = hdrInts[1].intValue();
            if (value > 0) {
                num_classes = value;
            } else
                throw new java.text.ParseException("CLS Format error\nHeader line missing second number, num classes", 0);
            value = hdrInts[2].intValue();
            if (value > 0) {
                num_assignments = value;
                if(num_assignments > 1)
                    REPORTER.logWarning("Classifiers file format: Header line third number assigns the number of assignments.\n"
                    +"Current value is "+num_assignments+" but currently program only supports one assignment.\n"
                    +"Ignoring value...");
            } else
                REPORTER.logWarning("Classifiers file format: Header line missing third number, num assignments");
            //System.out.println("CClassAssignmentsFile::run  NumData: " + num_data + "  NumClasses: " + num_classes);
            return hdrInts;
        }
        /** helper for interpreting Integers */
        private Object interpretInteger(final Integer integer) {
            if( integer == null )
                return null;
            return ((integer.intValue() > 0) ? (Object)integer : (Object)("Illegal value ("+integer+")"));
        }
        private final boolean isClassifier(String classifier_line) {
            return (classifier_line != null && classifier_line.length() > 2 && classifier_line.startsWith("#"));
        }
        private final String[] processClassifier(String classifier_line, final int num_classes) throws java.text.ParseException {
            // optional: class label line, otherwise the data line
            // # Breast Colon Pancreas ...
            
            // remove the # because it could be "# CLASS1" or "#CLASS1"
            classifier_line = classifier_line.substring(classifier_line.indexOf('#') + 1);
            StringTokenizer st = new StringTokenizer(classifier_line);
            if(st.countTokens() != num_classes)
                throw new java.text.ParseException("first line specifies "+ num_classes
                +" classes,\nbut found "+(st.countTokens())+"!", 0);
            String[] names = new String[num_classes];
            for(int ic = 0; st.hasMoreTokens(); ic++) {
                names[ic] = st.nextToken();
            }
            return names;
        }
        private final int[] processData(String data_line, final int num_classes, final int[] values, final int num_data) throws IOException, java.text.ParseException {
            // data line <int0> <space> <int1> <space> <int2> <space> ...
            if(data_line == null) {
                throw new java.text.ParseException(
                "Error:\nMissing data (numbers seperated by spaces) in 3rd line", 0);
            }
            try {
                boolean[] class_used = new boolean[num_classes];
                int classCount = 0;
                int dataInd = 0;
                
                // String to int and int to String mappings for our class names
                HashMap fClassNameMap = new HashMap(2 * num_classes);
                int[] classIndices = new int[num_classes];
                do {
                    //current.line = data_line;
                    StringTokenizer st = new StringTokenizer(data_line);
                    while (st.hasMoreTokens()) {
                        String className = st.nextToken().trim();
                        
                        final int index = Integer.parseInt(className);
                        if(index >= num_classes  || index < 0)
                            throw new java.text.ParseException("Header specifies " + num_classes
                            + " classes.\nBut data line contains a "+index+", a value "
                            +"that is too "+(index < 0 ? "small" : "large")+".\n"
                            +"All data values for this file must be in the range 0-"
                            +(num_classes-1)+".", 0);
                            
                            if(!class_used[index]) {
                                class_used[index] = true;
                                classIndices[classCount] = index;
                                ++classCount;
                            }
                            values[dataInd] = index;
                            ++dataInd;
                    }
                    data_line = reader.readLine();
                } while (data_line != null);
                
                if (dataInd != num_data)
                    throw new java.text.ParseException("Header specifies " + num_data + " datapoints. But file contains " + dataInd + " datapoints.", 0);
                
                if (classCount != num_classes)
                    throw new java.text.ParseException("Header specifies " + num_classes + " classes. But file contains " + classCount + " classes.", 0);
                return classIndices;
            } catch (NumberFormatException ex) {
                throw new NumberFormatException(
                "All data on the data line(s) (3rd and subsequent lines) "
                +"must be numbers!");
            }
        }
        /** processes the data not as integers but as arbitrary strings
         * problem is that the strings are not associated with the classifiers */
        private final String[] processDataAsArbitraryStrings(String data_line, final int num_classes, final int[] values, final int num_data) throws IOException, java.text.ParseException {
            // data line <c0> <space> <c1> <space> <c2> <space> ...
            if(data_line == null) {
                throw new java.text.ParseException(
                "Error:\nMissing data (numbers seperated by spaces) in 3rd line", 0);
            }
            // Allow the case of 1 class per line
            //if (theDelim == '\0')
            //	throw new org.genepattern.io.ParseException("Bad delimeter");
            
            // Allow the class assignments line to have arbitrary strings as class names
            // We then map the strings (classIDs) into integers (classInds)
            int classCount = 0;
            int dataInd = 0;
            
            // String to int and int to String mappings for our class names
            HashMap fClassNameMap = new HashMap(2 * num_classes);
            String[] classNameVec = new String[num_classes];
            do {
                //current.line = data_line;
                StringTokenizer st = new StringTokenizer(data_line);
                while (st.hasMoreTokens()) {
                    String className = st.nextToken();
                    
                    Integer class_index = (Integer) fClassNameMap.get(className);
                    if (class_index == null) {
                        if(classCount  >= num_classes)
                            throw new java.text.ParseException("Header specifies " + num_classes
                            + " classes. But file contains at least "
                            + (classCount + 1) + " different  classes.", 0);
                        class_index = new Integer(classCount);
                        
                        fClassNameMap.put(className, class_index);
                        classNameVec[classCount] = className;
                        
                        ++classCount;
                    }
                    values[dataInd] = class_index.intValue();
                    ++dataInd;
                }
                data_line = reader.readLine();
            } while (data_line != null);
            
            if (dataInd != num_data)
                throw new java.text.ParseException("Header specifies " + num_data + " datapoints. But file contains " + dataInd + " datapoints.", 0);
            
            if (classCount != num_classes)
                throw new java.text.ParseException("Header specifies " + num_classes + " classes. But file contains " + classCount + " classes.", 0);
            return classNameVec;
        }
        
        private final String[] createNames(int[] classes) {
            final int num = classes.length;
            String[] names = new String[num];
            final String preamble = "class_";
            for(int i = 0; i< num; i++) {
                names[i] = preamble+classes[i];
            }
            return names;
        }
        
        // fields
        /** wrapper for the current line being read. Used when throwing exceptions
         * to indicate what was being read at the time of the problem */
        transient private final LineReader reader;
        /** the template */
        //private Template template;
    } // end InnerParser
}
