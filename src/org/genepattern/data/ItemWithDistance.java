/*
 * ItemWithDistance.java
 *
 * Created on December 18, 2002, 10:56 AM
 */

package org.genepattern.data;

/**
 * This is an Item that supports a distance property
 * @author  kohm
 */
public class ItemWithDistance extends Template.Item {
    
    /** Creates a new instance of ItemWithDistance */
    public ItemWithDistance(final SampleLabel label, final String id, int pos, float distance) {
        super(label, id, pos);
        this.distance = distance;
    }
    
    /** returns the distance */
    public final float getDistance() {
        return distance;
    }
    /** returns a String representation */
    public String toString() {
        return super.toString() + " Distance "+distance;
    }
    // fields
    /** the distance */
    private final float distance;
    /** the Comparator */
    public static final java.util.Comparator DISTANCE_COMPARATOR = new DistanceComparator();
    // I N N E R   C L A S S E S
    
    /** Compares ItemWithDistance objects based on their distance*/
    static class DistanceComparator implements java.util.Comparator {
	public int compare(final Object o1, final Object o2) {
	    ItemWithDistance d1 = (ItemWithDistance) o1;
	    ItemWithDistance d2 = (ItemWithDistance) o2;
	    return compare(d1, d2);
	    //System.out.println(" Wrong compare being called");
	}

	public int compare(final ItemWithDistance o1, final ItemWithDistance o2) {
            float dist_a = o1.getDistance();
            float dist_b = o2.getDistance();
            //return (int)(dist_a - dist_b); // FIXME this should work !?!?
	    if (dist_a < dist_b)
		return -1;
	    else if (dist_a > dist_b)
		return 1;
	    else
		return 0;
	}
    }
}
