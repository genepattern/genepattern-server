/*
 * GifImageProducer.java
 *
 * Created on December 14, 2001, 11:39 AM
 */

package org.genepattern.modules.ui.graphics;

import java.awt.*;
import java.awt.image.*;

/**
 * This class writes the image to the ImageConsumer in parts.  This is done with
 * the current amount of free memory taken into consideration 
 * (graphics ram should be considered here).
 *
 * @author  KOhm
 * @version 
 */
public final class SmartImageProducer implements java.awt.image.ImageProducer {

    /** Creates new GifImageProducer */
    public SmartImageProducer (Component comp, int width, int height, ImageDrawer drawer) {
        this.width = width;
        this.height = height;
        this.component = comp;
        this.drawer = drawer;
        Runtime runtime = Runtime.getRuntime ();
        runtime.gc(); // clear any unused memory
        long free_mem = runtime.freeMemory ();
        long total_mem = runtime.totalMemory ();
        //assume 8 bit color and at least a 2 Mbyte graphics card
        this.num_parts = (int)((long)(width * height) / 2000000/*free_mem*/) + 1;
        
        //this.height /= num_parts; this.height *= 2; this.num_parts = 2;// testing
        
        System.out.println("SmartImageProducer: Free memory = "+free_mem
        +" total memory="+total_mem+" number of parts="+num_parts+" total width="+width+" height="+height);
    }
    /**
     * This method is used to register an ImageConsumer with the
     * ImageProducer for access to the image data during a later
     * reconstruction of the Image.
     */
    public void addConsumer (java.awt.image.ImageConsumer imageConsumer) {
        if(consumer == null) {
            consumer = imageConsumer;
        } else if(!isConsumer (imageConsumer)) {
            System.err.println ("Error: trying to add an image consumer, but alread have a consumer");
        }
    }
    /**
     * This method determines if a given ImageConsumer object is
     * currently registered with this ImageProducer as one of its consumers
     */    
    public boolean isConsumer (java.awt.image.ImageConsumer imageConsumer) {
        return (consumer == imageConsumer);
    }
    /**
     * This method removes the given ImageConsumer object from the
     * list of consumers currently registered to receive image data.
     */
    public void removeConsumer (java.awt.image.ImageConsumer imageConsumer) {
        consumer = null;
    }
    /**
     * This method is used by an ImageConsumer to request that the
     * ImageProducer attempt to resend the image data one more time in
     * TOPDOWNLEFTRIGHT order so that higher quality conversion algorithms
     * which depend on receiving pixels in order can be used to produce
     * a better output version of the image.
     */
    public void requestTopDownLeftRightResend (java.awt.image.ImageConsumer imageConsumer) {
        startProduction(imageConsumer);
    }
    /**
     * This method both registers the given ImageConsumer object as a
     * consumer and starts an immediate reconstruction of the image data
     * which will then be delivered to this consumer and any other consumer
     * which may have already been registered with the producer.
     */
    public void startProduction (final java.awt.image.ImageConsumer imageConsumer) {
        int increment = height / num_parts; // initial est.
        addConsumer(imageConsumer);
        imageConsumer.setDimensions (width, height);
        imageConsumer.setHints (ImageConsumer.TOPDOWNLEFTRIGHT);
        MyImageConsumer surrogate = new MyImageConsumer(imageConsumer);
        
        BufferedImage image = (BufferedImage)component.createImage (width, increment);
        System.out.println ("Image="+image);
        Graphics2D g = image.createGraphics ();
        Rectangle rec;
        int whats_left;
        //int h = 0;
        //for(int i = 0, limit = num_parts; i < limit; i++) {
        for(int i = 0, h = 0, limit = height; h < limit;) {
            whats_left = limit - h;
            rec = drawer.getActualSize (0, h, width, increment);
            increment = rec.height;
            if(whats_left < increment){ // no blank stuff at the bottom of image
                System.out.println ("limit="+limit+" h="+h+" increment="+increment+" whats_left="+whats_left);
                increment = whats_left + 1;
            }
            if(image.getHeight() != increment) {
                g.dispose ();
                image = (BufferedImage)component.createImage (width, increment);
                g = image.createGraphics ();
            }
            System.out.println ("loop "+(++i)+" h="+h+" increment="+increment);
            drawer.startDrawing (g, 0, h, width, increment);
            h = rec.y +  increment;
            g.setClip (0, 0, width, increment);
            ImageProducer producer = image.getSource ();
            producer.startProduction (surrogate);
        }
        g.dispose ();
        imageConsumer.imageComplete (ImageConsumer.STATICIMAGEDONE);
    }
    
    // fields
    /** this implementation only support one consumer */
    private ImageConsumer consumer;
    /** the Component where the image come from */
    private Component component;
    /** the width of the image */
    private int width;
    /** the height of the image */
    private int height;
    /** the number of parts the image should be delivered as */
    private int num_parts;
    /**
     * this will get it's chance to draw something before the image is sent 
     * to the ImageConsumer 
     */
    private ImageDrawer drawer;
    
    // inner classes
    /**
     * the image consumer
     */
    private final class MyImageConsumer implements ImageConsumer {
        /** constructs a new MyImageConsumer */
        private MyImageConsumer(ImageConsumer consumer) {
            this.consumer = consumer;
        }
        
        // ImageConsumer interface methods
        /**
         * @param colorModel
         */
        public void setColorModel (java.awt.image.ColorModel colorModel) {
            System.out.println ("colormodel-"+colorModel);
        }
        
        /**
         * @param param
         */
        public void setHints (int param) {
            System.out.print ("Hint: ");
            if((param & ImageConsumer.COMPLETESCANLINES) != 0)
                System.out.println ("COMPLETESCANLINES");
            if((param & ImageConsumer.RANDOMPIXELORDER) != 0)
                System.out.println ("RANDOMPIXELORDER");
            if((param & ImageConsumer.SINGLEPASS) != 0)
                System.out.println ("SINGLEPASS");
            if((param & ImageConsumer.TOPDOWNLEFTRIGHT) != 0)
                System.out.println ("TOPDOWNLEFTRIGHT");
            System.out.println ("hints done "+ param);
        }
        
        /**
         * @param hashtable
         */
        public void setProperties (java.util.Hashtable hashtable) {
            System.out.println ("properties-"+hashtable);
        }
        
        /**
         * @param x
         * @param y
         * @param w
         * @param h
         * @param model
         * @param pixels
         * @param off
         * @param scansize 
         */ 
        public void setPixels (int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
            //System.out.println("setPixels (ints): x="+x+" y="+y+" w="+w+" h="+h+" model="+model+" num pixels="+pixels.length+" off="+off+" scansize="+scansize);
            consumer.setPixels (x, y, w, h, model, pixels, off, scansize);
            if( y > max_y )
                max_y = y;
        }

        /**
         * @param x
         * @param y
         * @param w
         * @param h
         * @param model
         * @param pixels
         * @param off
         * @param scansize 
         */        
        public void setPixels (int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
            //System.out.println("setPixels (bytes): x="+x+" y="+y+" w="+w+" h="+h+" model="+model+" num pixels="+pixels.length+" off="+off+" scansize="+scansize);
            consumer.setPixels (x, y, w, h, model, pixels, off, scansize);
            if( y > max_y ) 
                max_y = y;
        }
        
        /**
         * @param width
         * @param height
         */
        public void setDimensions (int width, int height) {
            System.out.println ("dimension: width="+width+" height="+height);
        }
        
        /**
         * @param param
         */
        public void imageComplete (int param) {
            System.out.println ("Image conplete:");
            if((param & ImageConsumer.IMAGEABORTED) != 0)
                System.out.println ("IMAGEABORTED");
            if((param & ImageConsumer.IMAGEERROR) != 0)
                System.out.println ("IMAGEERROR");
            if((param & ImageConsumer.SINGLEFRAMEDONE) != 0)
                System.out.println ("SINGLEFRAMEDONE");
            if((param & ImageConsumer.STATICIMAGEDONE) != 0)
                System.out.println ("STATICIMAGEDONE");
            //        if((param & ImageConsumer.) != 0)
            //            System.out.println("");
            //        if((param & ImageConsumer.) != 0)
            //            System.out.println("");
            System.out.println ("image conplete done");
            System.out.println("Max size of y = "+max_y);
            max_y = 0;
        }
        /** the real image consumer */
        private ImageConsumer consumer;
        /** the max value of y */
        private int max_y;
     }
}
