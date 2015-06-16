/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
