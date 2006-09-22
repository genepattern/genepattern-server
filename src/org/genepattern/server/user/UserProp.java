package org.genepattern.server.user;

public class UserProp {
    private Integer id;
    /**
     * auto generated
     * 
     * @es_generated
     */
    private String key;
    /**
     * auto generated
     * 
     * @es_generated
     */
    private String value;
    /**
     * auto generated
     * 
     * @es_generated
     */
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
