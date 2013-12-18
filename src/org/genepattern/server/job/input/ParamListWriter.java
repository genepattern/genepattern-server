package org.genepattern.server.job.input;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;


import java.io.IOException;

import com.Ostermiller.util.CSVPrinter;


/**
 * Interface for writing parameter list files to be used as input to modules.
 * 
 * @author pcarr
 *
 */
public interface ParamListWriter {
    void writeParamList(GpFilePath toFile, List<GpFilePath> values) throws Exception;
    
    /**
     * Default implementation of the ParamListWriter class, outputs a new text file,
     * where each line is the fully qualified path to a data file.
     * 
     * @author pcarr
     */
    public static class Default implements ParamListWriter {
        private final String COL_DELIM="\t";
        private boolean writeTimestamp=false;
        
        /**
         * Write a new parameter list file from the list of values.
         * 
         */
        @Override
        public void writeParamList(GpFilePath toFile, List<GpFilePath> values) throws Exception {
            FileWriter writer = null;
            BufferedWriter out = null;
            try {
                writer = new FileWriter(toFile.getServerFile());
                out = new BufferedWriter(writer);
                for(GpFilePath filePath : values) {
                    File file = filePath.getServerFile();
                    out.write(file.getAbsolutePath());
                    if (writeTimestamp) {
                        out.write(COL_DELIM); out.write("timestamp="+file.lastModified());
                        out.write(COL_DELIM); out.write(" date="+new Date(file.lastModified())+" ");
                    }
                    out.newLine();
                }
            }
            finally {
                if (out != null) {
                    out.close();
                }
            }
        }
        
    }
    
    public static class Util {
        static void walkValues(final Param param) {
            for(final Entry<GroupId,Collection<ParamValue>> groupEntry : param.getGroupedValues().entrySet()) {
                final GroupId groupId=groupEntry.getKey();
                for(final ParamValue paramValue : groupEntry.getValue()) {
                    //TODO: visit(groupId, paramValue);
                }
            }
        }
    }

    public interface TableFileWriter {
        void init(final GpFilePath toFile) throws Exception;
        void writeComment(final String comment) throws Exception;
        void writeRow(final String[] values) throws Exception;
        void finish() throws Exception;
    }
    
    public static class ParamGroupWriter implements ParamListWriter {
        private static final Logger log = Logger.getLogger(ParamGroupWriter.class);
        public enum Column {
            VALUE,
            GROUP,
            URL
        }
        
        private final boolean includeHeader=true;
        private final TableFileWriter writer;
        private final Column[] columns={ Column.VALUE, Column.GROUP, Column.URL };
        private final List<String> groupIds;
        
        private ParamGroupWriter(final Builder in) {
            this.writer=in.writer;
            this.groupIds=new ArrayList<String>( in.groupIds );
        }
        
        private String getRowValue(final Integer rowIdx, final Column column, final GpFilePath gpFilePath) {
            switch (column) {
            case VALUE: return gpFilePath.getServerFile().getAbsolutePath();
            case GROUP: 
                try {
                    return groupIds.get(rowIdx);
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
        public void writeParamList(final GpFilePath toFile, final List<GpFilePath> values) throws Exception {
            try {
                writer.init(toFile);
                if (includeHeader) {
                    writeHeader();
                }
                int rowIdx=0;
                for(final GpFilePath filePath : values) {
                    writeRow(rowIdx, filePath);
                    ++rowIdx;
                }
            }
            finally {
                writer.finish();
            }
        }
        
        private void writeHeader() throws Exception {
            final String[] header=new String[columns.length];
            int i=0;
            for(Column col : columns) {
                header[i++]=col.name();
            }
            writer.writeRow(header);
        }
        
        private void writeRow(final int rowIdx, final GpFilePath gpFilePath) throws Exception {
            final String[] row=new String[columns.length];
            int i=0;
            for(final Column col : columns) {
                final String value = getRowValue(rowIdx, col, gpFilePath);
                row[i++]=value;
            }
            writer.writeRow(row);
        } 
        
        public static class Builder {
            private final TableFileWriter writer=new CsvWriter();
            private final List<String> groupIds=new ArrayList<String>();

            Builder addValue(final GpFilePath gpFilePath, final String groupId) {
                groupIds.add(groupId);
                return this;
            }
            ParamListWriter build() {
                return new ParamGroupWriter(this);
            }
        }
    }

    public static class CsvWriter implements TableFileWriter {
        private static final Logger log = Logger.getLogger(CsvWriter.class);
        final boolean alwaysQuote=false; //don't quote entries
        final char commentStart='#';
        final char quoteChar='\"';
        final char delim='\t'; //TAB delimited file
        final String lineEnding="\n";
        CSVPrinter printer = null;

        @Override
        public void init(GpFilePath toFile) throws IOException {
            FileWriter writer = null;
            BufferedWriter out = null;
            writer = new FileWriter(toFile.getServerFile());
            out = new BufferedWriter(writer);
                
            final boolean autoFlush=true;
            printer=new CSVPrinter(out, commentStart, quoteChar, delim, lineEnding, alwaysQuote, autoFlush);
        }

        @Override
        public void writeComment(final String comment) throws IOException {
            if (printer==null) {
                log.error("");
            }
            printer.writelnComment(comment);
            
        }

        @Override
        public void writeRow(final String[] values) throws IOException {
            printer.writeln(values);
        }

        @Override
        public void finish() throws IOException {
            if (printer != null) {
                printer.close();
            }
        }
    }

}
