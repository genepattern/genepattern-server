/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.genepattern.server.dm.GpFilePath;

/**
 * Utility class for creating parameter list files to be used as input to modules. 
 * Save the input file list to a text file.
 * Each line is the fully qualified path to a data file.
 * 
 * @author pcarr
 */
public class ParamListWriter {

    private static final String COL_DELIM="\t";
    private static final boolean writeTimestamp=false;

    public ParamListWriter() {
    }

    /**
     * Write a new parameter list file from the list of values.
     * @throws ParamListException, IOException 
     *
     */
    public void writeParamList(GpFilePath toFile, List<GpFilePath> values) throws ParamListException, IOException {
        writeParamList(toFile, values, false);
    }

    /**
     * Write a new parameter list file from the list of values.
     * @throws IOException 
     *
     */
    public void writeParamList(final GpFilePath toFile, final List<GpFilePath> values, final boolean urlMode)
    throws IOException 
    { 
        FileWriter writer = null;
        BufferedWriter out = null;
        try {
            writer = new FileWriter(toFile.getServerFile());
            out = new BufferedWriter(writer);
            for(GpFilePath filePath : values) {
                if(urlMode)
                {
                    out.write(filePath.getUrl().toExternalForm());
                }
                else
                {
                    File file = filePath.getServerFile();
                    out.write(file.getAbsolutePath());
                    if (writeTimestamp) {
                        out.write(COL_DELIM); out.write("timestamp="+file.lastModified());
                        out.write(COL_DELIM); out.write(" date="+new Date(file.lastModified())+" ");
                    }
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
