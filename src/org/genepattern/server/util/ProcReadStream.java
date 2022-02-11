package org.genepattern.server.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class ProcReadStream implements Runnable {

    private static Logger log = Logger.getLogger(ProcReadStream.class);
    
        String name;
        InputStream is;
        Thread thread;      
        public ProcReadStream(String name, InputStream is) {
            this.name = name;
            this.is = is;
        }       
        public void start () {
            thread = new Thread (this);
            thread.start ();
        }       
        public void run () {
            try {
                InputStreamReader isr = new InputStreamReader (is);
                BufferedReader br = new BufferedReader (isr);   
                while (true) {
                    String s = br.readLine ();
                    if (s == null) break;
                    log.debug ("[" + name + "] " + s);
                }
                is.close ();    
            } catch (Exception ex) {
                log.error ("Problem reading stream " + name + "... :" + ex);
                ex.printStackTrace ();
            }
        }
    }
