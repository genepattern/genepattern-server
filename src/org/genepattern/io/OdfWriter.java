package org.genepattern.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;


public class OdfWriter
    extends PrintWriter {
    private static final String TASKLOG = "gp_task_execution_log.txt";

    public OdfWriter(OutputStream os, boolean prependExecutionLog) {
        super(os);
		  if(prependExecutionLog) {
			  copyExecutionLog();
		  }
    }

    public OdfWriter(Writer w, boolean prependExecutionLog) {
        super(w);
		  if(prependExecutionLog) {
			  copyExecutionLog();
		  }
    }

    public void printVersion() {
        println("ODF 1.0");
    }

    public void printHeaderArray(String key, String[] value) {
        print(key + ":");
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                print("\t");
            }
            print(value[i]);
        }
        println();
    }

    public void printHeader(String key, String value) {
        println(key + "=" + value);
    }

    protected void copyExecutionLog() {
        if (new File(TASKLOG).exists()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(TASKLOG));
                String s = null;
                while ((s = br.readLine()) != null) {
                    println(s);
                }
            } catch (IOException ioe) {}
             finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException x) {}
                }
            }
        }
    }
}
