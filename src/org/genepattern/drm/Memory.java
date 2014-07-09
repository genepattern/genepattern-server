package org.genepattern.drm;

import org.apache.log4j.Logger;

/**
 * Generic representation of an amount of memory, so that it is easier to initialize memory settings from 
 * the config file. Examples:
 * <pre>
   job.memory: 1024b   
   job.memory: 1 kb
   job.memory: 100mb
   job.memory: 8gb
 * </pre>
 * @author pcarr
 *
 */
public class Memory {
    private static final Logger log = Logger.getLogger(Memory.class);

    /**
     * A unit of memory in bytes.
     * 
     * 
     * @author pcarr
     *
     */
    public static enum Unit {
        b (1L),
        k (1024L),
        kb(1024L),
        m (1048576L),
        mb(1048576L),
        g (1073741824L),
        gb(1073741824L),
        t (1099511627776L),
        tb(1099511627776L),
        p (1125899906842624L),
        pb(1125899906842624L);

        static Unit scaleDown(Unit in) {
            if (in==b) {
                throw new IllegalArgumentException("Can't scale down from "+in);
            }
            if (in==k) return b;
            // by convention, can subtract the index, e.g. scaleDown(g) returns m and scaleDown(gb) returns mb
            return Unit.values()[in.ordinal()-2];
        }
        
        static Unit scaleUp(Unit in) {
            if (in==p || in==pb) {
                return in;
            }
            if (in==b) return k;
            return Unit.values()[in.ordinal()+2];
        }

        private final long multiplier;
        private Unit(long multiplier) {
            this.multiplier=multiplier;
        }
        long getMultiplier() {
            return multiplier;
        }
    }
    
    public static Memory fromSizeInBytes(final long sizeInBytes) {
        return new Memory(sizeInBytes, Unit.b);
    }
    
    /**
     * Initialize memory instance from string, for example
     * <pre>
       maxMemory: "8 Gb"
       maxMemory: "8"
       maxMemory: "8gb"
     * </pre>
     * 
     * Must be a String which can be split by the space (' ') character into a
     * number (double) value and a memory.unit (string).
     * Memory units are not case sensitive.
     * If no unit is specified, then by default, 'Gb' is used.
     * Bytes must be specified as in integer, for other units, fractional (double) values
     * are allowed. Rounding occurs in the getNumBytes method.
     * 
     * @param str
     * @return
     * @throws NumberFormatException, IllegalArgumentException
     */
    public static Memory fromString(final String in) throws NumberFormatException, IllegalArgumentException {
        if (in==null) {
            log.debug("in==null");
            return null;
        }
        final String str=in.trim().toLowerCase();
        if (str.length()==0) {
            log.debug("str is empty, return null");
            return null;
        }

        //split the input string a magnitude (long value) and [optional] units
        //expecting: "<double> <units>" | "<double><units>" | "<double>"
        Unit unit=null;
        int matchingIdx=-1;
        for(final Unit unitFromEnum : Unit.values()) {
            if (str.endsWith(unitFromEnum.name())) {
                unit=unitFromEnum;
                matchingIdx=str.lastIndexOf(unitFromEnum.name());
            }
        }
        
        final String valueSpec;
        if (unit != null) {
            valueSpec=str.substring(0, matchingIdx).trim();
        }
        else {
            valueSpec=str;
        }
        if (unit == null) {
            unit=Unit.gb;
        }
        double value=Double.valueOf(valueSpec);

        return new Memory(value, unit);
    }

    private final double value;
    private final Unit unit;
    private final Long numBytes;

    // copy constructor
    private Memory(final Memory in) {
        this.value=in.value;
        this.unit=in.unit;
        this.numBytes=initNumBytes(value, unit);
    }
    private Memory(final double value, final Unit unit) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0");
        }
        this.value=value;
        this.unit=unit;
        this.numBytes=initNumBytes(value, unit);
    }

    private static final long initNumBytes(final double value, final Unit unit) {
        return Math.round(value * unit.getMultiplier());
    }
    public long getNumBytes() {
        return numBytes;
    }

    public double numGb() {
        double numGb=(double) getNumBytes() / (double) Unit.gb.getMultiplier();
        return numGb;
    }
    
    public String toString() {
        return ""+value+" "+unit.name();
    }

    /**
     * Output a valid Java '-Xmx' value, must be an integer value in k, m or g units.
     * This returns a String containing an integer and a single character scale, it is up to the calling method to append the '-Xmx' flag.
     * For example, '512m'.
     * @return
     */
    public String toXmx() { 
        if (unit==Unit.b) {
            //convert to k
            long numKb = (long)Math.max(1, Math.round( ((double)getNumBytes()) / Unit.kb.getMultiplier() ));
            return numKb+"k";
        }
        else if (unit.ordinal() >= Unit.g.ordinal()) {
            return toXmxUnits(Unit.gb);
        }
        else if (unit.ordinal() >= Unit.m.ordinal()) {
            return toXmxUnits(Unit.mb);
        }
        else if (unit==Unit.k || unit==Unit.kb) {
            //convert to k
            long numKb = (long)Math.max(1, Math.round( ((double)getNumBytes()) / Unit.kb.getMultiplier() ));
            return numKb+"k";
        }
        //dead code
        return toXmxUnits(Unit.gb);
    }

    /**
     * Helper method for the toXmx method, for the given xmxUnits, if we can output an integer value do so,
     * otherwise, scale down, for example from Gb to Mb, so that we don't lose too much precision when rounding
     * to an integer value.
     *     toXmxUnits of "2.5 Gb" becomes "2560m"
     *     toXmxUnits of "2 Gb" becomes "2g"
     * @param xmxUnits
     * @return
     */
    private String toXmxUnits(final Unit xmxUnits) {
        long num=getNumBytes() / xmxUnits.getMultiplier();
        long mod=getNumBytes() % xmxUnits.getMultiplier();
        if (mod==0) {
            return ""+num+xmxUnits.toString().charAt(0);
        }
        //else scale down and round to nearest int
        final Unit down=Unit.scaleDown(xmxUnits);
        long downNum=1024L*num + (long) Math.round(((double)mod) / down.getMultiplier());
        return ""+downNum+down.toString().charAt(0);
    }
    
    public boolean equals(final Object obj) {
        if (obj==null) {
            return false;
        }
        if (this==obj) {
            return true;
        }
        if (!(obj instanceof Memory)) {
            return false;
        }
        return ((Memory)obj).getNumBytes()==getNumBytes();
    }
    
    public int hashCode() {
        return numBytes.hashCode();
    }

}
