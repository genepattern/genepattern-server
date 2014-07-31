package org.genepattern.server.dm.congestion;

import javax.persistence.*;

/**
 * DAO for getting congestion objects from the database
 * @author Thorin Tabor
 */
@Entity
@Table(name="queue_congestion", uniqueConstraints = {@UniqueConstraint(columnNames={"queue"})})
public class Congestion {
    @Id
    @GeneratedValue
    private long id;

    private long queuetime;
    private String queue;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getQueuetime() {
        return queuetime;
    }

    public void setQueuetime(long queuetime) {
        this.queuetime = queuetime;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}
