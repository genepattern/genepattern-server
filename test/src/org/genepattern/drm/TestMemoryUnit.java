/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import static org.junit.Assert.assertEquals;

import org.genepattern.drm.Memory.Unit;
import org.junit.Test;

public class TestMemoryUnit {

    @Test
    public void preferredUnitBytes() {
        assertEquals("512 bytes", Unit.b, Unit.getPreferredUnit( 512L ));
    }
    
    @Test
    public void preferredUnitKb_exact() {
        assertEquals("1024 bytes", Unit.kb, Unit.getPreferredUnit( 1024L ));
    }
    
    @Test
    public void preferredUnitMb_exact() {
        assertEquals("1048576 bytes", Unit.mb, Unit.getPreferredUnit( 1048576L ));
    }

    @Test
    public void preferredUnitMb() {
        assertEquals("2048576 bytes", Unit.mb, Unit.getPreferredUnit( 2048576L ));
    }
    
    @Test
    public void preferredUnitGb() {
        assertEquals("3073741824 bytes", Unit.gb, Unit.getPreferredUnit(3073741824L));
    }
    
    @Test
    public void preferredUnitTb() {
        assertEquals("4099511627776 bytes", Unit.tb, Unit.getPreferredUnit(4099511627776L));
    }
    
    @Test
    public void preferredUnitPb_exact() {
        assertEquals("1125899906842624 bytes", Unit.pb, Unit.getPreferredUnit(1125899906842624L));
    }
    
    @Test
    public void preferredUnitPb() {
        assertEquals("58125899906842624 bytes", Unit.pb, Unit.getPreferredUnit(58125899906842624L));
    }

}
