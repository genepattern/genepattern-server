/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="gp_user_prop")
public class UserProp {
    /**
     * @param jobNumber
     * @return
     */
    public static final String getEmailNotificationPropKey(int jobNumber) {
        if (jobNumber < 0) {
            return null;
        }        
        return "sendEmailNotification_" + jobNumber;
    }
    
    @Id
    @GeneratedValue
    private Integer id;
    /**
     * auto generated
     * 
     * @es_generated
     */
    @Column(name="key")
    private String key;
    /**
     * auto generated
     * 
     * @es_generated
     */
    @Column
    private String value;
    /**
     * auto generated
     * 
     * @es_generated
     */
    @Column(name="gp_user_id")
    private String gpUserId;

    /**
     * auto generated
     * 
     * @es_generated
     */
    public UserProp() {
        super();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public UserProp(Integer id) {
        super();
        this.id = id;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public UserProp(Integer id, String key, String value, String gpUserId) {
        super();
        this.id = id;
        this.key = key;
        this.value = value;
        this.gpUserId = gpUserId;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public boolean equals(Object value) {
        // TODO Implement equals() using Business key equality.
        return super.equals(value);
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public int hashCode() {
        // TODO Implement hashCode() using Business key equality.
        return super.hashCode();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String toString() {
        // TODO Implement toString().
        return super.toString();
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setId(Integer value) {
        this.id = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getKey() {
        return this.key;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getValue() {
        return this.value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public String getGpUserId() {
        return this.gpUserId;
    }

    /**
     * auto generated
     * 
     * @es_generated
     */
    public void setGpUserId(String value) {
        this.gpUserId = value;
    }

}
