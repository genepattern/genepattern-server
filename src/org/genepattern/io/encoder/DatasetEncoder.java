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
 * DatasetEncoder.java
 *
 * Created on May 6, 2003, 5:23 PM
 */

package org.genepattern.io.encoder;

import java.io.IOException;
import java.io.OutputStream;

import org.genepattern.data.DataObjector;
import org.genepattern.data.Dataset;
/** Converts a Dataset object to a FeaturesetProperties oject to encode it as a odf
 * file format.
 * @author kohm
 */
public class DatasetEncoder extends FeaturesetPropertiesEncoder implements Encoder {
    
    /** Creates a new instance of DatasetEncoder */
    private  DatasetEncoder() {
    }
    
    /** returns true if this can handle encoding the specified DataObjector
     * @param data must be a dataset object
     * @return true if it was a <CODE>Dataset</CODE>
     */
    public boolean canEncode(DataObjector data) {
        return ( data instanceof Dataset );
    }
    
//    /** gets the file extension for the specified object or null if wrong type  */
//    public String getFileExtension(DataObjector data) {
//    }
    
    /** encodes the data to the output stream
     * @param data the dataset
     * @param out the outputstream to write to
     * @throws IOException if an problem occurs durring I/O
     */
    public void write(final DataObjector data, final OutputStream out) throws IOException {
        final Dataset dataset = (Dataset)data;
        super.write(new DatasetPropertiesWrapper(dataset), out);
    }
    // static methods
    /** gets the instance
     * @return DatasetEncoder, the singleton
     */
    public static Encoder instance() {
        return INSTANCE;
    }
    
//    /** test it
//     * @param args the arguments to the command line
//     */
//    public static final void main(final String[] args) {
//        final int num = args.length;
//        if( num == 0 ) {
//            throw new IllegalArgumentException("Usage: file1, file2, etc. or dir");
//        }
//        final java.io.File dir = new java.io.File(args[0]);
//        final boolean is_dir = dir.isDirectory();
//        if( is_dir && num > 1 ) 
//            throw new IllegalArgumentException("Cannot both specify a directory and files, either one dir or one or more files!");
//        
//        if( is_dir )
//            System.out.println("Loading dir "+dir);
//        System.out.println("");
//        final java.io.FilenameFilter filter = new java.io.FilenameFilter() {
//            public final boolean accept(final java.io.File dir, final String name) {
//                final String lower = name.toLowerCase().trim();
//                return ( lower.endsWith(".gct") || lower.endsWith(".res") );
//            }
//        };
//        final String[] file_names = ( ( is_dir ) ? dir.list(filter) : args );
//            
//        final int limit = file_names.length;
//        System.out.println("Processing "+limit+" files.");
//        for(int i = 0; i < limit; i++) {
//            final String file_name = file_names[i];
//            final java.io.File the_file = new java.io.File(dir, file_name);
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
//                final String name = org.genepattern.io.parsers.AbstractDataParser.getFileNameNoExt(file_name);
//                
//                DataObjector data_object = org.genepattern.io.UniversalDecoder.parse(in, file_name);
//                in.close();
//                System.out.println("RESULT  ****** File ("+file_name+") ->"+data_object);
//                if( data_object instanceof Dataset ) {
//                    final java.io.File odf_file = new java.io.File(dir, name + ".odf");
//                    try {
//                        System.out.println("Writting "+odf_file);
//                        final java.io.FileOutputStream out = new java.io.FileOutputStream(odf_file);
//                        INSTANCE.write(data_object, out);
//                        System.out.println("worked!");
//                        out.close();
//                    } catch (IOException ex) {
//                        System.err.println("Had a problem with I/O for file \""+odf_file.getName()+"\"!");
//                        ex.printStackTrace();
//                    } 
//                }
//                data_object = null;
//                //final String header = getFullHeader(in, name);
//                //System.out.println("RESULT ****** File ("+file_name+") header=\n"+header);
//            } catch (IOException ex) {
//                System.err.println("Had a problem with I/O for file \""+file_name+"\"!");
//                ex.printStackTrace();
//            } catch (java.text.ParseException ex) {
//                System.err.println("Had a problem parsing file \""+file_name+"\"!");
//                ex.printStackTrace();
//            }
//        }
//        System.out.println("Done!");
//    }
    
    // fields
    /** the instance */
    private static final DatasetEncoder INSTANCE = new DatasetEncoder();
}
