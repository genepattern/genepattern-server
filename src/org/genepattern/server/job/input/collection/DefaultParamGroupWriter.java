/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.collection;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListException;
import org.genepattern.server.job.input.ParamValue;

/**
 * Default implementation of the ParamGroupWriter interface,
 * assumes that all of the external files have already been transferred to the server.
 * 
 * @author pcarr
 *
 */
public class DefaultParamGroupWriter implements ParamGroupWriter {
    private static final Logger log = Logger.getLogger(ParamGroupWriter.class);

    private final File toFile;
    private final TsvWriter tsvWriter;

    /**
     * The columnSpec defines the number of columns to included in the generated file.
     * For example, a filelist file would be,
     * <pre>
       columnSpec = new Column[] { Column.VALUE };
     * </pre>
     * For example, a grouplist file would be,
     * <pre>
       columnSpec = new Column[] { Column.VALUE, Column.GROUP, Column.URL };
     * </pre> 
     */
    final Column[] columns;
    final boolean includeHeader;
    final String baseGpHref; // e.g. 'http://127.0.0.1:8080/gp'

    @SuppressWarnings("deprecation")
    private DefaultParamGroupWriter(final Builder in) {
        if (in.toFile==null) {
            throw new IllegalArgumentException("toFile==null");
        }
        if (in.columns==null) {
            this.columns=new Column[]{ Column.VALUE, Column.GROUP, Column.URL };
        }
        else {
            this.columns=in.columns.toArray(new Column[in.columns.size()]);
        }
        this.includeHeader=in.includeHeader;
        this.toFile=in.toFile;
        this.tsvWriter=new TsvWriter();
        if (in.baseGpHref==null) {
            log.error("baseGpHref not set; getting default value from GpConfig");
            this.baseGpHref=UrlUtil.getBaseGpHref(ServerConfigurationFactory.instance());
        }
        else {
            this.baseGpHref=in.baseGpHref;
        }
    }
    
    @Override
    public void writeParamGroup(final GroupInfo groupInfo, final Param inputParam, final List<GpFilePath> files) throws ParamListException {
        if (inputParam==null) {
            throw new IllegalArgumentException("inputParam==null");
        }
        if (files==null) {
            throw new IllegalArgumentException("files==null");
        }
        if (inputParam.getNumValues() != files.size()) {
            throw new ParamListException(
                    "numValues in inputParam must match num files, numValues="+inputParam.getNumValues()+
                    ", "+files.size());
        }
        try {
            tsvWriter.init(toFile);
            if (includeHeader) {
                writeHeader(groupInfo);
            }
            int idx=0;
            for(final Entry<GroupId,ParamValue> entry : inputParam.getValuesAsEntries()) {
                final GroupId groupId=entry.getKey();
                final GpFilePath gpFilePath=files.get(idx);
                writeRow(idx, groupId, gpFilePath);
                ++idx;
            }
        }
        catch (Throwable t) {
            throw new ParamListException("Error writing group file='"+toFile+"'", t);
        }
        finally {
            try {
                tsvWriter.finish();
            }
            catch (Throwable t) {
                throw new ParamListException("Error closing group file='"+toFile+"'", t);
            }
        }
    }

    private void writeHeader(final GroupInfo groupInfo) throws IOException {
        //special-case for custom label for group and file columns
        if (groupInfo != null) {
            groupInfo.getGroupColumnLabel();
            groupInfo.getFileColumnLabel();
        }
        final String[] header=new String[columns.length];
        int i=0;
        for(Column col : columns) {
            header[i++]=getHeaderValue(groupInfo, col);
        }
        tsvWriter.writeRow(header);
    }
    
    private String getHeaderValue(final GroupInfo groupInfo, final Column col) {
        if (groupInfo==null) {
            return col.name();
        }
        if (col == Column.GROUP && groupInfo.getGroupColumnLabel() != null) {
            return groupInfo.getGroupColumnLabel();
        }
        if (col == Column.VALUE && groupInfo.getFileColumnLabel() != null) {
            return groupInfo.getFileColumnLabel();
        }
        return col.name();
    }

    private void writeRow(final int rowIdx, final GroupId groupId, final GpFilePath gpFilePath) throws IOException {
        final String[] row=new String[columns.length];
        int i=0;
        for(final Column col : columns) {
            final String value = getRowValue(rowIdx, col, groupId, gpFilePath);
            row[i++]=value;
        }
        tsvWriter.writeRow(row);
    } 

    protected String getRowValue(final Integer rowIdx, final Column column, final GroupId groupId, final GpFilePath gpFilePath) {
        Throwable ex=null;
        switch (column) {
        case VALUE: 
            try {
                return gpFilePath.getServerFile().getAbsolutePath();
            }
            catch (Throwable t) {
                ex=t;
            }
        case GROUP: 
            try {
                return groupId.getGroupId();
            }
            catch (Throwable t) {
                ex=t;
            }
        case URL:
            try {
                if (gpFilePath.isLocal()) {
                    return baseGpHref+gpFilePath.getRelativeUri().toString();
                }
                else {
                    return gpFilePath.getUrl().toExternalForm();
                }
            }
            catch (Throwable t) {
                ex=t;
            }
        }
        log.error("did not initialize value from column="+column+", rowIdx="+rowIdx+", groupId="+groupId, ex);
        return "";
    }
    
    public static class Builder {
        private String baseGpHref=null;
        private File toFile=null;
        private List<Column> columns=null;
        private boolean includeHeader=false;

        public Builder(final File toFile) {
            this.toFile=toFile;
        }
        public Builder baseGpHref(final String baseGpHref) {
            this.baseGpHref=baseGpHref;
            return this;
        } 
        public Builder addColumn(final Column column) {
            if (columns==null) {
                columns=new ArrayList<Column>();
            }
            columns.add(column);
            return this;
        }
        public Builder includeHeader(final boolean includeHeader) {
            this.includeHeader=includeHeader;
            return this;
        }
        
        DefaultParamGroupWriter build() {
            return new DefaultParamGroupWriter(this);
        }
    }
}
