/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.pipelines;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.pipelines.PipelineQueryServlet.FileCollection;
import org.yaml.snakeyaml.Yaml;

public class PipelineDesignerFile extends File {
    private static final long serialVersionUID = 2244908506129495781L;
    public static Logger log = Logger.getLogger(PipelineDesignerFile.class);
    public static final String PIPELINE_DESIGNER_FILE = ".pipelineDesigner";
    public static final int PIPELINE_DESIGNER_FILE_VERSION = 1;
    
    public PipelineDesignerFile(File dir) {
        super(dir, PIPELINE_DESIGNER_FILE);   
    }
    
    public String fixLegacyPosition(String position) {
        String withoutPX = position.substring(0, position.length() - 2);
        int pos = Integer.parseInt(withoutPX);
        pos += 200;
        return pos + "px";
    }

    public Map<String, Object> readLegacy() {
        Map<String, Object> reads = new HashMap<String, Object>();
        reads.put("modules", new HashMap<Integer, Map<String, String>>());
        
        if (this.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(this))));
                String line = null;
                Integer expected = 0;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split(" ");
                    if (parts[0].equals(expected.toString())) {
                        Map<String, String> toInsert = new HashMap<String, String>();
                        if (parts.length >= 2) {
                            toInsert.put("top", fixLegacyPosition(parts[1]));
                        }
                        if (parts.length >= 3) {
                            toInsert.put("left", fixLegacyPosition(parts[2]));
                        }
                        ((HashMap<Integer, Map<String, String>>) reads.get("modules")).put(expected, toInsert);
                        expected++;
                    }
                }
            }
            catch (Exception e) {
                log.error("ERROR: Reading pipeline designer file on file load");
                return null;
            }
            return reads;
        }
        else {
            return reads;
        }
    }
    
    public void write(List<ModuleJSON> modules, FileCollection files) {
        String toYaml = "version: " + PIPELINE_DESIGNER_FILE_VERSION + "\n";
        
        try {
            // Write the modules
            int counter = 0;
            toYaml += "modules" + ":\n";
            for (ModuleJSON module : modules) {
                toYaml += "    " + counter + ":\n";
                toYaml += "        " + "type: module\n";
                toYaml += "        " + "top: " + module.getTop() + "\n";
                toYaml += "        " + "left: " + module.getLeft() + "\n";
                counter++;
            }
            
            // Write the files
            counter = 0;
            toYaml += "files" + ":\n";
            if (files.doc != null) {
                toYaml += "    " + counter + ":\n";
                toYaml += "        " + "type: doc\n";
                toYaml += "        " + "name: " + files.doc.getName() + "\n";
                counter++;
            }
            if (files.license != null) {
                toYaml += "    " + counter + ":\n";
                toYaml += "        " + "type: license\n";
                toYaml += "        " + "name: " + files.license.getName() + "\n";
                counter++;
            }
            for (File file : files.inputFiles) {
                toYaml += "    " + counter + ":\n";
                toYaml += "        " + "type: file\n";
                toYaml += "        " + "name: " + file.getName() + "\n";
                toYaml += "        " + "top: " + files.positions.get(file).get("top") + "\n";
                toYaml += "        " + "left: " + files.positions.get(file).get("left") + "\n";
                counter++;
            }
            for (String file : files.urls) {
                toYaml += "    " + counter + ":\n";
                toYaml += "        " + "type: url\n";
                toYaml += "        " + "name: " + file + "\n";
                toYaml += "        " + "top: " + files.positions.get(file).get("top") + "\n";
                toYaml += "        " + "left: " + files.positions.get(file).get("left") + "\n";
                counter++;
            }
            
            PrintWriter writer = new PrintWriter(new FileWriter(this));
            writer.print(toYaml);
            writer.close();
        }
        catch (Exception e) {
            log.error("Unable to write to .pipelineDesigner");
        }
    }
    
    public Map<String, Object> read() {
        Map<String, Object> pdFile = null;
        try {
            Yaml yaml = new Yaml();
            pdFile = (Map<String, Object>) yaml.load(new FileInputStream(this));
        }
        catch (Throwable t) {
            // Fall back to the legacy loader if there was an error parsing the yaml
            return this.readLegacy();
        }
        
        return pdFile;
    }
}
