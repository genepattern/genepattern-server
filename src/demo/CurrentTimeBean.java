/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package demo;

import java.util.Calendar;

public class CurrentTimeBean {
    
    public String getCurrentTime() {
        
        return Calendar.getInstance().getTime().toString();
    }

}
