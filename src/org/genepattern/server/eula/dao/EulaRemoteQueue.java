/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula.dao;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * Hibernate mapping class to the 'eula_remote_queue' table.
 * 
 * Which stores eula_info for items which should be posted to a remote service,
 * and which have not yet successfully been recored remotely.
 * 
 * @author pcarr
 */
@Entity
@IdClass(EulaRemoteQueue.Key.class)
@Table(name="eula_remote_queue")
public class EulaRemoteQueue {
    /**
     * This is the composite primary key for the eula_remote_queue table and the EulaRemoteQueue hibernate mapping class.
     * 
     * @author pcarr
     */
    final static public class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        private static boolean strcmp(String arg1, String arg2) {
            if (arg1==null) {
                return arg2==null;
            }
            return arg1.equals(arg2);
        }

        @Column(name="eula_record_id")
        private long eulaRecordId;

        @Column(name="remote_url")
        private String remoteUrl=null;

        public Key() {
        }
        public Key(final long eulaRecordId, final String remoteUrl) {
            this.eulaRecordId=eulaRecordId;
            this.remoteUrl=remoteUrl;
        }

        public Long getEulaRecordId() {
            return eulaRecordId;
        }
        public void setEulaRecordId(final long id) {
            this.eulaRecordId=id;
        }
        public String getRemoteUrl() {
            return remoteUrl;
        }
        public void setRemoteUrl(final String url) {
            this.remoteUrl=url;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return eulaRecordId == key.eulaRecordId && strcmp(remoteUrl, key.remoteUrl);
        }

        public int hashCode() {
            String str="id="+eulaRecordId+",url="+remoteUrl;
            return str.hashCode();
        }
    }
    
    public EulaRemoteQueue() {
    }
    public EulaRemoteQueue(Key key) {
        this.eulaRecordId=key.getEulaRecordId();
        this.remoteUrl=key.getRemoteUrl();
    }
    
    //this is a foreign key to the eula_record table
    @Id
    private long eulaRecordId;

    @Id
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
