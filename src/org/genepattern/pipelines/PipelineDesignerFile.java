package org.genepattern.pipelines;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;

public class PipelineDesignerFile extends File {
    private static final long serialVersionUID = 2244908506129495781L;
    public static Logger log = Logger.getLogger(PipelineDesignerFile.class);
    public static final String PIPELINE_DESIGNER_FILE = ".pipelineDesigner";
    
    public PipelineDesignerFile(File dir) {
        super(dir, PIPELINE_DESIGNER_FILE);   
    }
    
    public void writeLegacy(List<ModuleJSON> modules) throws JSONException {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(this));
            int counter = 0;
            
            for (ModuleJSON module : modules) {
                writer.println(counter + " " + module.getTop() + " " + module.getLeft());
                counter++;
            }
            
            writer.close();
        }
        catch (IOException e) {
            log.error("Unable to write to .pipelineDesigner");
        }
    }
    
    public List<String[]> readLegacy() {
        List<String[]> fileReads = new ArrayList<String[]>();
        if (this.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(this))));
                String line = null;
                Integer expected = 0;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split(" ");
                    if (parts[0].equals(expected.toString())) {
                        String[] toInsert = new String[2];
                        if (parts.length >= 2) {
                            toInsert[0] = parts[1];
                        }
                        else {
                            toInsert[0] = null;
                        }
                        if (parts.length >= 3) {
                            toInsert[1] = parts[2];
                        }
                        else {
                            toInsert[1] = null;
                        }
                        fileReads.add(toInsert);
                        expected++;
                    }
                }
            }
            catch (Exception e) {
                log.error("ERROR: Reading pipeline designer file on file load");
                return null;
            }
            return fileReads;
        }
        else {
            return new ArrayList<String[]>();
        }
    }
    
    public void write() {
        
    }
    
    public void read() {
        
    }
}
