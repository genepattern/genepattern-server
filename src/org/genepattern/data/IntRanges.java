/*
 * IntRanges.java
 *
 * Created on December 13, 2002, 2:22 PM
 */

package org.genepattern.data;

import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Represents a bunch of numbers that are not necessarily continuous.
 * i.e. 1-3 5, 7, 100
 *
 * @author  kohm
 */
public class IntRanges {
  
    /** Creates a new instance of IntRanges that will be condensed into fewer ones */
    public IntRanges(final RangeInt[] ranges) {
        final ArrayList list = new ArrayList(ranges.length + 1);
        int old_len = list.size();
        int new_len = 0;
        
        // copy to a list and sort it
        //Collections.copy(list, Arrays.asList(ranges));
        list.addAll(Arrays.asList(ranges));
        //System.out.println("Ranges:\n"+list);
        Collections.sort(list);
        // look at adjacent RangeInt objects and try to merge them together
        do { // repeat a few times until no more mergers
            old_len = list.size();
            for(int j = list.size() - 1, i = j - 1; i >= 0; i--, j--) {// rev loop
                final RangeInt r1 = ranges[j];
                final RangeInt r2 = ranges[i];
                
                if(r1.overlaps(r2)) {
                    list.set(i, RangeInt.createMerger(r1, r2));
                    list.remove(j);
                }
            }
            new_len = list.size();
        } while(old_len != new_len);
        //store the final condensed array
        this.ranges = (RangeInt[])list.toArray(new RangeInt[new_len]);
        
        int num_ints = 0;
        final int range_cnt = ranges.length;
        for(int i = 0; i < range_cnt; i++) {
            num_ints += ranges[i].getCount();
        }
        this.count = num_ints;
        
        this.min = ranges[0].min();
        this.max = ranges[range_cnt - 1].max();
    }
//    /** Creates a new instance of IntRanges */
//    public IntRanges(final RangeInt[] ranges, final int[] ints) {
//        
//    }
//    /** Creates a new instance of IntRanges */
//    public IntRanges(final RangeInt[] ranges, final Integer[] ints) {
//        
//    }
    /** Creates a new instance of IntRanges by parsing the parameter string.
     * i.e. "1-3,4 6, 7"
     * where:
     * two numbers seperated by a dash represents a range and single numbers represent
     * a number.  All numbers and ranges are seperated by a comma or just white space
     */
    public static final IntRanges parse(final String string) {
        final StringTokenizer commaTok = new StringTokenizer (string, ", ");
        final int num = commaTok.countTokens ();
        final ArrayList ranges = new ArrayList(num);
        
//        try {
        String current = null; // for error reporting
        for (int i = 0; commaTok.hasMoreTokens (); i++) {
            final StringTokenizer dashTok = new StringTokenizer (commaTok.nextToken ().trim(), "-");
            final int num_tokes = dashTok.countTokens ();
            if (num_tokes == 2) {
                final int val1 = Integer.parseInt (dashTok.nextToken ().trim());
                final int val2 = Integer.parseInt (dashTok.nextToken ().trim());
                final RangeInt range = new RangeInt(val1, val2);
                ranges.add(range);
            } else if (num_tokes == 1) {
                final int val = Integer.parseInt(dashTok.nextToken ().trim());
                final RangeInt range = new RangeInt(val, val);
                ranges.add(range);
            } else {
                throw new IllegalArgumentException("Wrong format in the number element "
                    +i+" ("+dashTok.nextToken()+")");
            }
        }
//        } catch (NumberFormatException ex) {
//            // rethrow a Warning that can display a Warning message so the user 
//            // can try again (same thing for above: IllegalArgumentException->Warning)
//        }
        return new IntRanges((RangeInt[])ranges.toArray(new RangeInt[ranges.size()]));
    }
    /**
     * creates the complement of the given min-max range from this IntRanges
     * For example:
     * if this instance defines a range of (10-20) and the min & max are 0, 50;
     * the complement would be a new IntRanges (0-9, 21-50)
     *
     * Note that the specified IntRanges has to be contained within the 
     * min and max values. Thus if this defines a range (45-55) and
     * the min max values are 0, 50; then the complement would produce
     * an error because the max value is 55 which is greater than
     * this range's max value of 50.
     *
     * Result is undefined for when the range is a single IntRanges that spans the 
     * minimum and maximum values specified.  for example this IntRanges has only
     * one RangeInt that defines the range 0-50 and the parameters are also
     * defined to be minimum is 0 and maximum is 50
     */
    public IntRanges createComplement(final int minimum, final int maximum) {
        if( min < minimum || max > maximum )
            throw new IllegalArgumentException("This IntRanges is outside"
                +" of the specified minimum or maximum values!\n"
                +"min,max: ("+minimum+", "+maximum+") other: ("+this+")");

        final ArrayList list = new ArrayList();
        
        RangeInt last_range = ranges[0];
        final int cnt = ranges.length;
        int start_i = 0;
        int new_min = minimum;
        if (last_range.min() == minimum) {
            new_min = last_range.max() + 1;
            start_i = 1;
        }
        
        // do the middle ones (simplest case)
        for(int i = start_i; i < cnt; i++) { // not first nor last
            final RangeInt range = ranges[i];
            final int new_max = range.min() - 1;
          //  System.out.println("mn="+new_min+", mx="+new_max);
            last_range = new RangeInt(new_min, new_max);
            list.add(last_range);
            new_min = range.max() + 1;
        }
            
        if( new_min <= maximum )
            list.add(new RangeInt(new_min, maximum));
        //else // new_min >= maximum so don't create RangeInt
        //    noop();
        return new IntRanges((RangeInt[])list.toArray(new RangeInt[list.size()]));
    }
    /** tests the createComplement method */
    public static final void main(String[] args) {
        final int min = 0, max = 50;
        System.out.println("min ="+min+" max ="+max);
        System.out.println();
        process("0", min, max);
        System.out.println("should be 1-50\n");
        
        process("0-1", min, max);
        System.out.println("should be 2-50\n");
        
        process("0-49", min, max);
        System.out.println("should be 50-50\n");
        
        process("1-50", min, max);
        System.out.println("should be 0-0\n");
        
        process("1-49", min, max);
        System.out.println("should be 0-0 50-50\n");
        
        process("1-3 5-9", min, max);
        System.out.println("should be 0-0 4-4 10-50\n");
        
        process("0-1 3 6 44-48", min, max);
        System.out.println("should be 2-2 4-5 7-43 49-50\n");
    }
    /** helper method for the main() */
    private static void process(final String arg, final int min, final int max) {
        final IntRanges base = parse(arg);
        //System.out.println("Base "+base);
        final IntRanges comp = base.createComplement(min, max);
       // System.out.println("comp "+comp);
    }
    // getters 
    /** gets the number of int values represented */
    public final int getCount() {
        return count;
    }
    /** returns the minimum value of these ranges */
    public final int min() {
        return min;
    }
    /** returns the maximum value of these ranges */
    public final int max() {
        return max;
    }
    // iterators 
    /** gets the int iterator */
    public final IntIterator getIntIterator() {
        return new IntIterator(ranges);
    }
    /** gets the int iterator that returns int values in reverse sort order */
    public final ReverseIntIterator getReverseIntIterator() {
        return new ReverseIntIterator(ranges);
    }
//    /** gets the RangeInt iterator that will return all RangeInt in sort order */
//    public final RangeIterator getRangeIterator() {
//        return new RangeIterator(ranges);
//    }    
    /** gets the RangeInt iterator that will return all RangeInt in reverse sort order */
    public final ReverseRangeIterator getReverseRangeIterator() {
        return new ReverseRangeIterator(ranges);
    }
    /** string rep of this object */
    public final String toString() {
        
        final int limit = ranges.length;
        String label = "RangeInt ("+limit+") [";
        for(int i = 0; i < limit; i++) {
            label += ranges[i].toString() +", ";
        }
        label +="]";
        return label;
    }
    // Fields
    /** the RangeInt and Integer objects in sorted order */
    private final RangeInt[] ranges;
    /** the number of int values represented */
    private final int count;
    /** the minimum value of this collection of ranges */
    private final int min;
    /** the maximum value of this collection of ranges */
    private final int max;
    
    // I N N E R   C L A S S E S
    /** This is a non standard iterator in that it returns a primitive int value */
    public static class IntIterator extends AbstractIntIterator{
        /** constructor */
        private IntIterator(final RangeInt[] ranges) {
            super( ranges );
        }

        /** returns true if there are more values */
        public final boolean hasMore() {
          //  System.out.println("before index="+index+" limit="+limit
          // +" current_limit="+current_limit);
            if( index <= limit ) {
                if(value >= current_limit) {
                    if( index >= limit )
                        return false;
                    index++;
                    current     = this.ranges[index];
                    current_limit = current.max();
                    value       = current.min();
                } else 
                    value++;
            //    System.out.println("after index="+index+" limit="+limit
            //    +" current_limit="+current_limit+" value="+value);    
                return true;
            }
            return false;
        }

    } // end IntIterator
    /** This is a non standard iterator in that it returns a primitive int value 
     * and in reverse order
     */
    public static class ReverseIntIterator extends AbstractIntIterator{
        /** constructor */
        private ReverseIntIterator(final RangeInt[] ranges) {
            super( ranges );
            index = limit + 1;
        }

        /** returns true if there are more values */
        public final boolean hasMore() {
            if( index >= 0 ) {
                if(value <= current_limit) {
                    if( index <= 0 )
                        return false;
                    index--;
                    current       = this.ranges[index];
                    current_limit = current.min();
                    value         = current.max();
                } else 
                    value--;
                //System.out.println("index "+index+" current="+current
                //+" current_limit="+current_limit+" value="+value);
                return true;
            }
            return false;
        }

    } // end ReverseIntIterator
    /** Iterates over the RangeInt array */
    public class ReverseRangeIterator {
        /** constructor */
        ReverseRangeIterator(final RangeInt[] ranges) {
            this.ranges = ranges;
            this.index = ranges.length;
            this.limit = 0;
        }

        /** returns true if there are more values */
        public final boolean hasMore() {
            if( index > limit ) {
                index--;
                return true;
            }
            return false;
        }
        /** returns the current value */
        public final RangeInt next() {
            return ranges[index];
        }
        
        // fields
        /** the ranges */
        protected final RangeInt[] ranges;
        /** the limit that the index is incremented or decremented */
        private final int limit;
        /** the index into the ranges array */
        protected int index;
    } // end ReverseRangeIterator
} // end IntRanges
