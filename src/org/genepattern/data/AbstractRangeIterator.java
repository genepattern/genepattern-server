package org.genepattern.data;

/** This is a non standard iterator in that it returns a primitive int value */
    public abstract class AbstractRangeIterator {
        /** constructor */
        AbstractRangeIterator(final RangeInt[] ranges) {
            this.ranges = ranges;
            int num = 0;
            final int cnt = ranges.length;
            for(int i = 0; i < cnt; i++) {
                num += ranges[i].getCount();
            }
            this.count = num;
            this.limit = cnt - 1;
        }
        
        /** returns the number of numbers that will be iterated */
        public final int getCount() {
            return count;
        }
        /** returns true if there are more values */
        abstract public boolean hasMore();
        /** returns the current value */
        public final int next() {
            return value;
        }
        
        // fields
        /** the number of ints that will be interated */
        protected final int count;
        /** the ranges */
        protected final RangeInt[] ranges;
        /** the max index value */
        protected final int limit;
        /** the index into the ranges array */
        protected int index = -1;
        /** the currently active one */
        protected RangeInt current;
        /** the maximum value represented by the current RangeInt */
        protected int current_limit = 0;
        /** the current value */
        protected int value = 0;
    } // end RangeIterator
