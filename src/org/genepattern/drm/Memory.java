package org.genepattern.drm;

import org.apache.log4j.Logger;

/**
 * Generic representation of an amount of memory.
 * @author pcarr
 *
 */
public class Memory {
    private static final Logger log = Logger.getLogger(Memory.class);

    /**
     * A unit of memory in bytes.
     * 
     * @see http://www.clusterresources.com/torquedocs21/2.1jobsubmission.shtml#size
     * 
     * @author pcarr
     *
     */
    public static enum Unit {
         b(1L),
        kb(1024L),
        mb(1048576L),
        gb(1073741824L),
        tb(1099511627776L);

        private final long multiplier;
        private Unit(long multiplier) {
            this.multiplier=multiplier;
        }
        long getMultiplier() {
            return multiplier;
        }
    }
    
    /**
     * Initialize memory instance from string, for example
     * <pre>
       maxMemory: "8 Gb"
     * </pre>
     * 
     * Must be a String which can be split by the space (' ') character into an
     * number (double) value and a memory.unit (string).
     * Memory units are not case sensitive.
     * If no unit is specified, then by default, 'Gb' is used.
     *
     * 
     * @param str
     * @return
     * @throws NumberFormatException, IllegalArgumentException
     */
    public static Memory fromString(final String str) throws NumberFormatException, IllegalArgumentException {
        if (str==null) {
            log.debug("str==null");
            return null;
        }
        if (str.trim().length()==0) {
            log.debug("str is empty, return null");
            return null;
        }
        //expecting: <double> <units>
        final String[] args=str.trim().split(" ");
        
        if (args.length<1 || args.length>2) {
            throw new IllegalArgumentException("Invalid format for str='"+str+"', Must split into 1, or 2 args" );
        }

        //throws NumberFormatException
        final double value=Double.valueOf(args[0]);
       
        final Unit unit;
        if (args.length==1) {
            unit=Unit.gb;
        }
        else {
            //throws IllegalArgumentException
            unit=Unit.valueOf(args[1].toLowerCase());
        }
        return new Memory(value, unit);
    }

    private double value;
    private Unit unit;
    
    private Memory(final double value, final Unit unit) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0");
        }
        this.value=value;
        this.unit=unit;
    }

    public long getNumBytes() {
        //return value * unit.getMultiplier();
        return Math.round(value * unit.getMultiplier());
    }
    
    public double numGb() {
        double numGb=(double) getNumBytes() / (double) Unit.gb.getMultiplier();
        return numGb;
    }
    
    public String toGb() {
        return ""+ ( ((long)getNumBytes()) / Unit.gb.getMultiplier());
    }

    public String toString() {
        return ""+value+" "+unit.name();
    }
}
