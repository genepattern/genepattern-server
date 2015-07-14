package org.genepattern.server.database;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Range;

/**
 * Helper class for filtering and sorting the list of database schema files
 * (aka DDL scripts) in the WEB-INF/schema directory.
 * 
 * Match all <schemaPrefix><schemaVersion>.sql files.
 * Sort alphabetically (case-insensitive) by <schemaVersion>.
 * 
 * @author pcarr
 *
 */
public final class DbSchemaFilter implements FilenameFilter, Comparator<File> {
    private static final Logger log = Logger.getLogger(DbSchemaFilter.class);

    final String schemaPrefix;
    // (a..b] = {schemaVersion | a < schemaVersion <= b}
    final Range<String> schemaRange;
        
    private DbSchemaFilter(final Builder in) {
        this.schemaPrefix=in.schemaPrefix;
        this.schemaRange=initRange(in.dbSchemaVersion, in.maxSchemaVersion);
    }
    
    /**
     * A schemaVersion is in range when dbSchemaVersion < schemaVersion <= maxSchemaVersion, with special-cases for null args.
     * (a..b] = {x | a < x <= b}
     *     '(' means 'open'
     *     '[' means 'closed'
     * 
     * @param dbSchemaVersion
     * @param maxSchemaVersion
     * @return
     */
    protected static Range<String> initRange(final String dbSchemaVersion, final String maxSchemaVersion) {
        if (dbSchemaVersion==null && maxSchemaVersion==null) {
            return Range.all();
        }
        else if (dbSchemaVersion==null) {
            // (-INF..b]
            return Range.atMost(maxSchemaVersion.toLowerCase());
        }
        else if (maxSchemaVersion==null) {
            // (a..+INF)
            return Range.greaterThan(dbSchemaVersion.toLowerCase());
        }
        else {
            int c=dbSchemaVersion.toLowerCase().compareTo(maxSchemaVersion.toLowerCase());
            if (c<0) {
                return Range.openClosed(dbSchemaVersion.toLowerCase(), maxSchemaVersion.toLowerCase());
            }
            else {
                // min >= max, construct an empty range (v..v]
                return Range.openClosed(maxSchemaVersion.toLowerCase(), maxSchemaVersion.toLowerCase());
            }
        }
    } 
    
    protected String initSchemaVersion(final File schemaFile) {
        return initSchemaVersionFromFilename(schemaPrefix, schemaFile);
    }
    
    /**
     * Given a schemaPrefix and a schemaFile
     * @param schemaPrefix, e.g. 'analysis_hypersonic-'
     * @param schemaFile, e.g. 'analysis_hpersonic-3.9.3.sql'
     * @return the schemaVersion to use when comparing and recording the the DB
     */
    public static String initSchemaVersionFromFilename(final String schemaPrefix, final File schemaFile) {
        final String name=schemaFile.getName();
        final String version = name.substring(schemaPrefix.length(), name.length() - ".sql".length());
        return version;
    }
    
    /**
     * Get the list if DDL scripts to run, based on the contents of the given schemaDir.
     * 
     * @param schemaDir, the directory which contains the DDL scripts (e.g. '<webappDir>/WEB-INF/schema')
     * @return
     */
    public List<File> listSchemaFiles(final File schemaDir) {   
        final List<File> rval=new ArrayList<File>();
        final File[] schemaFiles = schemaDir.listFiles(this);
        Arrays.sort(schemaFiles, this);
        for (int f = 0; f < schemaFiles.length; f++) {
            final File schemaFile = schemaFiles[f];
            final String name=schemaFile.getName();
            final String schemaVersion=initSchemaVersion(schemaFile);
            if (acceptSchemaVersion(schemaVersion)) {
                log.debug("adding " + name + " (" + schemaVersion + ")");
                rval.add(schemaFile);
            }
            else {
                log.debug("skipping " + name + " (" + schemaVersion + ")");
            }
        }
        log.debug("listing schema files ... Done!");
        return rval;
    }

    public boolean accept(final File dir, final String name) {
        return name.endsWith(".sql") && name.startsWith(schemaPrefix);
    }

    public int compare(final File f1, final File f2) {
        final String schemaVersion1=initSchemaVersion(f1);
        final String schemaVersion2=initSchemaVersion(f2);
        return schemaVersion1.compareToIgnoreCase(schemaVersion2);
    }
    
    /**
     * Check if the schemaVersion is in the range of schema DDL versions to run.
     * @param schemaVersion
     * @return
     */
    protected boolean acceptSchemaVersion(final String schemaVersion) { 
        return schemaRange.contains(schemaVersion.toLowerCase());
    }
    
    public static final class Builder {
        private String schemaPrefix="analysis_hypersonic-";
        private String dbSchemaVersion=null;
        private String maxSchemaVersion=null;

        /**
         * Set the schemaPrefix, default value is 'analysis_hypersonic-'.
         * @param schemaPrefix
         * @return
         */
        public Builder schemaPrefix(final String schemaPrefix) {
            this.schemaPrefix=schemaPrefix;
            return this;
        }
        
        public Builder dbSchemaVersion(final String dbSchemaVersion) {
            this.dbSchemaVersion=dbSchemaVersion;
            return this;
        }
        
        public Builder maxSchemaVersion(final String maxSchemaVersion) {
            this.maxSchemaVersion=maxSchemaVersion;
            return this;
        }
        
        public DbSchemaFilter build() {
            return new DbSchemaFilter(this);
        }
    }

}
