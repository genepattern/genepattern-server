/*
 * ImageDrawer.java
 *
 * Created on December 14, 2001, 4:01 PM
 */

package org.genepattern.modules.ui.graphics;

/**
 * Classes that implement this interface know how to draw themselves 
 *
 * @author  KOhm
 * @version 
 */
public interface ImageDrawer {
    /** draws something */
    public void startDrawing(java.awt.Graphics g, int x, int y, int width, int height);
    /** gets the actual size of the image that will be drawn given the requested size */
    public java.awt.Rectangle getActualSize(int x, int y, int w, int h);
}

