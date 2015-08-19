/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import static org.junit.Assert.*;

import org.genepattern.server.eula.InitException;
import org.junit.Test;

/**
 * Tests for the EulaInfo class.
 * @author pcarr
 *
 */
public class TestEulaInfo {
    @Test
    public void testSetModuleLsid() {
        final String lsidIn="urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3";
        final String nameIn="testLicenseAgreement";
        EulaInfo eula=new EulaInfo();
        try {
            eula.setModuleLsid(lsidIn);
        }
        catch (InitException e) {
            fail(e.getLocalizedMessage());
        }
        eula.setModuleName(nameIn);
        assertEquals("eula.lsid.toString", lsidIn, eula.getLsid().toString());
        assertEquals("eula.moduleLsid", lsidIn, eula.getModuleLsid());
        assertEquals("eula.moduleLsidVersion", "3", eula.getModuleLsidVersion()); 
        assertEquals("eula.moduleName", nameIn, eula.getModuleName());
    }
    
    @Test
    public void testSetModuleLsid_Null() {
        EulaInfo eula=new EulaInfo();
        try {
            eula.setModuleLsid(null);
            fail("setModuleLsid(null) should throw exception");
        }
        catch (InitException e) {
            //expected
        }
    }
    
    @Test
    public void testSetModuleLsid_Empty() {
        EulaInfo eula=new EulaInfo();
        try {
            eula.setModuleLsid("");
            fail("setModuleLsid(\"\") should throw exception");
        }
        catch (InitException e) {
            //expected
        }
    }
    
    @Test
    public void testSetModuleLsid_Invalid() {
        EulaInfo eula=new EulaInfo();
        try {
            eula.setModuleLsid("this is not an lsid");
            fail("setModuleLsid(\"\") should throw exception");
        }
        catch (InitException e) {
            //expected
        }
    }
}
