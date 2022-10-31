/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import java.text.NumberFormat;
import java.util.EnumSet;

import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlRootElement;

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
@XmlRootElement
public class Memory {
    private static final Logger log = Logger.getLogger(Memory.class);
    private static NumberFormat numberFormat;
    static {
        numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
    }

    // used only for WADL generation, no-arg constructor to eliminate error messages
    private Memory(){
        numBytes = 0L;
        unit = Unit.values()[0];
        value = 0;
        displayValue = "0MB";        
    }
    
    
    /**
     * A unit of memory in bytes, based on a 1024 scale factor.
     *     1kb == 1024b, 1mb == 1024kb and so on.
     * This is the standard when measuring processor or virtual memory.
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
        
        /** to make it easier to iterate through each scale value. */
        static final EnumSet<Unit> elements=EnumSet.of(Unit.b, Unit.kb, Unit.mb, Unit.gb, Unit.tb, Unit.pb);

        /**
         * Convenience method for getting the unit one below the given unit.
         * @param in
         * @return
         */
        static final Unit scaleDown(Unit in) {
            if (in==b) {
                throw new IllegalArgumentException("Can't scale down from "+in);
            }
            if (in==k) return b;
            // by convention, can subtract the index, e.g. scaleDown(g) returns m and scaleDown(gb) returns mb
            return Unit.values()[in.ordinal()-2];
        }
        
        /**
         * Convenience method for getting the unit one above the given unit.
         * @param in
         * @return
         */
        static final Unit scaleUp(Unit in) {
            if (in==p || in==pb) {
                return in;
            }
            if (in==b) return k;
            return Unit.values()[in.ordinal()+2];
        }
        
        /**
         * Convenience method for getting the preferred unit for the given number of bytes.
         * This returns the Unit such that converting the given numBytes into the returned Unit
         * will be a value < 1024, except when it's scaled to Unit.pb.
         * 
         * @param numBytes
         * @return
         */
        static final Unit getPreferredUnit(long numBytes) {
            Unit prev=Unit.b;
            for(Unit unit : Unit.elements) {
                if (numBytes < unit.getMultiplier()) {
                    return prev;
                }
                prev=unit;
            }
            if (numBytes >= Unit.pb.getMultiplier()) {
                return Unit.pb;
            }
            return prev;
        }

        private final long multiplier;
        private Unit(long multiplier) {
            this.multiplier=multiplier;
        }
        
        /**
         * Get the number of bytes represented by this unit.
         */
        public long getMultiplier() {
            return multiplier;
        }
    }

    /** exactly one Mb */
    public static final Memory MB=new Memory(1L, Unit.mb, Unit.mb.getMultiplier(), "1 Mb");
    /** exactly one Gb */
    public static final Memory GB=new Memory(1L, Unit.gb, Unit.gb.getMultiplier(), "1 Gb");
    
    /** 
     * Create a new Memory instance based on the number of bytes.
     * 
     * @return a new Memory instance
     */
    public static final Memory fromSizeInBytes(long numBytes) {
        String displayValue=Memory.formatNumBytes(numBytes);
        return new Memory(numBytes, displayValue);
    }

    /**
     * Create a new memory instance from the given string, for example
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
     * Bytes must be specified as an integer, for other units, fractional (double) values
     * are allowed. Rounding occurs in the getNumBytes method.
     * 
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

        // when initialized from string, set the displayValue to the input string
        return new Memory(value, unit, in);
    }

    /**
     * Human readable representation by scaling the raw number of bytes up to a reasonable approximation.
     * For example, long numBytes=2969658452L will be formatted as '2.8 GB'.
     * For small numbers, rounds up to the nearest 1.0 KB.
     * 
     */
    public static final String formatNumBytes(long size) {
        if (size >= Unit.pb.getMultiplier()) {
            double petabytes = size / (double) Unit.pb.getMultiplier();
            return numberFormat.format(petabytes) + " PB";
        }
        else if (size >= Unit.tb.getMultiplier()) {
            double terabytes = size / (double) Unit.tb.getMultiplier();
            return numberFormat.format(terabytes) + " TB";
        }
        else if (size >= Unit.gb.getMultiplier()) {
            double gigabytes = size / (double) Unit.gb.getMultiplier();
            return numberFormat.format(gigabytes) + " GB";
        }
        else if (size >= Unit.mb.getMultiplier()) {
            double megabytes = size / (double) Unit.mb.getMultiplier();
            return numberFormat.format(megabytes) + " MB";
        }
        else {
            return Math.max(0, Math.ceil(size / 1024.0)) + " KB";
        }
    }

    /**
     * Get the max value of the two args.
     * @param arg0
     * @param arg1
     */
    public static final Memory max(final Memory arg0, final Memory arg1) {
        if (arg0==null) { return arg1; }
        if (arg1==null) { return arg0; }
        if (arg0.getNumBytes()>=arg1.getNumBytes()) {
            return arg0;
        }
        return arg1;
    }

    private final double value;
    private final Unit unit;
    private final Long numBytes;
    private final String displayValue;

    // copy constructor
    public Memory(final Memory in) {
        this.value=in.value;
        this.unit=in.unit;
        this.numBytes=in.numBytes;
        this.displayValue=in.displayValue;
    }

    /**
     * @deprecated Use Memory.fromSizeInBytes instead.
     */
    public Memory(Long numBytes) {
        this.value = numBytes;
        this.unit = Unit.b;
        this.numBytes = numBytes;
        this.displayValue = Memory.formatNumBytes(numBytes);
    }
    
    /**
     * Create a new Memory instance.
     * @param numBytes the amount of memory in bytes
     * @param displayValue a formatted display value for the UI
     */
    protected Memory(final long numBytes, final String displayValue) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes must be >= 0");
        }
        this.value=numBytes;
        this.unit=Unit.b;
        this.numBytes=numBytes;
        this.displayValue=displayValue;
    }

    /**
     * Create a new Memory instance.
     * @param value the amount of memory
     * @param unit in the given units
     * @param displayValue a formatted display value for the UI
     */
    public Memory(final double value, final Unit unit, final String displayValue) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be >= 0");
        }
        this.value=value;
        this.unit=unit;
        this.numBytes=Math.round(value * unit.getMultiplier());
        this.displayValue=displayValue;
    }

    protected Memory(final double value, final Unit unit, final long numBytes, final String displayValue) {
        this.value = numBytes;
        this.unit = Unit.b;
        this.numBytes = numBytes;
        this.displayValue = displayValue;
    }

    public long getNumBytes() {
        return numBytes;
    }
    
    public String getDisplayValue() {
        return displayValue;
    }

    public double numGb() {
        double numGb=(double) getNumBytes() / (double) Unit.gb.getMultiplier();
        return numGb;
    }
    
    /**
     * Get this memory value in the given units.
     * @param unit
     */
    public double numUnits(Unit unit) {
        double numUnits=(double) getNumBytes() / (double) unit.getMultiplier();
        return numUnits;
    }

    /**
     * Human readable representation by scaling the raw number of bytes up to a reasonable approximation.
     * This calls #formatNumBytes.
     * For example, long numBytes=2969658452L will be formatted as '2832 mb'.
     */
    public String format() {
        return formatNumBytes(numBytes);
    }
    
    public String toString() {
        return toString(this.value, this.unit);
    }
    
    public static String toString(double value, Unit unit) {
        if (isIntValue(value)) {
            return ""+(int)value+" "+unit.name();
        }
        return ""+value+" "+unit.name();
    }

    /**
     * Helper method, for the toString method, to avoid '512.0 mb' when '512 mb' is correct.
     * @return
     */
    private static boolean isIntValue(double value) {
        if ((value == Math.floor(value)) && !Double.isInfinite(value)) {
            return true;
        }
        return false;
    }

    /**
     * Output a valid Java '-Xmx' value, must be an integer value in k, m or g units.
     * This returns a String containing an integer and a single character scale, it is up to the calling method to append the '-Xmx' flag.
     * For example, '512m'.
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
