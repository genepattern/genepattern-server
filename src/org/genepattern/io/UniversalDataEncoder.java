/*
 * UniversalDataEncoder.java
 *
 * Created on February 14, 2003, 3:38 PM
 */

package org.genepattern.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.io.encoder.DatasetEncoder;
import org.genepattern.io.encoder.Encoder;
import org.genepattern.io.encoder.FeaturesetPropertiesEncoder;
import org.genepattern.io.encoder.GctDatasetEncoder;
import org.genepattern.io.encoder.TemplateEncoder;
import org.genepattern.io.encoder.XmlSerializer;

//import org.genepattern.io.encoder.Encoder;

/**
 * This class has many static methods for saving a data object (DataObjector) 
 * to an output stream. An example code fragment for saving a Dataset to a file is:
 * <pre>
 * Dataset transposed_dataset = ...;
 * String file_name = transposed_dataset.getName() + getFileExtensionFor(transposed_dataset);;
 * OutputStream out = new FileOutputStream(file_name);
 * UniversalDataEncoder.encode(transposed_dataset, out);
 * out.closed();
 * </pre>
 *
 * Note that the default <CODE>Encoder</CODE> will be a odf encoder unless the 
 * data object's <CODE>DataModel</CODE> was mapped to a different 
 * <CODE>Encoder</CODE> with <CODE>mapModelToEncoder(DataModel, Encoder)</CODE>.
 *
 * Another way to do this is by specifying which encoder to get:
 * <pre>
 * Dataset transposed_dataset = ...;
 * Encoder encoder = UniversalDataEncoder.getEncoder("gct dataset");
 * if( encoder != null ) {
 *    String file_name = transposed_dataset.getName() + encoder.getFileExtension(transposed_dataset);
 *    OutputStream out = new FileOutputStream(file_name);
 *    encoder.write(transposed_dataset, out);
 *    out.close();
 * }
 * </pre>
 *
 * The above code will save the transposed_dataset as a gct file instead of
 * the default which is an odf file.
 *
 * The following keys are defined for various <CODE>Encoder</CODE>s:
 * "odf dataset" - save a Dataset as a odf file
 * "gct dataset" - save a Dataset as a gct file
 * "odf template" - save a Template (Class Vector) as a odf file
 *
 * Note that res and cls <CODE>Encoder</CODE>s are not yet implemented.
 * Also if a data object is represented as a FeaturesetProperties object or subset
 * of FeaturesetProperties then it is saved in odf format without the need for
 * any special <CODE>Encoder</CODE> besides the default one that handles 
 * <CODE>FeaturesetProperties</CODE>.
 *
 *
 * FIXME problem is that DataModels are mapped to encoders.  But it should
 * be that classes are mapped to encoders.  Reason is that more than one class
 * can represent a DataModel.  For example a Dataset can be represented in memory
 * by a DefaultDataset or a DefaultFeaturesetProperties objects.  These two classes 
 * require different encoders.  Note a DefaultFeaturesetProperties instance is 
 * usually transformed by a DatasetConverter into a Dataset.
 *
 * @author  kohm
 * @see org.genepattern.io.UniversalDecoder
 */
public class UniversalDataEncoder implements Encoder{
    
    /** Creates a new instance of UniversalDataEncoder */
    private  UniversalDataEncoder() {
        //  Dataset     //
        mapModelToEncoder(org.genepattern.data.Dataset.DATA_MODEL,
                          DatasetEncoder.instance());
        mapTextToEncoder("odf_dataset", DatasetEncoder.instance()); // default for dataset
        mapTextToEncoder("gct_dataset", GctDatasetEncoder.instance());
        //mapTextToEncoder("res dataset", ResDatasetEncoder.instance());
        
        //  Template    //
        mapModelToEncoder(org.genepattern.data.Template.DATA_MODEL,
                          TemplateEncoder.instance());
        mapTextToEncoder("odf_class_vector", TemplateEncoder.instance());
        //mapTextToEncoder("cls template", ClsTemplateEncoder.instance());
        
        //  SOMCluster  //
        // this next line is taken care of by the 
        //edu.mit.genome.gp.alg.unsupervised.SomAlg
        //mapModelToEncoder(org.genepattern.data.SomProperties.DATA_MODEL,
        //                  FeaturesetPropertiesEncoder);
        
        //  Default for any superclass of FeaturesetProperties //
        mapModelToEncoder(org.genepattern.data.FeaturesetProperties.DATA_MODEL,
                          FeaturesetPropertiesEncoder.instance());
    }
    /** factory method
     * @return <CODE>Encoder</CODE> the singleton of this class
     *
     */
    public static final Encoder instance() {
        return INSTANCE;
    }
    
    /** returns the Encoders appropriet for the data object or else null if none
     * determines this by using querying all encoders with canEncode(DataObjector)
     * @param data The data object to encode
     * @return <CODE>Encoder[]</CODE> an array of encoders that can be used
     */
    public static final Encoder[] getEncoders(final DataObjector data) {
        final int limit = ENCODERS.length;
        final List list = new ArrayList(limit);
        for(int i = 0; i < limit; i++) {
            final Encoder encoder = ENCODERS[i];
            if( encoder.canEncode(data) )
                list.add(encoder);
        }
        final int num = list.size();
        if( num == 0 )
            return null;
        return (Encoder[])list.toArray(new Encoder[num]);
    }

    /** encodes the data object to the output stream or throws an
     * UnsupportedEncodingException if the data object is not supported 
     * or an IOException if there are I/O problems.
     * @param data the data object to encode
     * @param out the out put stream to save to
     * @throws IOException if an error occures during an IO operation
     * @throws UnsupportedEncodingException if the data object is not supported 
     * @return <CODE>Encoder</CODE> the encoder used to save the data object
     */
    public static final Encoder encode(final DataObjector data, final OutputStream out) throws IOException, UnsupportedEncodingException {
        final Encoder encoder = getEncoderFor(data);

        if( encoder == null ) {
            throw new UnsupportedEncodingException("The data object, "+data+", does not have "
                +"an encoder");
        }
        encoder.write(data, out);
        return encoder;
    }
    /** gets the file extension for the specified data object or null if 
     * there are no Encoders known.
     * @param data the data object
     * @return a file extension that is associated with the encoder
     */
    public static final String getFileExtensionFor(final DataObjector data) {
        final Encoder encoder = getEncoderFor(data);
        if( encoder == null) {
            System.err.println("Couldn't get encoder for: " + data.getDataModel());
            return null;
        }
        return encoder.getFileExtension(data);
    }
    /** gets the <CODE>Encoder</CODE> associated with the <CODE>DataModel</CODE>
     * @param data_model the data model to get an encoder for
     * @return <CODE>Encoder</CODE> is returned that was associated with the <CODE>DataModel</CODE>
     */
    public static final Encoder getEncoderFor(final DataObjector data) {
        
        final Encoder encoder = (Encoder)MODEL_ENCODER.get(data.getDataModel());
        if( encoder == null && data instanceof FeaturesetProperties )
            return FeaturesetPropertiesEncoder.instance();
        else
            return encoder;
    }
    /** gets the encoder that is associated with the key text */
    public static final Encoder getEncoder(final String key) {
        return (Encoder)TEXT_TO_ENCODER.get(key);
    }
    // Encoder interface method signature
    
    /** returns true if this can handle encoding the specified DataObjector
     * @param data the data object
     * @return boolean true if the <CODE>DataObjector</CODE> has an encoder that
     * can save it
     */
    public final boolean canEncode(final DataObjector data) {
        return (getEncoderFor(data) != null);
    }

    /** gets the file extension for the specified data object or null if 
     * there are no Encoders known.
     * @param data the data object
     * @return a file extension that is associated with the encoder
     */
    public final String getFileExtension(final DataObjector data) {
        return getFileExtensionFor(data);
    }
    
    /** encodes the data to the output stream
     * @param data the data object
     * @param out the output stream where the data object will be saved
     * @throws IOException if an IO error occures
     */
    public void write(final DataObjector data, final OutputStream out) throws IOException {
        try {
            encode(data, out);
        } catch (UnsupportedEncodingException ex) {
            throw new IOException(ex.getMessage());
        }
    }
    
    // static setter methods
    /** adds another DataModel, Encoder mapping
     * @param data_model the data model to associate with the encoder
     * @param encoder the encoder that is associated with the data model
     * @return true if the data model was not entered previously
     */
    public static final boolean mapModelToEncoder(final DataModel data_model, final Encoder encoder) {
//        if( MODEL_ENCODER.containsKey(data_model) )
//            throw new DuplicateKeyException("Cannot have a DataModel mapped to more than one Encoder!");
        return ( MODEL_ENCODER.put(data_model, encoder) == null );
    }
    /** maps a text String to an <CODE>Encoder</CODE>
     * @param key the key that will be used to retrieve the encoder
     * @param encoder the encoder to retrieve
     * @return true if it worked otherwise it replaced an old encoder that was
     * mapped to the same text
     */    
    public static final boolean mapTextToEncoder(final String key, final Encoder encoder) {
//        if( TEXT_TO_ENCODER.containsKey(key) )
//            throw new DuplicateKeyException("Cannot have a text key mapped to more than one Encoder!");
        return ( TEXT_TO_ENCODER.put(key, encoder) == null );
    }
    
    //fields
    /** the Encoders */
    private static final Encoder[] ENCODERS = new Encoder[] {
        FeaturesetPropertiesEncoder.instance(),
        DatasetEncoder.instance(), GctDatasetEncoder.instance(),
        //ResDatasetEncoder.instance(),
        TemplateEncoder.instance(), // ClsTemplateEncoder.instance(),
        XmlSerializer.instance()
    };
    /** maps the DataModel to the encoder */
    private static final Map MODEL_ENCODER = new HashMap();
    /** maps text to a specific encoder */
    private static final Map TEXT_TO_ENCODER = new HashMap();
    /** the singleton instance of this class */
    public static final UniversalDataEncoder INSTANCE = new UniversalDataEncoder();
}
