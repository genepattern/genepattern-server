/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package demo;

import java.util.Calendar;

public class CurrentTimeBean {
    
    public String getCurrentTime() {
        
        return Calendar.getInstance().getTime().toString();
    }

}
