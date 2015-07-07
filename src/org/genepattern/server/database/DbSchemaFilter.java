package org.genepattern.server.database;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;

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
    final String schemaPrefix;
    
    /**
     * By default use 'analysis_hypersonic-' schemaPrefix.
     */
    public DbSchemaFilter() {
        this("analysis_hypersonic-");
    }
    
    /**
     * Set non-default schemaPrefix, e.g. 'analysis_oracle-'.
     * @param schemaPrefix
     */
    public DbSchemaFilter(final String schemaPrefix) {
        this.schemaPrefix=schemaPrefix;
    }

    public boolean accept(File dir, String name) {
        return name.endsWith(".sql") && name.startsWith(schemaPrefix);
    }

    public int compare(File f1, File f2) {
        String name1 = f1.getName();
        String version1 = name1.substring(schemaPrefix.length(), name1.length() - ".sql".length());
        String name2 = f2.getName();
        String version2 = name2.substring(schemaPrefix.length(), name2.length() - ".sql".length());
        return version1.compareToIgnoreCase(version2);
    }
}