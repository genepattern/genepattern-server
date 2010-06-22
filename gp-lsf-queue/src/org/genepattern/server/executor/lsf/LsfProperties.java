package org.genepattern.server.executor.lsf;

public class LsfProperties {
    public enum Key {
        PROJECT("lsf.project"),
        QUEUE("lsf.queue"),
        MAX_MEMORY("lsf.max.memory"),
        WRAPPER_SCRIPT("lsf.wrapper.script"), //similar to command prefix, e.g. bsub <bsub.args> <lsf.wrapper.script> <cmd>
        STDOUT_FILE("lsf.stdout.file"),
        JOB_REPORT_FILE("lsf.job.report.file"), //the -o arg to bsub
        USE_PRE_EXEC_COMMAND("lsf.use.pre.exec.command"),
        HOST_OS("lsf.host.os"),
        EXTRA_BSUB_ARGS("lsf.extra.bsub.args"),
        JOB_COMPLETION_LISTENER("lsf.job.completion.listener"),
        IS_SCATTER_GATHER("lsf.scatter.gather"); //[true|false]
        
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
}
