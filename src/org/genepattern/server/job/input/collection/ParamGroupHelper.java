package org.genepattern.server.job.input.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.job.input.ParamListHelper.Record;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.collection.ParamGroupWriter.Column;

public class ParamGroupHelper {

    public static GpFilePath initToFile(final Context jobContext, final Param param) throws Exception {
        //now, create a new group file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
        final int index=-1;
        final String pname=param.getParamId().getFqName();
        final String filename=".group.csv";
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filename);
        return gpFilePath;
    }
    
    public static void walkValuesByGroup(final Param param, final GroupVisitor visitor) {
        boolean errors=false;
        try {
            visitor.start();
            for(final Entry<GroupId,Collection<ParamValue>> groupEntry : param.getGroupedValues().entrySet()) {
                final GroupId groupId=groupEntry.getKey();
                visitor.startGroup(groupId);
                for(final ParamValue paramValue : groupEntry.getValue()) {
                    visitor.visitValue(groupId, paramValue);
                }
                visitor.finishGroup(groupId);
            }
        }
        catch (Throwable t) {
            //TODO: log errors
            errors=true;
        }
        visitor.finish(errors);
    }
    public static void walkValuesInOrder(final Param param, final ParamValueVisitor visitor) {
        boolean withErrors=false;
        try {
            visitor.start();
            for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                visitor.visitValue(entry.getKey(), entry.getValue());
            }
        }
        catch (Throwable t) {
            //TODO: handle errors
            withErrors=true;
        }
        visitor.finish(withErrors);
    }
    
    public static ParamGroupHelper create(final Context jobContext, final Param param) throws Exception {
        if (jobContext==null) { 
            throw new IllegalArgumentException("jobContext==null");
        }
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        GpFilePath toFile=initToFile(jobContext, param);
        ParamGroupHelper pgh=new ParamGroupHelper(toFile, param);
        return pgh;
    }
    
    private final GpFilePath toFile;
    private final Param param;

    public ParamGroupHelper(final GpFilePath toFile, final Param param) {
        this.toFile=toFile;
        this.param=param;
    }
    
    public GpFilePath getToFile() {
        return toFile;
    }
    
    public List<GpFilePath> downloadExternalUrl(final Context jobContext) {
        //use a guava table to store a Map of GpFilePath, where each row is an index, and each column is a groupId
        //final Table<Integer, GroupId, GpFilePath> table=HashBasedTable.create();
        final List<GpFilePath> gpFilePaths=new ArrayList<GpFilePath>();
        ParamValueVisitor visitor=new ParamValueVisitor() {
            int idx=0;
            final boolean downloadExternalFiles=true;

            @Override
            public void start() throws Exception {
            }


            @Override
            public void visitValue(GroupId groupId, ParamValue value) throws Exception {
                try {
                    final Record rec=ParamListHelper.initFromValue(jobContext, value);
                    //if necessary, download data from external sites
                    if (downloadExternalFiles) {
                        if (rec.getType().equals(Record.Type.EXTERNAL_URL)) {
                            ParamListHelper.forFileListCopyExternalUrlToUserUploads(jobContext, rec.getGpFilePath(), rec.getUrl());
                        }
                    }
                    gpFilePaths.add(rec.getGpFilePath());
                }
                catch (Throwable t) {
                    //TODO: handle exception
                }
                ++idx;
            }


            @Override
            public void finish(final boolean withError) {
            }
        };
        
        walkValuesInOrder(param, visitor);
        return gpFilePaths;
    }
    
    
    public void writeGroupFile(final List<GpFilePath> gpFilePaths) {
        final TableWriter writer=new TsvWriter();
        final Column[] columns={ Column.VALUE, Column.GROUP, Column.URL };
        final boolean includeHeader=true;

        final ParamValueVisitor visitor=new ParamValueVisitor() {
            int idx=0;

            private void writeHeader() throws Exception {
                final String[] header=new String[columns.length];
                int i=0;
                for(Column col : columns) {
                    header[i++]=col.name();
                }
                writer.writeRow(header);
            }
            
            private void writeRow(final int rowIdx, final GroupId groupId, final GpFilePath gpFilePath) throws Exception {
                final String[] row=new String[columns.length];
                int i=0;
                for(final Column col : columns) {
                    final String value = getRowValue(rowIdx, col, groupId, gpFilePath);
                    row[i++]=value;
                }
                writer.writeRow(row);
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

            @Override
            public void start() throws Exception {
                writer.init(toFile.getServerFile());
                if (includeHeader) {
                    writeHeader();
                }
            }

            @Override
            public void visitValue(final GroupId groupId, final ParamValue value) throws Exception {
                final GpFilePath gpFilePath=gpFilePaths.get(idx);
                writeRow(idx, groupId, gpFilePath);
                ++idx;
            }


            @Override
            public void finish(final boolean withError) {
                try {
                    writer.finish();
                }
                catch (Throwable t) {
                    //TODO: handle exceptions
                }
            }
        };
        walkValuesInOrder(param, visitor);
    }
    
}
