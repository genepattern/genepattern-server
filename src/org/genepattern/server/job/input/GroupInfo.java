/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import org.apache.log4j.Logger;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONException;
import org.json.JSONObject;

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
   p1_groupColumnLabel=sample
   p1_groupFileLabel=replicate
   p1_groupNumValuesMustMatch=false
 * </pre>
 * 
 * Proposed JSON representation:
 * <pre>
   groupInfo: {
       minNumGroups: 0,
       maxNumGroups: 2,
       groupColumnLabel: "",
       fileColumnLabel: "",
       numValuesMustMatch: false
   }
 * </pre>
 * If there is no 'groupInfo' key then it means the grouping feature should not be enabled in the UI.
 * If maxNumGroups is not set, it means there can be an unlimited number of groups.
 * 
 * @author pcarr
 *
 */
public class GroupInfo {
    final static private Logger log = Logger.getLogger(GroupInfo.class);

    /** manifest file property name */
    public static final String PROP_NUM_GROUPS="numGroups";
    public static final String PROP_GROUP_COLUMN_LABEL="groupColumnLabel";
    public static final String PROP_FILE_COLUMN_LABEL="fileColumnLabel";
    public static final String PROP_NUM_VALUES_MUST_MATCH="groupNumValuesMustMatch";
    
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
    
    public boolean getNumValuesMustMatch() {
        return this.numValuesMustMatch;
    }
    
    /**
     * Get the json representation of the given groupInfo instance.
     * @param groupInfo
     * @return null if the groupInfo is null.
     */
    public static JSONObject toJson(final GroupInfo groupInfo) throws JSONException {
        if (groupInfo==null) {
            return null;
        }
        JSONObject json=new JSONObject();
        if (groupInfo.getMinNumGroups() != null) {
            json.put("minNumGroups", groupInfo.getMinNumGroups());
        }
        if (groupInfo.getMaxNumGroups() != null) {
            json.put("maxNumGroups", groupInfo.getMaxNumGroups());
        }
        json.put("groupColumnLabel", groupInfo.getGroupColumnLabel());
        json.put("fileColumnLabel", groupInfo.getFileColumnLabel());
        json.put("numValuesMustMatch", groupInfo.getNumValuesMustMatch());
        return json;
    }
    
    public static class Builder {
        private Integer minNumGroups=0;
        private Integer maxNumGroups=0;
        private String groupColumnLabel=null;
        private String fileColumnLabel=null;
        private boolean numValuesMustMatch=false;
        
        //quick and messy way to communicate parse error as a null objecect
        private boolean returnNull=false;
        
        public GroupInfo build() {
            if (returnNull) {
                return null;
            }
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
            if (pinfo==null) {
                throw new IllegalArgumentException("pinfo==null");
            }
            if (pinfo.getAttributes()==null) {
                log.error("pinfo.getAttributes()==null, can't parse");
                returnNull=true;
                return this;
            }
            
            //parse numGroups string 
            final String numGroupsStr = (String) pinfo.getAttributes().get(PROP_NUM_GROUPS); 
            if (StringUtils.isSet(numGroupsStr)) {
                final NumValuesParser nvParser=new NumValuesParserImpl();
                try { 
                    final NumValues numGroups = nvParser.parseNumValues(numGroupsStr);
                    this.minNumGroups=numGroups.getMin();
                    this.maxNumGroups=numGroups.getMax();
                }
                catch (Exception e) {
                    String message="Error parsing numGroups="+numGroupsStr;
                    log.error(message,e);
                    returnNull=true;
                    return this;
                }
            }
            else {
                //ignore everything else
                returnNull=true;
                return this;
            }
            
            final String groupColumnLabel = (String) pinfo.getAttributes().get(PROP_GROUP_COLUMN_LABEL);
            if (groupColumnLabel!=null) {
                this.groupColumnLabel=groupColumnLabel;
            }
            final String fileColumnLabel = (String) pinfo.getAttributes().get(PROP_FILE_COLUMN_LABEL);
            if (fileColumnLabel!=null) {
                this.fileColumnLabel=fileColumnLabel;
            }
            final String groupNumValuesMustMatchStr = (String) pinfo.getAttributes().get(PROP_NUM_VALUES_MUST_MATCH);
            if (StringUtils.isSet(groupNumValuesMustMatchStr)) {
                this.numValuesMustMatch=Boolean.valueOf(groupNumValuesMustMatchStr);
            }
            return this;
        }
    }

}
