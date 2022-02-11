/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;

import java.util.List;

import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListException;

/**
 * Interface for writing a parameter group, in which the parameters are optionally
 * organized into groups.
 * The default format is as a tab-delimited text file.
 * The first row is a header, each subsequent row includes the fully qualified path to the file in column 1,
 * the groupId in column2, and possibly the url to the file in column 3.
 * The GP server may append more columns, make sure to implement your reader in such a way that it can
 * ignore arbitrary number of additional columns, as well as rows preceded by a comment character.
 * 
 * Example parameter group file: 
 * <pre>

 * </pre>
 * @author pcarr
 *
 */
public interface ParamGroupWriter {
    public enum Column {
        VALUE,
        GROUP,
        URL
    }
    
    void writeParamGroup(GroupInfo groupInfo, Param inputParam, List<GpFilePath> files) throws ParamListException;
}
