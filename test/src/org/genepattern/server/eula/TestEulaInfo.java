/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import junit.framework.Assert;

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
            Assert.fail(e.getLocalizedMessage());
        }
        eula.setModuleName(nameIn);
        Assert.assertEquals("eula.lsid.toString", lsidIn, eula.getLsid().toString());
        Assert.assertEquals("eula.moduleLsid", lsidIn, eula.getModuleLsid());
        Assert.assertEquals("eula.moduleLsidVersion", "3", eula.getModuleLsidVersion()); 
        Assert.assertEquals("eula.moduleName", nameIn, eula.getModuleName());
    }
    
    @Test
    public void testSetModuleLsid_Null() {
        EulaInfo eula=new EulaInfo();
        try {
            eula.setModuleLsid(null);
            Assert.fail("setModuleLsid(null) should throw exception");
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
            Assert.fail("setModuleLsid(\"\") should throw exception");
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
            Assert.fail("setModuleLsid(\"\") should throw exception");
        }
        catch (InitException e) {
            //expected
        }
    }
}
