/*
 * FeaturesetPropertiesConverter.java
 *
 * Created on May 8, 2003, 3:24 PM
 */

package org.genepattern.io.parsers;

import org.genepattern.data.DataObjector;
import org.genepattern.data.FeaturesetProperties;

import org.genepattern.io.*;
/**
 * Classes that implement this interface convert a FeaturesetProperties object 
 * to another DataObjector.
 *
 * @author  kohm
 */
public interface FeaturesetPropertiesConverter {
    /** converts the FeaturesetProperties to another DataObjector
     * @param fs the featurset properties object
     * @return DataObjector, the data object
     */
    public DataObjector createDataObject(final FeaturesetProperties fs);
    
    // fields
    /** the "Do Nothing" converter */
    public static final FeaturesetPropertiesConverter PASSTHOUGH_CONVERTER = new FeaturesetPropertiesConverter() {
            public final DataObjector createDataObject(final FeaturesetProperties fs) {
                return fs;
            }
    };
}
