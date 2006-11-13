package demo;

import java.util.Calendar;

public class CurrentTimeBean {
    
    public String getCurrentTime() {
        
        return Calendar.getInstance().getTime().toString();
    }

}
