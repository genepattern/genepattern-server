/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package demo;

public class PollExampleBean {
    private Integer counter;

    public PollExampleBean() {
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }
    
    public String incCounter() {
        counter++;
        return null;
    }

    public String resetCounter() {
        counter = 0;
        return null;
    }
    
}
