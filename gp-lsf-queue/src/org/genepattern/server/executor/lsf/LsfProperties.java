package org.genepattern.server.executor.lsf;

import org.apache.log4j.Logger;

public class LsfProperties {
    private static Logger log = Logger.getLogger(LsfProperties.class);
    
    public enum Key {
        PROJECT("lsf.project"),
        QUEUE("lsf.queue"),
        MAX_MEMORY("lsf.max.memory"),
        WRAPPER_SCRIPT("lsf.wrapper.script"),
        OUTPUT_FILENAME("lsf.output.filename"),
        USE_PRE_EXEC_COMMAND("lsf.use.pre.exec.command"),
        HOST_OS("lsf.host.os"),
        EXTRA_BSUB_ARGS("lsf.extra.bsub.args");
        
        private String key="lsf.key";
        Key(String key) {
            this.key = key;
            if (key == null) {
                key = name();
            }
        }
        public String getKey() {
            return key;
        }
    }
    
    //private Map<Key,String> props = new HashMap<Key,String>();
    
    //public void put(Key key, String value) {
    //    props.put(key, value);
    //}
    //public String get(Key propertyName) {
    //    return props.get(propertyName);
    //}
    //public boolean getAsBoolean(Key propertyName) {
    //    String v = props.get(propertyName);
    //    return Boolean.valueOf(v);
    //}
    
    //public void validate() {
    //    validateMaxMemory();
    //    validateWrapperScript();
    //}
    
//    private void validateMaxMemory() {
//        String s = props.get(Key.MAX_MEMORY);
//        if (s == null) {
//            //Note: hard coded default setting
//            props.put(Key.MAX_MEMORY, "2");
//            return;
//        }
//        try {
//            Integer.parseInt(s);
//        }
//        catch (NumberFormatException e) {
//            log.error("Invalid setting for 'lsf.max.memory="+s+"': "+e.getLocalizedMessage(), e);
//            //Note: hard coded default setting
//            props.put(Key.MAX_MEMORY, "2");
//            return;
//        }
//    }
//    
//    private void validateWrapperScript() {
//        String s = props.get(Key.WRAPPER_SCRIPT);
//        log.debug("setting lsf.wrapper.script: "+s+" ...");
//        if (s != null) {
//          File f = new File(s);
//          if (!f.isAbsolute()) {
//              f = new File(System.getProperty("genepattern.properties"), s);
//          }
//          if (!f.isFile() || !f.canRead()) {
//              log.error("Configuration error, 'lsf.wrapper.script="+s+"' can't read: "+f.getAbsolutePath());
//              props.put(Key.WRAPPER_SCRIPT, null);
//          }
//          else {
//              s=f.getAbsolutePath();
//              props.put(Key.WRAPPER_SCRIPT, s);
//          }
//      }
//      log.debug("lsf.wrapper.script="+s);
//    }
}
