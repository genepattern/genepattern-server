/*
 * DatasetConverter.java
 *
 * Created on May 8, 2003, 2:40 PM
 */

package org.genepattern.io.parsers;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.data.Dataset;
import org.genepattern.data.DefaultDataset;
import org.genepattern.data.DefaultFeaturesetProperties;
import org.genepattern.data.DefaultMatrix;
import org.genepattern.data.DefaultNamesPanel;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.data.annotation.AnnotationFactory;
import org.genepattern.data.annotation.PrimeAnnotationFactory;
/**
 * Converts a FeaturesetProperties object with DataModel == Dataset into
 * a Dataset object.
 * @author  kohm
 */
public class DatasetConverter implements FeaturesetPropertiesConverter{
    
    /** Creates a new instance of DatasetConverter */
    protected DatasetConverter() {
    }
    /** returns the instance of this FeaturesetPropertiesConverter
     * @return FeaturesetPropertiesConveter the instance
     */
    public static final FeaturesetPropertiesConverter instance() {
        return INSTANCE;
    }
    
    /** Creates the Dataset from the FeaturesetProperties.
     * @param fs the FeaturesetProperties
     * @return DataObjector a <CODE>Dataset</CODE>
     */    
    public DataObjector createDataObject(final FeaturesetProperties fs) {
        if( !Dataset.DATA_MODEL.equals(DataModel.findModel(fs.getModel())) ) {
            throw new IllegalArgumentException("The FeaturesetProperties represents a "
                +fs.getModel()+" model. It should be a "+Dataset.DATA_MODEL+" model!");
        }
        
        final int real_col_cnt = fs.getColumnCount();
        final int col_cnt = real_col_cnt - 2;
        final int row_cnt = fs.getRowCount();
        final float[] data_array = new float[row_cnt * col_cnt];
        
        for(int r = 0, i = 0; r < row_cnt; r++) {
            for(int c = 2; c < real_col_cnt; c++) {
                data_array[i++] = ((DefaultFeaturesetProperties.SimpleFloat)fs.getValueAt(r, c)).getFloat();
            }
        }
        
        final String[] row_names = fs.getRowNames(null);
        final String[] col_names = createArrayTwoLess(fs.getColumnNames(null));
        AnnotationFactory rfactory = PrimeAnnotationFactory.createAnnotationFactory(row_names, fs.getRowDescriptions(null));
        AnnotationFactory cfactory = PrimeAnnotationFactory.createAnnotationFactory(col_names, createArrayTwoLess(fs.getColumnDescriptions(null)));
        return  new DefaultDataset(fs.getName(),
            new DefaultMatrix(row_cnt, col_cnt, data_array),
            new DefaultNamesPanel(row_names, rfactory),
            new DefaultNamesPanel(col_names, cfactory));
    }
    
    // helpers
    /** this shouldn't be needed */
    private String[] createArrayTwoLess(final String[] array) {
        if( array == null )
            return null;
        final int len = array.length - 2;
        final String[] new_array = new String[len];
        System.arraycopy(array, 2, new_array, 0, len);
        return new_array;
    }
    
//    public static final void main(final String[] args) throws Exception {
//        System.out.println("reading the res/gct file...");
//        final java.io.FileInputStream in = new java.io.FileInputStream("/Users/kohm/data/test_ds_encoder/Prostate_BiopsyTrial_ams.res");
//        final Dataset res_dataset = (Dataset)new ResParser().parse(in, "From res");
//        in.close();
//        
//        System.out.println("\nreading the odf file...");
//        final java.io.FileInputStream in2 = new java.io.FileInputStream("/Users/kohm/data/test_ds_encoder/Prostate_BiopsyTrial_ams.odf");
//        final Dataset odf_dataset = (Dataset)new FeaturesetPropertiesParser().parse(in2, "From odf");
//        in2.close();
//        
//        System.out.println("\n\n");
//        System.out.println("res object=");
//        org.genepattern.data.Datasets.datasetDump(res_dataset);
//        System.out.println("odf object=");
//        org.genepattern.data.Datasets.datasetDump(odf_dataset);
//        System.out.println("\ndoes the res object equal the odf object "+res_dataset.equals(odf_dataset));
//    }
    
    //fields
    /** the singleton of this */
    public static final DatasetConverter INSTANCE = new DatasetConverter();
}
