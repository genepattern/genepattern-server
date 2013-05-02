package org.genepattern.server.executor.lsf;

public class LsfProperties {
    public enum Key {
        PROJECT("lsf.project"),
        QUEUE("lsf.queue"),
        MAX_MEMORY("lsf.max.memory"),
        STDOUT_FILE("lsf.stdout.file"),
        JOB_REPORT_FILE("lsf.job.report.file"), //the -o arg to bsub
        USE_PRE_EXEC_COMMAND("lsf.use.pre.exec.command"),
        PRE_EXEC_STANDARD_DIRECTORIES("lsf.pre.exec.standard.directories"),
        HOST_OS("lsf.host.os"),
        EXTRA_BSUB_ARGS("lsf.extra.bsub.args"),
        JOB_COMPLETION_LISTENER("lsf.job.completion.listener"),
        PRIORITY("lsf.priority"),
        CPU_SLOTS("lsf.cpu.slots"); // the -n arg to bsub
        
        private String key="lsf.key";
        Key(String key) {
            this.key = key;
            if (key == null) {
                key = name();
            }
        }
        public String getKey() {
            return this.key;
        }
    }
}
