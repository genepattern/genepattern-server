package org.genepattern.util;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Help class for managing the LSID version String.
 * 
 * @author pcarr
 *
 */
public class LsidVersion implements Comparable<LsidVersion> {
    /**
     * Create a new instance.
     * Split the version string into a sequence of major and minor version elements.
     * Each element must be an Integer >= 0. Special-case for null or empty input, return the
     * default initial version.
     * 
     * @param versionString, e.g. "", "1", "1.2", "3.0.1"
     * @return an immutable LsidVersion instance.
     * @throws IllegalArgumentException
     */
    public static LsidVersion fromString(final String versionString) throws IllegalArgumentException {
        if (Strings.isNullOrEmpty(versionString)) {
            return new LsidVersion();
        }
        final List<Integer> versions=new ArrayList<Integer>();
        for(final String s : Splitter.on(".").splitToList(versionString)) {
            try {
                final Integer i=parseVersionElement(s);
                versions.add(i);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid versionString='"+versionString+"'", e);
            }
        }
        return new LsidVersion(versions);
    }
    
    protected static Integer parseVersionElement(final String versionElement) throws IllegalArgumentException {
        Integer i=null;
        try {
            i=Integer.parseInt(versionElement);
        }
        catch (NumberFormatException e) {
            // ignore, handle in next line
        }
        if (i==null || i<0) {
            throw new IllegalArgumentException("Invalid versionElement='"+versionElement+"', Must be an integer >= 0");
        }
        return i;
    }

    private final ImmutableList<Integer> versions;
    private final String versionString;
    private final int hashCode;
    
    /** 
     * create a new immutable instance, from an array of Integer 
     * E.g.
     *     LsidVersion(1); // create version="1"
     *     LsidVersion();  // empty version="", synonym for "0"
     *     LsidVersion(1,1); // create version "1.1"
     */
    protected LsidVersion(final Integer...versions) {
        this.versions=ImmutableList.copyOf(versions);
        this.versionString=asString(this.versions);
        this.hashCode=versions.hashCode();
    }

    /** 
     * create a new immutable instance, from a List<Integer>.
     * @param versions
     */
    protected LsidVersion(final List<Integer> versions) {
        this.versions=ImmutableList.copyOf(versions);
        this.versionString=asString(this.versions);
        this.hashCode=versions.hashCode();
    }
    
    public LsidVersion nextMajor() {
        return nextSequence(0);
    }
    
    public LsidVersion nextMinor() {
        return nextSequence(1);
    }
    
    public LsidVersion nextPatch() {
        return nextSequence(2);
    }

    /**
     * 
     * @param sequenceIdx, 
     *     versions[0], is MAJOR version
     *     versions[1], is MINOR version
     *     versions[2], is PATCH version
     *     versions[N], is Nth additional version
     * @return
     */
    public LsidVersion nextSequence(final int idx) {
        final List<Integer> next=new ArrayList<Integer>(idx+1);
        // initialize each element with '0'
        for (int i = 0; i<=idx; ++i) {
            next.add(0);
        }
        // copy pre-existing lhs version
        for (int i = 0; i<idx && i<versions.size(); ++i) {
            next.set(i, versions.get(i));
        }
        // increment pre-existing idx version
        if (idx < versions.size()) {
            next.set(idx, 1+versions.get(idx));
        }
        else {
            // set to initial version
            next.set(idx, 1);            
        }
        return new LsidVersion(next);
    }

    protected static String asString(final List<Integer> versions) {
        final Joiner joiner = Joiner.on(LSID.VERSION_DELIMITER); // '.'
        return joiner.join(versions);
    }
    
    public String toString() {
        return versionString;
    }
    
    private int getOrZero(int idx) {
        return getOrDefault(idx, 0);
    }

    private int getOrDefault(int idx, final Integer defaultValue) {
        if (versions==null || versions.size()<=idx) {
            return defaultValue;
        }
        return versions.get(idx);
    }

    @Override
    public int compareTo(final LsidVersion o) {
        return compareToLsidMode(o);
    }

    /**
     * Compare LSID versions to handle expected case when the minor and patch
     * version are not set. E.g.
     *     '' < '0'
     *     '1' < '1.0'
     *     '1.0' < '1.0.0'
     * 
     * @param o
     * @return
     */
    public int compareToLsidMode(final LsidVersion o) {
        if (this == o) return 0;
        
        // special-case, versions.size==0 is always less than versions.size > 0
        if (this.versions.size()==0) {
            if (o.versions.size()==0) {
                return 0;
            }
            return -1;
        }
        else if (o.versions.size()==0) {
            return 1;
        }
        
        for(int i=0; i<versions.size(); ++i) {
            int v=versions.get(i);
            int ov =  i<o.versions.size() ? o.versions.get(i) : 0;
            if (v < ov) {
                return -1;
            }
            else if (v > ov) {
                return 1;
            }
        }
        
        // if we are here, it means each element in versions matches each corresponding element in o.versions
        if (versions.size()==o.versions.size()) {
            return 0;
        }
        
        // if we are here, it means o.versions.size > versions.size
        if (o.versions.size() > versions.size()) {
            return -1;
        }
        else if (o.versions.size() < versions.size()) {
            return 1;
        }
        return 0;
    }

    /**
     * Compare LSID versions, missing minor and patch versions are equivalent to '0'. E.g.
     *     '' == '0'
     *     '1' == '1.0'
     *     '1.0' == '1.0.0'
     *  
     * @param o
     * @return
     */
    public int compareToSemVer(final LsidVersion o) {
        if (this == o) return 0;
        
        int MAX=Math.max(versions.size(), o.versions.size());
        for(int i=0; i<MAX; ++i) {
            int v=getOrZero(i);
            int ov=o.getOrZero(i);
            if (v < ov) {
                return -1;
            }
            else if (v > ov) {
                return 1;
            }
        }
        return 0;
    }
    
    public boolean equals(final Object obj) {
        if (!(obj instanceof LsidVersion)) {
            return false;
        }
        return java.util.Objects.deepEquals(versions, ((LsidVersion)obj).versions);
    }
    
    public int hashCode() {
        return hashCode;
    }

}


