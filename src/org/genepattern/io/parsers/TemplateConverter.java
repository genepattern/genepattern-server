/*
 * TemplateConverter.java
 *
 * Created on May 8, 2003, 2:40 PM
 */

package org.genepattern.io.parsers;

import org.genepattern.data.DataModel;
import org.genepattern.data.DataObjector;
import org.genepattern.data.FeaturesetProperties;
import org.genepattern.data.KlassTemplate;
import org.genepattern.data.SampleLabel;
import org.genepattern.data.Template;

/**
 * Converts a FeaturesetProperties object with DataModel == Template into
 * a Template object.
 * @author  kohm
 */
public class TemplateConverter implements FeaturesetPropertiesConverter{
    
    /** Creates a new instance of TemplateConverter */
    protected TemplateConverter() {
    }
    /** returns the instance of this FeaturesetPropertiesConverter.
     * @return FeaturesetPropertiesConverter
     */
    public static final FeaturesetPropertiesConverter instance() {
        return INSTANCE;
    }
    
    /** onverts the FeaturesetProperties object to the regular data object
     * @param fs the featureset properties object
     * @return DataObjector instance
     */    
    public DataObjector createDataObject(final FeaturesetProperties fs) {
        if( !Template.DATA_MODEL.equals(DataModel.findModel(fs.getModel())) ) {
            throw new IllegalArgumentException("The FeaturesetProperties represents a "
                +fs.getModel()+" model. It should be a "+Template.DATA_MODEL+" model!");
        }
        
        final int col_cnt = fs.getColumnCount();
        final int row_cnt = fs.getRowCount();
        final boolean is_numeric = calcIsNumeric(fs.getAttributes());
        final String[] klass_names = getKlasseNames(fs);
//        final java.util.List klass_names = new java.util.ArrayList(row_cnt);
        
        if( is_numeric ) {
            final int[] ids = new int[row_cnt];
            for(int r = 0; r < row_cnt; r++) {
                ids[r] = Integer.parseInt(fs.getValueAt(r, 1).toString());
            }
            return new KlassTemplate(fs.getName(), klass_names, ids);
        } else { // not numeric items
            final Template.Item[] items = new Template.Item[row_cnt];
            for(int r = 0; r < row_cnt; r++) {
                final String sample_name = fs.getValueAt(r, 0).toString();
                items[r] = new Template.Item(SampleLabel.create(sample_name), fs.getValueAt(r, 1).toString());
            }
            
            //create Klass objects
            final Template.Klass[] klasses = createKlasses(klass_names);
//            final Template.Klass[] klasses = new Template.Klass[klass_names.length];
//            final int limit = klasses.length;
//            for(int i = 0; i < limit; i++) {
//                klasses[i] = new Template.Klass(klass_names[i]);
//            }
            return new KlassTemplate(fs.getName(), klasses, items, true);
        }
    }
    
    // helpers
    /** determines if the values are supposed to be numeric
     * @param attrs the attributes of the featureset properties object
     * @return true if it contains numeric identifiers
     */
    protected static final boolean calcIsNumeric(final java.util.Map attrs) {
        final Object value = attrs.get("Numeric");
        return ( value != null && Boolean.valueOf(value.toString()).booleanValue() );
    }
    /** gets the Klasses from the FeaturesetProperties
     * @param fs the featureset properties object
     * @return String[], the array of klass names
     */
    protected static final String[] getKlasseNames(final FeaturesetProperties fs) {
        final Object value = fs.getAttributes().get("KLASSES");
        if( value != null ) {
            if( value instanceof String[] ) {
                final String[] klass_names = (String[])value;
                return klass_names;
            } else if( value instanceof String ) {
                final java.util.StringTokenizer toke = new java.util.StringTokenizer((String)value, "\t");
                final int cnt = toke.countTokens();
                final String[] values = new String[cnt];
                for(int i = 0; toke.hasMoreTokens(); i++) {
                    values[i] = toke.nextToken().trim();
                }
                return values;
            } else 
                throw new IllegalArgumentException("Unknown 'klasses' class value "+value);
        }
        return null;
    }
    private static final Template.Klass[] createKlasses(final String[] klass_names) {
        final int limit = klass_names.length;
        final Template.Klass[] klasses = new Template.Klass[limit];
        for(int i = 0; i < limit; i++) {
            klasses[i] = new Template.Klass(klass_names[i]);
        }
        return klasses;
    }
//    /** Test it
//     * @param args the command line arguments
//     * @throws Exception if something bad happens
//     */    
//    public static final void main(final String[] args) throws Exception {
//        System.out.println("reading the cls file...");
//        final java.io.FileInputStream in = new java.io.FileInputStream("/Users/kohm/data/test_template_encoder/Test_small.cls");
//        final Template cls_template = (Template)new ClsDataParser().parse(in, "cls_template");
//        in.close();
//        
//        System.out.println("\nreading the odf file...");
//        final java.io.FileInputStream in2 = new java.io.FileInputStream("/Users/kohm/data/test_template_encoder/Test_small.odf");
//        final Template odf_template = (Template)new FeaturesetPropertiesParser().parse(in2, "odf_template");
//        in2.close();
//        
//        System.out.println("\n\n");
//        System.out.println("cls object=");
//        ((KlassTemplate)cls_template).dump();
//        System.out.println("odf object=");
//        ((KlassTemplate)odf_template).dump();
//        System.out.println("\ndoes the cls object equal the odf object "+cls_template.equals(odf_template));
//    }
    //fields
    /** the singleton of this */
    public static final TemplateConverter INSTANCE = new TemplateConverter();
}
