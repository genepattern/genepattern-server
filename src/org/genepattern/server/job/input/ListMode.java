package org.genepattern.server.job.input;

public enum ListMode { 
    /**
     * When listMode=legacy and num input files is ...
     *     0, no cmd line arg
     *     1, the data file is the cmd line arg
     *     >1, the filelist is the cmd line arg
     *     
     * This is for compatibility with older versions of GP, send data files by value, when only one input value is submitted.
     */
    LEGACY, 
    /**
     * When listMode=list and num input files is ...
     *     0, no cmd line arg
     *     >0, the filelist is the cmd line arg
     *     
     * For newer (3.6+) versions of GP, always send filelist files, except for when the list is empty.
     */
    LIST,
    /**
     * When listMode=listIncludeEmpty, always create a filelist file on the cmd line, even for empty lists.
     */
    LIST_INCLUDE_EMPTY,
    /**
     * When listMode=CMD, the individual values will be listed on the CMD line as a single arg with a default comma separator.
     * Set a custom separator with the 'listModeSep' attribute. The prefix_when_specified flag is optionally
     * added as a command line arg.
     * 
     * case 1: no prefix_when_specified results in one arg
     *     "argA,argB"
     * case 2: prefix with no trailing space "-i", results in one arg
     *     "-iargA,argB"
     * case 3: prefix with trailing space "-i ", results in two args
     *     "-i", "argA,argB"
     */
    CMD,
    /**
     * When listMode=CMD_OPT, the individual values will be listed on the CMD line one arg per value.
     * The prefix_when_speficied optionally is appended to the value.
     * 
     * case 1: no prefix_when_specified results in N args
     *     "argA", "argB"
     * case 2: prefix with no trailing space results in N args
     *     "-iargA", "-iargB"
     * case 3: prefix with trailing space results in 2*N args
     *     "-i", "argA", "-i", "argB"
     */
    CMD_OPT
}