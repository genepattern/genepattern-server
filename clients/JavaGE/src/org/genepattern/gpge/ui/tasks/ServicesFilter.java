/*
 * ServicesFilter.java
 *
 * Created on March 27, 2003, 1:12 AM
 */

package org.genepattern.gpge.ui.tasks;

import java.util.Comparator;
import java.util.Vector;

/**
 *
 * @author  kohm
 */
public interface ServicesFilter {
    /** returns the Vector with only the services of interest */
    public Vector processServices(Vector services);
    
    // fields
    /** this Comparator can be used to sort the services
     * Collections.sort(services, COMPARE);
     */
    public static final Comparator COMPARE = new Comparator() { // sort the services
        public int compare(final Object o1,  final Object o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.toString(), o2.toString());
        }
    };
    /** doesn't actually filter just sorts the services */
    public static final ServicesFilter SORTED_SERVICES_FILTER = new ServicesFilter() {
        public Vector processServices(final Vector services) {
            java.util.Collections.sort(services, COMPARE);
            return services;
        }
    };
}
