/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package demo;

public class RepeaterExampleBean {
    private String text;

    public RepeaterExampleBean(){}
    
    public String getText() {
        System.out.println("GetText");
        return text;
    }

    public void setText(String text) {
        System.out.println("SetText");
        this.text = text;
    }
    
    public String submit() {
        return null;
    }
}
