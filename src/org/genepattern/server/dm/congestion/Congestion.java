package org.genepattern.server.dm.congestion;

import javax.persistence.*;

/**
 * DAO for getting congestion objects from the database
 * @author Thorin Tabor
 */
@Entity
@Table(name="task_congestion", uniqueConstraints = {@UniqueConstraint(columnNames={"lsid"})})
public class Congestion {
    @Id
    @GeneratedValue
    private long id;

    private String lsid;
    private long runtime;
    private long queuetime;

    @Column(name = "virtual_queue")
    private String virtualQueue;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public long getRuntime() {
        return runtime;
    }

    public void setRuntime(long runtime) {
        this.runtime = runtime;
    }

    public long getQueuetime() {
        return queuetime;
    }

    public void setQueuetime(long runtime) {
        this.runtime = queuetime;
    }

    public String getVirtualQueue() {
        return virtualQueue;
    }

    public void setVirtualQueue(String virtualQueue) {
        this.virtualQueue = virtualQueue;
    }
}
