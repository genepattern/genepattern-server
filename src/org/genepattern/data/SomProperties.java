/*
 * SomProperties.java
 *
 * Created on March 6, 2003, 2:53 PM
 */

package org.genepattern.data;

import java.util.Map;

/**
 *
 * @author  kohm
 */
public class SomProperties extends DefaultFeaturesetProperties {
    
    /** Creates a new instance of SomProperties */
    public SomProperties(final String name, final String[] col_names, final String[] col_descs, final String[] row_names, final String[] row_descs, final Map attributes) {
        super(name, DATA_MODEL.value.toString(), col_names, col_descs, row_names,
            row_descs, attributes);
    }
    
    /** returns a DataModel that defines the type of model this implementation
     * represents
     */
    public org.genepattern.data.DataModel getDataModel() {
        return DATA_MODEL;
    }

    
    //fields
    /** data model */
    public static final DataModel DATA_MODEL = new DataModel("SOM Cluster");
}
