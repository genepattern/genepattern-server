/*
 * UniversalDecoder.java
 *
 * Created on February 13, 2003, 11:41 AM
 */

package org.genepattern.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.DataObjector;
import org.genepattern.io.parsers.AbstractDataParser;
import org.genepattern.io.parsers.ClsDataParser;
import org.genepattern.io.parsers.DataParser;
import org.genepattern.io.parsers.FeaturesetPropertiesParser;
import org.genepattern.io.parsers.GctParser;
import org.genepattern.io.parsers.MyParser;
import org.genepattern.io.parsers.ResParser;


/**
 * This class has a static method that can parse an InputStream to produce a 
 * <CODE>DataObjector</CODE> instance.  The primary method in this class is 
 * <CODE>parse(InputStream in, String name, String ext)</CODE>.  
 *
 * Example code for loading in a Dataset:
 * <pre>
 * String file_name = "My_Big_Dataset.gct";
 * FileInputStream in = new FileInputStream(file_name);
 * Dataset dataset = (Dataset)UniversalDecoder.parse(in, "Big Dataset", file_name);
 * in.close();
 * </pre>
 * 
 * Note that using the parse method with the file extension hint (see above code example)
 * is usually much faster than using parse(InputStream, String)
 *
 * @author  kohm
 */
public class UniversalDecoder {
    
	static MyParser.ImageParser imageParser = new MyParser.ImageParser();
	static MyParser.HtmlParser htmlParser = new MyParser.HtmlParser();
	static MyParser.TextParser textParser = new MyParser.TextParser();
	static MyParser.ExcelParser excelParser = new MyParser.ExcelParser();

	public static final String EXCEPTION = "exception";
	 
    /** Creates a new instance of UniversalDecoder */
    private UniversalDecoder() {
    }
    
    /** decodes the input stream and returns a data object of some type
     *
     * @param in the input stream
     * @param name the name given to the returned data object note that the value of 
     * name is just a suggestion; the name of the object may be defined in the data
     * format of the input stream
     * @throws IOException if there was a problem reading the input stream
     * @throws ParseException if there was an error parsing the data
     * @return DataObjector the data object
     */
    public static final DataObjector parse(final InputStream in, final String name) throws IOException, ParseException {
        // all the parsers in order of priority
        final DataParser[] parsers = getParsersWithExt(name);
        final int limit = parsers.length;
        
        // read the data into a tmp file
        final String file_ext = AbstractDataParser.getLowercaseFileExtension(name);
        final File tmp_file = StorageUtils.createTempFile("UniDecode."+file_ext);
        // dump the InputStream to the temp file
        StorageUtils.writeToFile(tmp_file, in);
		  
		  if(DEBUG) {
			  System.out.println("Temp File ("+tmp_file+") for \""+name+"\": is "+tmp_file.length()+" bytes long");
        }
        for(int i = 0; i < limit; i++) {
            // read fresh the file every time
            final FileInputStream input = new FileInputStream(tmp_file);
            FileInputStream tmp_input = null;
            final DataParser parser = parsers[i];
            // debug
            //final String cname = edu.mit.genome.debug.FieldDumper.getClassNameNoPack(parser);
            final String cname = parser.getClass().getName();
				if(DEBUG) {
					System.out.println("trying parser "+cname); //debug
				}
            try {
                if( parser.canDecode(input) ) {
                    tmp_input = new FileInputStream(tmp_file);
                    final DataObjector dataobj = parser.parse(tmp_input, name);
						  if(DEBUG) {
							  System.out.println(cname+" worked!!!!!"); // debug
						  }
                    return dataobj;
                } else if(DEBUG) {
                    System.out.println(cname+" CANNOT decode 8-(");
                }
            } finally {
                if( tmp_input != null )
                    tmp_input.close();
                input.close();
            }
        }
        throw new ParseException("Unsupported input stream format! Cannot create the DataObject.", 0);
    }
    /** decodes the input stream and returns a data object of some type this is faster
     * than parse(InputStream in, String name) because the file extension determines
     * which parser to try.
     * @return DataObjector the data object
     * @param ext the file extension
     * @param in the input stream
     * @param name the name given to the returned data object note that the value of
     * name is just a suggestion; the name of the object may be defined in the data
     * format of the input stream
     * @throws IOException if there was a problem reading the input stream
     * @throws ParseException if there was an error parsing the data
     */
    public static final DataObjector parse(final InputStream in, final String name, final String ext) throws IOException, ParseException {
        if( ext != null ) {
            final DataParser parser = getParserWithExt(ext);
            if( parser == null ) //
                return parse(in, name);
            return parser.parse(in, name);
        } else 
            return parse(in, name);
    }
    /** returns the header information by parsing the input stream
     * If the file extension is known and provided the routine will try to 
     * inteligently parse the stream using the parser that is associated with
     * the file extension.  The "file extension", with_ext, could be the
     * whole file name or null if the file name is not known or doesn't have 
     * an extension.
     *
     * @param in the input stream
     * @param with_ext the file name with extension or just the extension could be null if not known
     * @throws IOException if there was a problem reading from the input stream
     * @throws ParseException if none of the parsers could interpret the initial 
     *                        data from the input stream
     * @return String the header lines
     */
    public static final String getFullHeader(final InputStream in, final String with_ext) throws IOException, ParseException{
        final DataParser[] parsers = getParsersWithExt(with_ext);
        
        //start reading the data into growable buffer that is also an input stream
        final BufferedInputStream b_in = new BufferedInputStream(in);
        b_in.mark(Integer.MAX_VALUE); // this shouldn't blowup - just reading the headers
        final int limit = parsers.length;
        for(int i = 0; i < limit; i++) {
            final DataParser parser = parsers[i];
            // debug
            //final String cname = edu.mit.genome.debug.FieldDumper.getClassNameNoPack(parser);
            final String cname = parser.getClass().getName();
				if(DEBUG) {
					System.out.println("trying parser "+cname); //debug
				}
            try {
                final String header = parser.getFullHeader(b_in);
					 if(DEBUG) {
						 System.out.println(cname+" worked!!!!!"); // debug
					 }
                b_in.close();
                return header;
            } catch (ParseException ex) {
                // ignore Exception and try next parser
            }
            // next parser must read from the beginning of the data
            b_in.reset();
        }
        b_in.close();
        
        throw new ParseException("Unsupported input stream format! Cannot read the header.", 0);
    }
    /** returns the parser that can decode the input stream.
     * If the file extension is known and provided the routine will try to 
     * inteligently parse the stream using the parser that is associated with
     * the file extension.  The "file extension", with_ext, could be the
     * whole file name or null if the file name is not known or doesn't have 
     * an extension.
     *
     * @param in the input stream
     * @param with_ext the file name with extension or just the extension could be null if not known
     * @throws IOException if there was a problem reading from the input stream
     * @return DataParser the parser that can decode the inputstream or null if none can
     */
    public static final DataParser getParser(final InputStream in, final String with_ext) throws IOException{
        final DataParser[] parsers = getParsersWithExt(with_ext);
        
        //start reading the data into growable buffer that is also an input stream
        final BufferedInputStream b_in = new BufferedInputStream(in);
        b_in.mark(Integer.MAX_VALUE); // this shouldn't blowup - just reading the headers
        final int limit = parsers.length;
        for(int i = 0; i < limit; i++) {
            final DataParser parser = parsers[i];
            // debug
            //final String cname = edu.mit.genome.debug.FieldDumper.getClassNameNoPack(parser);
            final String cname = parser.getClass().getName();
				if(DEBUG) {
					System.out.println("trying parser "+cname); //debug
            }
				
            if( parser.canDecode(b_in) ) {
					if(DEBUG) {
						System.out.println(cname+" worked!!!!!"); // debug
					}
                b_in.close();
                return parser;
            }
            // next parser must read from the beginning of the data
            b_in.reset();
        }
        b_in.close();
        
        return null;
    }
    /** returns the Summary created from the header information 
     * by parsing the input stream
     * If the file extension is known and provided the routine will try to 
     * inteligently parse the stream using the parser that is associated with
     * the file extension.  The "file extension", with_ext, could be the
     * whole file name or null if the file name is not known or doesn't have 
     * an extension.
     *
     * @param in the input stream
     * @param with_ext the file name with extension or just the extension could be null if not known
     * @throws IOException if there was a problem reading from the input stream
     * @throws ParseException if none of the parsers could interpret the initial 
     *                        data from the input stream
     * @return SummaryInfo the information from the header
     */
    public static final SummaryInfo createSummary(final InputStream in, final String with_ext) throws IOException, ParseException{
        final DataParser[] parsers = getParsersWithExt(with_ext);
		  if(DEBUG) {
			  System.out.println("Found "+parsers.length+" parsers with ext '"+with_ext+"'");
		  }
        //start reading the data into growable buffer that is also an input stream
        final BufferedInputStream b_in = new BufferedInputStream(in);
        b_in.mark(Integer.MAX_VALUE); // this shouldn't blowup - just reading the headers
        final int limit = parsers.length;
        for(int i = 0; i < limit; i++) {
            final DataParser parser = parsers[i];
            // debug
            //final String cname = edu.mit.genome.debug.FieldDumper.getClassNameNoPack(parser);
            final String cname = parser.getClass().getName();
				if(DEBUG) {
					System.out.println("trying parser "+cname); //debug
				}
            if( parser.canDecode(b_in) ) {
                 b_in.reset();
                 try {
                     final SummaryInfo summary = parser.createSummary(b_in);
							if(DEBUG) {
								System.out.println(cname+" worked!!!!!"); // debug
							}
                     b_in.close();
                     return summary;
                 } catch (ParseException ex) {
                     // ignore Exception and try next parser
                 }
            }
            // next parser must read from the beginning of the data
            b_in.reset();
        }
        b_in.close();
        
        throw new ParseException("Unsupported input stream format! Cannot read the header.", 0);
    }
    /** gets the parser associated with the file name, or extension
     * if the extension is null, empty string, whitespace, or doesn't end in
     * a recognizable extension then all the parsers are returned
     * otherwise an array containing just the parser that is associated with
     * the extension is returned.
     * @param file_name the file name with file extension
     * @return DataParser[] the data parsers that can handle the file with file extension
     */
    public static final DataParser[] getParsersWithExt(final String file_name) {
        if( file_name == null )
            return PARSERS;
        final String ext = file_name.trim().toLowerCase();
        if( ext.length() == 0 )
            return PARSERS;
        
        // find a matching extension
        final int limit = FILE_EXTS.length;
        for(int i = 0; i < limit; i++) {
            if( ext.endsWith(FILE_EXTS[i]) ) {// could be a file name with an extension
                final DataParser parser = (DataParser)FILE_EXT_TO_PARSER.get(FILE_EXTS[i]);
                if( parser != null ) {
                    return new DataParser[]{parser};
                } else {
                    throw new IllegalStateException("Internal Error: "
                        +"Some FILE_EXTS Strings are not keys of the"
                        +" FILE_EXT_TO_PARSER map!\nBad key: \""+FILE_EXTS[i]+"\"");
                }
            }
        }
        
        return PARSERS;
    }
    /** gets the data parser that is associated with the file extension or null if none
     * @param file_name the file name with file extension
     * @return DataParser, the primary data parser mapped to the file extension
     */
    public static final DataParser getParserWithExt(final String file_name) {
        if( file_name == null )
            return null;
        final String lower = file_name.trim().toLowerCase();
        if( lower.length() == 0 )
            return null;
        
        final char del = AbstractDataParser.FILE_EXT_SEPARATOR;
        final int loc = lower.lastIndexOf(del);
        if( loc < 0 )
            return null;
        final String ext = lower.substring(loc);
        //System.out.println("lower case ext is '"+ext+"'");
        // find a matching extension
        final int limit = FILE_EXTS.length;
        for(int i = 0; i < limit; i++) {
            if( ext.endsWith(FILE_EXTS[i]) ) {// could be a file name with an extension
                return (DataParser)FILE_EXT_TO_PARSER.get(FILE_EXTS[i]);
            }
        }
		  int index = file_name.lastIndexOf(".") ;
	
		  if(index != -1) {
			  String extension = file_name.substring(index + 1, file_name.length()).toLowerCase();
			  if(java.util.Arrays.asList(htmlParser.getFileExtensions()).contains(extension)) {
				  return htmlParser;
			  }
			  
			  if(java.util.Arrays.asList(imageParser.getFileExtensions()).contains(extension)) {
				  return imageParser;
			  }
			  
			  if(java.util.Arrays.asList(textParser.getFileExtensions()).contains(extension)) {
				  return textParser;
			  }
			  
			  if(java.util.Arrays.asList(textParser.getFileExtensions()).contains(extension)) {
				  return excelParser;
			  }
			 
			  
		  }
		  // change here
        return null;
    }
//    public static void main(final String[] args) {
//        final String[] names = new String[]{
//            "pred-res", "pred.res"
//        };
//        System.out.println("printing the parser associated with the ext");
//        final int limit = names.length;
//        for(int i = 0; i < limit; i++) {
//            System.out.println(names[i]+" "+getParserWithExt(names[i]));
//        }
//    }
//    /** test getFullHeader(InputStream, String)
//     * @param args standard args
//     */
//    public static final void main(final String[] args) {
//        final int num = args.length;
//        if( num == 0 ) {
//            throw new IllegalArgumentException("Usage: file1, file2, etc. or dir");
//        }
//        final File dir = new File(args[0]);
//        final boolean is_dir = dir.isDirectory();
//        if( is_dir && num > 1 ) 
//            throw new IllegalArgumentException("Cannot both specify a directory and files, either one dir or one or more files!");
//        
//        if( is_dir )
//            System.out.println("Loading dir "+dir);
//        System.out.println("");
//        final String[] file_names = ( ( is_dir ) ? dir.list() : args );
//            
//        final int limit = file_names.length;
//        System.out.println("Processing "+limit+" files.");
//        for(int i = 0; i < limit; i++) {
//            final String file_name = file_names[i];
//            final File the_file = new File(dir, file_name);
//            if( !the_file.isFile() ) {
//                System.out.println("Not file - skipping "+the_file);
//                continue;
//            }
//            if( !the_file.canRead() ) {
//                System.out.println("Cannot read - skipping "+the_file);
//                continue;
//            }
//            System.out.println("START **** Processing File "+file_name+" *****");
//            try {
//                final java.io.FileInputStream in = new java.io.FileInputStream(the_file);
//                //final String name = AbstractDataParser.getFileNameNoExt(file_name);
//                final String name = the_file.getName();
//                DataObjector data_object = parse(in, name);
//                System.out.println("RESULT  ****** File ("+file_name+") ->"+data_object);
//                data_object = null;
//                in.close();
//                //final String header = getFullHeader(in, name);
//                //System.out.println("RESULT ****** File ("+file_name+") header=\n"+header);
//            } catch (IOException ex) {
//                System.err.println("Had a problem with I/O for file \""+file_name+"\"!");
//                ex.printStackTrace();
//            } catch (ParseException ex) {
//                System.err.println("Had a problem parsing file \""+file_name+"\"!");
//                ex.printStackTrace();
//            }
//        }
//        System.out.println("Done!");
//    }
//    /** test parse(InputStream, String) */
//    public static final void main(final String[] args) {
//        final int limit = args.length;
//        if( limit == 0 ) {
//            throw new IllegalArgumentException("Usage: file1, file2, etc.");
//        }
//        
//        for(int i = 0; i < limit; i++) {
//            final String file_name = args[i];
//            System.out.println("START **** Processing File "+file_name+" *****");
//            try {
//                final java.io.FileInputStream in = new java.io.FileInputStream(file_name);
//                final DataObjector data = parse(in, file_name);
//                System.out.println("RESULT ****** File ("+file_name+") Data="
//                    +edu.mit.genome.debug.FieldDumper.getClassNameNoPack(data)+" *****");
//            } catch (IOException ex) {
//                System.err.println("Had a problem with I/O for file \""+file_name+"\"!");
//                ex.printStackTrace();
//            } catch (ParseException ex) {
//                System.err.println("Had a problem parsing file \""+file_name+"\"!");
//                ex.printStackTrace();
//            }
//        }
//        System.out.println("Done!");
//    }
    //fields
    /** all the currently know parsers
     * FIXME pick class that has this list of Parsers
     */
    private static final DataParser[] PARSERS;
    /** all the currently know parsers as an unmodifiable view of the array */
    public static final List PARSER_LIST;
    /** associates the file extensions with the parser instance 
     * note that the file extensions are lower case and with out the file
     * file-extension-separator  usually the dot '.'
     */
    public static final Map FILE_EXT_TO_PARSER;
    /** all file extensions known */
    private static final String[] FILE_EXTS;
    /** an unmodifiable view of the file extension array */
    public static final List FILE_EXTENSIONS;
    
	 /** whether to print debugging messages */
	 static final boolean DEBUG = false;
	 
    /** static initializer */
    static { 
        PARSERS = new DataParser[] {
            new FeaturesetPropertiesParser(), new GctParser(),
            new ResParser(), new ClsDataParser(), imageParser, htmlParser, textParser, excelParser
        };
        PARSER_LIST = java.util.Collections.unmodifiableList(java.util.Arrays.asList(PARSERS));
        
        // associate all the file extensions to each parser instance
        final java.util.List list = new java.util.ArrayList();
        
        final Map map = new HashMap();
        final int limit = PARSERS.length;
        for(int i = 0; i < limit; i++) {
            final DataParser parser = PARSERS[i];
            final String[] exts = parser.getFileExtensions();
            final int cnt = exts.length;
            for(int j = 0; j < cnt; j++) {
                final String ext = exts[j];
                map.put(ext, parser);
                list.add(ext);
             }
        }
        FILE_EXT_TO_PARSER = java.util.Collections.unmodifiableMap(map);
        FILE_EXTS = (String[])list.toArray(new String[list.size()]);
        FILE_EXTENSIONS = java.util.Collections.unmodifiableList(list);
    } // end static initializer
    
}
