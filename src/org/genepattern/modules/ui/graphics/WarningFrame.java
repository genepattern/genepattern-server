/*
 * This software and its documentation are copyright 1999 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 * 
 * This software is made available for use on a case by case basis, and
 * only with specific written permission from The Whitehead Institute.
 * It may not be redistributed nor posted to any bulletin board, included
 * in any shareware distributions, or the like, without specific written
 * permission from The Whitehead Institute.  This code may be customized
 * by individual users, although such versions may not be redistributed
 * without specific written permission from The Whitehead Institute.
 * 
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 *
 */

package org.genepattern.modules.ui.graphics;



public class WarningFrame extends MessageFrame{
    /** constructs a new UILogFrame with the appropriet title and message */
    public WarningFrame () {
        super("Warning Messages");
        appendMsg("Unless you are a developer or are trying to debug a problem\n"
        +"you can, in general, ignore messages displayed here.\n\n");
    }
    
}

