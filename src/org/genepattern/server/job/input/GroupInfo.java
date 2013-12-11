package org.genepattern.server.job.input;

import org.genepattern.webservice.ParameterInfo;

/**
 * Data structure for indicating how a particular module input parameter can be organized into groups.
 * For example, you may want to declare that a file input parameter can be given a group of input files,
 * one group for 'tumor' and one group for 'normal'.
 * 
 * To do this add the following lines to the manifest file.
 * 
 * Proposed manifest file representation:
 * <pre>
   p1_numValues=0+
   p1_numGroups=1+
 * </pre>
 * 
 * Proposed JSON representation:
 * <pre>
   groupInfo: {
       numGroups: 0+,
       columnLabel: "",
       fileLabel: "",
       numValuesMustMatch: false
   }
 * </pre>
 * 
 * @author pcarr
 *
 */
public class GroupInfo {
    /** manifest file property name */
    public static final String PROP_NUM_GROUPS="numGroups";
    public static final String PROP_GROUP_COLUMN_LABEL="groupsColumnLabel";
    public static final String PROP_GROUP_FILE_LABEL="groupsFileLabel";
    
    private final Integer minNumGroups;
    private final Integer maxNumGroups;
    private final String groupColumnLabel;
    private final String fileColumnLabel;
    
    /**
     * When this flag is set to true, the UI and the server should check that each group contains the exact same number of values.
     * For a given job submission, when there are multiple groups, each group must contain the same number of
     * input files.
     */
    private final boolean numValuesMustMatch;

    
    private GroupInfo(final Builder in) {
        this.minNumGroups=in.minNumGroups;
        this.maxNumGroups=in.maxNumGroups;
        this.groupColumnLabel=in.groupColumnLabel;
        this.fileColumnLabel=in.fileColumnLabel;
        this.numValuesMustMatch=in.numValuesMustMatch;
    }

    public Integer getMinNumGroups() {
        return minNumGroups;
    }
    public Integer getMaxNumGroups() {
        return maxNumGroups;
    }
    
    /**
     * Get the optional header label to use in the job input form UI for the groups column.
     * For example to label the groups column as 'sample type'.
     * 
     * @return
     */
    public String getGroupColumnLabel() {
        return groupColumnLabel;
    }
    
    /**
     * Get the optional header label to use in the job input form UI for the files column.
     * For example to label the files column as 'replicates'.
     * 
     * @return
     */
    public String getFileColumnLabel() {
        return fileColumnLabel;
    }
    
    public static class Builder {
        private Integer minNumGroups=0;
        private Integer maxNumGroups=0;
        private String groupColumnLabel="group";
        private String fileColumnLabel="file";
        private boolean numValuesMustMatch=false;
        
        public GroupInfo build() {
            return new GroupInfo(this);
        }
        
        public Builder min(final Integer min) {
            this.minNumGroups=min;
            return this;
        }
        
        public Builder max(final Integer max) {
            this.maxNumGroups=max;
            return this;
        }
        
        public Builder groupColumnLabel(final String label) {
            this.groupColumnLabel=label;
            return this;
        }
        
        public Builder fileColumnLabel(final String label) {
            this.fileColumnLabel=label;
            return this;
        }
        
        public Builder numValuesMustMatch(final boolean b) {
            this.numValuesMustMatch=b;
            return this;
        }
        
        public Builder fromParameterInfo(final ParameterInfo pinfo) {
            //TODO: implement manifest format parser
            throw new IllegalArgumentException("Method not implemented!");
            //return this;
        }
    }

}
