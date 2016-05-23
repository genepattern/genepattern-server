/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

/**
 * Bean used to back the form found in the Config app
 *
 * @author Thorin Tabor
 */
public class StartConfigBean {
    private String email;
    private String daysPurge;
    private String timePurge;
    private String hsqlPort;
    private String r;
    private String r25;
    private String java;

    public StartConfigBean() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getDaysPurge() {
        return daysPurge;
    }

    public void setDaysPurge(final String daysPurge) {
        this.daysPurge = daysPurge;
    }

    public String getTimePurge() {
        return timePurge;
    }

    public void setTimePurge(final String timePurge) {
        this.timePurge = timePurge;
    }

    public String getHsqlPort() {
        return hsqlPort;
    }

    public void setHsqlPort(final String hsqlPort) {
        this.hsqlPort = hsqlPort;
    }

    public String getR25() {
        return r25;
    }

    public void setR25(final String r25) {
        this.r25 = r25;
    }

    public String getR() {
        return r;
    }

    public void setR(final String r) {
        this.r = r;
    }

    public String getJava() {
        return java;
    }

    public void setJava(final String java) {
        this.java = java;
    }
}