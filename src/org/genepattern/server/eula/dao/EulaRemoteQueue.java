package org.genepattern.server.eula.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Hibernate mapping class to the 'eula_remote_queue' table.
 * 
 * Which stores eula_info for items which should be posted to a remote service,
 * and which have not yet successfully been recored remotely.
 * 
 * @author pcarr
 */
@Entity
@Table(name="eula_remote_queue", uniqueConstraints = {@UniqueConstraint(columnNames={"eula_record_id", "remote_url"})})
public class EulaRemoteQueue {
    //this is a foreign key to the eula_record table
    @Id
    @Column(name="eula_record_id")
    private long eulaRecordId;

    @Column(name="remote_url")
    private String remoteUrl;
    
    private boolean recorded=false;

    @Column(name="date_recorded")
    private Date dateRecorded=new Date();

    @Column(name="num_attempts")
    private int numAttempts=0;

    public long getEulaRecordId() {
        return eulaRecordId;
    }

    public void setEulaRecordId(final long id) {
        this.eulaRecordId=id;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(final String remoteUrl) {
        this.remoteUrl=remoteUrl;
    }

    public boolean getRecorded() {
        return recorded;
    }

    public void setRecorded(final boolean recorded) {
        this.recorded = recorded;
    }

    public Date getDateRecorded() {
        return new Date(dateRecorded.getTime());
    }

    public void setDateRecorded(final Date date) {
        this.dateRecorded = new Date(date.getTime());
    }

    public int getNumAttempts() {
        return numAttempts;
    }

    public void setNumAttempts(final int count) {
        this.numAttempts = count;
    }

}
