package org.genepattern.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Helper class for managing the LSID version String.
 * 
 * @author pcarr
 *
 */
public class LsidVersion implements Comparable<LsidVersion> {
    private static final Logger log = Logger.getLogger(LsidVersion.class);

    public static enum Increment {
        next("1") { // means fall back to default
            @Override
            public String nextVersion(LSID lsid) {
                return fromLsid(lsid).increment().toString();
            }
        }, 
        major("1") {  // next major
            @Override
            public String nextVersion(LSID lsid) {
                return fromLsid(lsid).nextMajor().toString();
            } 
        },
        minor("0.1") { // next minor
            @Override
            public String nextVersion(LSID lsid) {
                return fromLsid(lsid).nextMinor().toString();
            } 
        },
        patch("0.0.1") { // next patch
            @Override
            public String nextVersion(LSID lsid) {
                return fromLsid(lsid).nextPatch().toString();
            } 
        }
        ;
        
        protected static LsidVersion fromLsid(final LSID lsid) {
            return LsidVersion.fromString(lsid.getVersion());
        }
        
        public static Increment fromString(final String in) {
            // special-case: null or empty means use default
            if (Strings.isNullOrEmpty(in)) {
                return next;
            }
            try {
                return Increment.valueOf(in);
            }
            catch (Throwable t) {
                log.debug("Error initializing from '"+in+"': Use default value", t);
                return next;
            }
        }

        final String initialVersion;

        private Increment(final String initialVersion) {
            this.initialVersion=initialVersion;
        }

        /**
         * Get the initial version.
         */
        public String initialVersion() {
            return initialVersion;
        }
        
        /**
         * Get the next version.
         */
        abstract public String nextVersion(final LSID lsid);
    }

    /**
     * Create a new instance from a String.
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
    
    /**
     * Helper method for parsing the major or minor version in the sequence.
     * @param versionElement, expecting an integer >= zero.
     * @return
     * @throws IllegalArgumentException
     */
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
     *     LsidVersion();    // empty version="", synonym for "0"
     *     LsidVersion(1);   // create version="1"
     *     LsidVersion(1,1); // create version "1.1"
     *     
     * @throws IllegalArgumentException if any integer values are less than zero.
     */
    protected LsidVersion(final Integer...versions) throws IllegalArgumentException {
        // validate versions
        for(final Integer i : versions) {
            if (i<0) {
                throw new IllegalArgumentException("Invalid versionElement='"+i+"', Must be an integer >= 0");
            }
        }
        this.versions=ImmutableList.copyOf(versions);
        this.versionString=asString(this.versions);
        this.hashCode=versions.hashCode();
    }

    /** 
     * create a new immutable instance, from a List<Integer>.
     * @param versions
     */
    protected LsidVersion(final List<Integer> versions) {
        this(versions.toArray(new Integer[versions.size()]));
    }
    
    /** 
     * Increment the current version level, for example to create a new major release,
     *     1 -> 2
     * Or to create a new minor release,
     *     3.1 -> 3.2
     * Other examples,
     *     3.1.15 -> 3.1.16
     */
    public LsidVersion increment() {
        final int K=versions.size();
        // special-case: no versions
        if (K==0) {
            return incrementVersion(0);
        }
        return incrementVersion(K-1);
    }

    /**
     * Shift the current version level one to the left, for example 
     * to create a new major release from a minor release,
     *     0.48 -> 1
     *     1.3  -> 2
     * Other examples,
     *     3.1.15 -> 3.2
     * @return a new immutable LsidVersion instance
     */
    public LsidVersion incrementLeft() {
        final int K=versions.size();
        return incrementVersion(
                // handle special cases: zero or 1 version
                Math.max(0, K-2)
        );
    }

    /**
     * Shift the current version level on to the right, for example
     * to create a new minor release from a major release,
     *     1 -> 1.1
     *     2 -> 2.1
     * Other examples,
     *     3.1 -> 3.1.1
     *     3.1.15 -> 3.1.15.1
     * @return a new immutable LsidVersion instance
     */
    public LsidVersion incrementRight() {
        final int K=versions.size();
        if (K==0) {
            return new LsidVersion(0,1);
        }
        return incrementVersion(K);
    }

    /** synonym for {@link #incrementVersion(0)} */
    public LsidVersion nextMajor() {
        return incrementVersion(0);
    }
    
    /** synonym for {@link #incrementVersion(1)} */
    public LsidVersion nextMinor() {
        return incrementVersion(1);
    }
    
    /** synonym for {@link #incrementVersion(2)} */
    public LsidVersion nextPatch() {
        return incrementVersion(2);
    }

    /**
     * Create a new LsidVersion by incrementing the version at the request idx level.
     *     idx=0, {MAJOR}
     *     idx=1, {MINOR}
     *     idx=2, {PATCH}
     *     ...
     *     idx=N, {additional minor versions}
     * 
     * For example, given an existing version "3.1.15" ({major}.{minor}.{patch}),
     * versions=[3, 1, 15], idx is the index into the versions array.
     *     increment(0) -> 4
     *     increment(1) -> 3.2
     *     increment(2) -> 3.1.16
     * Fill in additional minor levels as needed, with an initial value of 0 and 
     * the right-most value incremented to 1.
     *     increment(5) -> 3.1.15.0.0.1
     * 
     * @param idx - the index in the versions[] array
     * @return a new immutable LsidVersion instance
     * @throws IllegalArgumentException - idx must be >=0 and <= 100 (arbitrary MAX number of version elements)
     */
    protected LsidVersion incrementVersion(final int idx) throws IllegalArgumentException {
        if (idx<0) {
            throw new IllegalArgumentException("idx='"+idx+"': Must be >= 0");
        }
        if (idx > 100) {
            throw new IllegalArgumentException("idx='"+idx+"': Must be <= 100, Usually in the range 0..2");
        }
        final int K = versions.size();
        
        final ImmutableList.Builder<Integer> b = new ImmutableList.Builder<Integer>();
        b.addAll( versions.subList(0, Math.min(idx,K)) ); // copy up to idx, exclusive
        if (idx<K) {
            b.add(1+versions.get(idx));
        }
        else {
            for(int i=K; i<idx; ++i) {
                b.add(0);
            }
            b.add(1);
        }
        return new LsidVersion(b.build());
    }

    protected static String asString(final List<Integer> versions) {
        final Joiner joiner = Joiner.on(LSID.VERSION_DELIMITER); // '.'
        return joiner.join(versions);
    }
    
    public String toString() {
        return versionString;
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
        else if (versions.size() < o.versions.size()) {
            return -1;
        }
        else {
            return 1;
        }
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
    
    private int getOrZero(int idx) {
        return getOrDefault(idx, 0);
    }

    private int getOrDefault(int idx, final Integer defaultValue) {
        if (versions==null || versions.size()<=idx) {
            return defaultValue;
        }
        return versions.get(idx);
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


