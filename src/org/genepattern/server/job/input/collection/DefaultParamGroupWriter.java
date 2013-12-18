package org.genepattern.server.job.input.collection;
import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.GroupInfo;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;

/**
 * Default implementation of the ParamGroupWriter interface,
 * assumes that all of the external files have already been transferred to the server.
 * 
 * @author pcarr
 *
 */
public class DefaultParamGroupWriter implements ParamGroupWriter {
    private final File toFile;
    private final TableWriter tableFileWriter;
    final Column[] columns={ Column.VALUE, Column.GROUP, Column.URL };
    final boolean includeHeader=true;

    private DefaultParamGroupWriter(final Builder in) {
        if (in.toFile==null) {
            throw new IllegalArgumentException("toFile==null");
        }
        this.toFile=in.toFile;
        this.tableFileWriter=new TsvWriter();
    }

    @Override
    public void writeParamGroup(final GroupInfo groupInfo, final Param inputParam, final List<GpFilePath> files) throws Exception {
        if (inputParam==null) {
            throw new IllegalArgumentException("inputParam==null");
        }
        if (files==null) {
            throw new IllegalArgumentException("files==null");
        }
        if (inputParam.getNumValues() != files.size()) {
            throw new IllegalArgumentException(
                    "numValues in inputParam must match num files, numValues="+inputParam.getNumValues()+
                    ", "+files.size());
        }
        try {
            tableFileWriter.init(toFile);
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
            if (t instanceof Exception) {
                throw  (Exception) t;
            }
            throw new Exception("Unexpected error writing group file to "+toFile, t);
        }
        finally {
            tableFileWriter.finish();
        }
    }

    private void writeHeader(final GroupInfo groupInfo) throws Exception {
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
        tableFileWriter.writeRow(header);
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

    private void writeRow(final int rowIdx, final GroupId groupId, final GpFilePath gpFilePath) throws Exception {
        final String[] row=new String[columns.length];
        int i=0;
        for(final Column col : columns) {
            final String value = getRowValue(rowIdx, col, groupId, gpFilePath);
            row[i++]=value;
        }
        tableFileWriter.writeRow(row);
    } 

    private String getRowValue(final Integer rowIdx, final Column column, final GroupId groupId, final GpFilePath gpFilePath) {
        switch (column) {
        case VALUE: return gpFilePath.getServerFile().getAbsolutePath();
        case GROUP: 
            try {
                return groupId.toString();
            }
            catch (Throwable t) {
                return "";
            }
        case URL:
            try {
                return gpFilePath.getUrl().toExternalForm();
            }
            catch (Exception e) {

            }
        }
        return "";
    }
    
    public static class Builder {
        private File toFile=null;
        public Builder(final File toFile) {
            this.toFile=toFile;
        }
        DefaultParamGroupWriter build() {
            return new DefaultParamGroupWriter(this);
        }
    }
}