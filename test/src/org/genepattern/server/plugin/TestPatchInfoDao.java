package org.genepattern.server.plugin;

import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.DbUtil.DbType;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HsqlDbUtil;
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
        HsqlDbUtil.updateSchema(gpResourcesDir, dbSchemaPrefix, gpVersion);

    }
        
    @Test
    public void recordPatch() throws DbException, MalformedURLException {
        // test one, empty list 
        final List<PatchInfo> empty=Collections.emptyList();
        assertEquals("before recordPatch, expecting empty list", empty, new PatchInfoDao().getInstalledPatches());

        // test two, record patch
        final String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
        new PatchInfoDao().recordPatch(new PatchInfo(BWA));
        
        assertEquals(
            // expected
            Arrays.asList(new PatchInfo(BWA)), 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test three, duplicate lsid, update the entry
        new PatchInfoDao().recordPatch(new PatchInfo(BWA));
        assertEquals(
            // expected
            Arrays.asList(new PatchInfo(BWA)), 
            // actual
            new PatchInfoDao().getInstalledPatches());
        
        // test four, delete record
        boolean success=new PatchInfoDao().removePatch(new PatchInfo(BWA));
        assertEquals("Expecting successful remove patch", true, success);
        assertEquals(
            "after removePatch, expecting empty list",
            // expected
            empty, 
            // actual
            new PatchInfoDao().getInstalledPatches());
    }

}
