package org.genepattern.gpge.ui.graphics.draggable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;


/**
 * A BufferedImage of one of four types of arrow (up, down, left or right)
 * drawn to the size specified on the constructor.
 */
public class ArrowImage extends BufferedImage {
    // Constants...
    public static final int ARROW_UP	= 0;
    public static final int ARROW_DOWN	= 1;
    public static final int ARROW_LEFT	= 2;
    public static final int ARROW_RIGHT	= 3;
    
    
    // Member variables...
    private GeneralPath	_pathArrow      = new GeneralPath();
    
    // Constructor...
    public ArrowImage(int nArrowDirection) {
        this(15, 9, nArrowDirection);
    }
    
    public ArrowImage(int nWidth, int nHeight, int nArrowDirection) {
        super(nWidth, nHeight,TYPE_INT_ARGB_PRE);	// Set the width, height and image type
        
        Map map = new HashMap();
        map.put(RenderingHints.KEY_ANTIALIASING,	RenderingHints.VALUE_ANTIALIAS_ON	);
        map.put(RenderingHints.KEY_RENDERING,		RenderingHints.VALUE_RENDER_QUALITY	);
        RenderingHints hints = new RenderingHints(map);
        
        Graphics2D g2 = this.createGraphics();		// Create a graphics context for this buffered image
        g2.setRenderingHints(hints);
        
        float h   = getHeight();
        float w	  = getWidth();
        float w13 = w/3;
        float w12 = w/2;
        float w23 = w*2/3;
        float h13 = h/3;
        float h12 = h/2;
        float h23 = h*2/3;
        
        switch (nArrowDirection) {
            case ARROW_UP:
                _pathArrow.moveTo(w12,	h12	);
                _pathArrow.lineTo(w12,	0	);
                _pathArrow.lineTo(w, 	h-1	);
                _pathArrow.lineTo(0, 	h-1	);
                _pathArrow.closePath();
                g2.setPaint(new GradientPaint(
                w13,	h13,	SystemColor.controlLtHighlight,
                w, 	h-1,	SystemColor.controlShadow
                )
                );
                
                g2.fill(_pathArrow);
                
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(new Line2D.Float(0,		h-1,	w,		h-1	));
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w12,	0,		w,		h-1	));
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(new Line2D.Float(0,		h-1,	w12,	0	));
                break;
                
            case ARROW_DOWN:
                _pathArrow.moveTo(w12,	h12	);
                _pathArrow.lineTo(w, 	0	);
                _pathArrow.lineTo(w12,	h-1	);
                _pathArrow.closePath();
                g2.setPaint(new GradientPaint(
                0,	0,		SystemColor.controlLtHighlight,
                w23,	h23,	SystemColor.controlShadow
                )
                );
                g2.fill(_pathArrow);
                
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(new Line2D.Float(w,		0,		w12,	h-1	));
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w12,	h-1,	0,		0	));
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(new Line2D.Float(0,		0,		w,		0	));
                break;
                
            case ARROW_LEFT:
                _pathArrow.moveTo(w-1,	h13	);
                _pathArrow.lineTo(w13, h13	);
                _pathArrow.lineTo(w13,	0	);
                _pathArrow.lineTo(0,	h12	);
                _pathArrow.lineTo(w13,	h-1	);
                _pathArrow.lineTo(w13,	h23	);
                _pathArrow.lineTo(w-1,	h23	);
                _pathArrow.closePath();
                g2.setPaint(new GradientPaint(
                0, 0,		Color.white, //SystemColor.controlLtHighlight,
                0, h,		SystemColor.controlShadow
                )
                );
                g2.fill(_pathArrow);
                
                _pathArrow.reset();
                _pathArrow.moveTo(w13,	0	);
                _pathArrow.lineTo(w13, h13	);
                _pathArrow.moveTo(w-1,	h13	);
                _pathArrow.lineTo(w-1,	h23	);
                _pathArrow.lineTo(w13,	h23	);
                _pathArrow.lineTo(w13,	h-1	);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(_pathArrow);
                
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(0,		h12,	w13,	h-1	));
                
                _pathArrow.reset();
                _pathArrow.moveTo(0,	h12	);
                _pathArrow.lineTo(w13,	0	);
                _pathArrow.moveTo(w13,	h13	);
                _pathArrow.lineTo(w-1,	h13	);
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(_pathArrow);
                break;
                
            case ARROW_RIGHT:
            default: {
                _pathArrow.moveTo(0,	h13	);
                _pathArrow.lineTo(w23, h13	);
                _pathArrow.lineTo(w23,	0	);
                _pathArrow.lineTo(w-1,	h12	);
                _pathArrow.lineTo(w23,	h-1	);
                _pathArrow.lineTo(w23,	h23	);
                _pathArrow.lineTo(0,	h23	);
                _pathArrow.closePath();
                g2.setPaint(new GradientPaint(
                0, 0,		Color.white, //SystemColor.controlLtHighlight,
                0, h,		SystemColor.controlShadow
                )
                );
                g2.fill(_pathArrow);
                
                _pathArrow.reset();
                _pathArrow.moveTo(0,	h23	);
                _pathArrow.lineTo(w23, h23	);
                _pathArrow.moveTo(w23,	h-1	);
                _pathArrow.lineTo(w-1,	h12	);
                g2.setColor(SystemColor.controlDkShadow);
                g2.draw(_pathArrow);
                
                g2.setColor(SystemColor.controlShadow);
                g2.draw(new Line2D.Float(w-1,		h12,	w23,	0	));
                
                _pathArrow.reset();
                _pathArrow.moveTo(w23,	0	);
                _pathArrow.lineTo(w23,	h13	);
                _pathArrow.lineTo(0,	h13	);
                _pathArrow.lineTo(0,	h23	);
                _pathArrow.moveTo(w23,	h23	);
                _pathArrow.lineTo(w23,	h-1	);
                g2.setColor(SystemColor.controlLtHighlight);
                g2.draw(_pathArrow);
                break;
            }
        }
        
    }
    
    
    
    
}





