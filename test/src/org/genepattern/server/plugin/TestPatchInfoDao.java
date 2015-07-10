/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import static org.genepattern.server.plugin.TestMigratePlugins.assertComparePatchInfo;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.DbUtil.DbType;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.database.SchemaUpdater;
import org.genepattern.server.plugin.dao.PatchInfoDao;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestPatchInfoDao {
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
    }
    
    @Ignore // <---- just for manually testing MySQL db initialization
    @Test
    public void initMysqlDb() throws Throwable {
        DbUtil.initDb(DbType.MYSQL);
        File gpResourcesDir=new File("resources");
        String dbSchemaPrefix="analysis_mysql-";
        String gpVersion="3.9.2";
        SchemaUpdater.updateSchema(HibernateUtil.instance(), gpResourcesDir, dbSchemaPrefix, gpVersion);
        
        List<PatchInfo> expected = TestPluginRegistrySystemProps.initDefaultInstalledPatchInfos();
        assertEquals("before record patch, expecting default entries",
                expected,
                new PatchInfoDao().getInstalledPatches());
    }
    
    @Test
    public void recordPatch() throws DbException, MalformedURLException {
        // test one, default entries 
        List<PatchInfo> defaultEntries = TestPluginRegistrySystemProps.initDefaultInstalledPatchInfos();
        assertComparePatchInfo("before recordPatch, expecting default list", defaultEntries, new PatchInfoDao().getInstalledPatches());

        // test two, record patch
        final String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
        new PatchInfoDao().recordPatch(new PatchInfo(BWA));
        List<PatchInfo> expected = new ArrayList<PatchInfo>(defaultEntries);
        expected.add(new PatchInfo(BWA));
        assertComparePatchInfo(
            "expecting a new entry after recording patch", 
            // expected
            expected, 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test three, duplicate lsid, update the entry
        PatchInfo bwa=new PatchInfo(BWA);
        String patchDir=new File("../patches/"+BWA).getAbsolutePath();
        bwa.setPatchDir(patchDir);
        new PatchInfoDao().recordPatch(bwa);
        assertEquals("BWA.patchDir", patchDir, new PatchInfoDao().selectPatchInfoByLsid(BWA).getPatchDir());
              
        assertComparePatchInfo(
            "expecting no new entry after recording a patch update", 
            // expected
            expected, 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test four, delete record
        boolean success=new PatchInfoDao().removePatch(new PatchInfo(BWA));
        assertEquals("Expecting successful remove patch", true, success);
        assertComparePatchInfo(
            "after removePatch, expecting the default list",
            // expected
            defaultEntries, 
            // actual
            new PatchInfoDao().getInstalledPatches());
    }

}
