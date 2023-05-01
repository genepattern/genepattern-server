/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.message;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A System message - a single message to be displayed as an alert message.
 * 
 * Maps to the SYSTEM_MESSAGE table in the GP database.
 * @author pcarr
 */
public class SystemMessage implements Serializable {
    private Long id = new Long(0); // INTEGER identity primary key,
    private String message = ""; // LONGVARCHAR not null,
    private Date startTime = null; // TIMESTAMP default now not null,
    private Date endTime = null; // TIMESTAMP null,
    private boolean deleteOnRestart = false; // BIT default 0 not null
    
    public void setId(Long id) {
        this.id = id;
    }
    public Long getId() {
        return id;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getStartTime() {
        return startTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    public String getFormattedStartTime(){
        String pattern = "dd MMMM yyyy, HH:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(startTime);
    }
    
    
    public Date getEndTime() {
        return endTime;
    }
    public void setDeleteOnRestart(boolean b) {
        this.deleteOnRestart = b;
    }
    public boolean isDeleteOnRestart() {
        return deleteOnRestart;
    }
}
