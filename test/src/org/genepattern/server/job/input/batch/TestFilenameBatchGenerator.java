package org.genepattern.server.job.input.batch;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInput;
import org.junit.Before;
import org.junit.Test;

public class TestFilenameBatchGenerator {
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private GpContext jobContext;
    private FilenameBatchGenerator gen;
    
    // batch input from web client request
    private JobInput jobInput;

    @Before
    public void setUp() {
        mgr=mock(HibernateSessionManager.class);
        gpConfig=mock(GpConfig.class);
        jobContext=mock(GpContext.class);
        gen = new FilenameBatchGenerator(mgr, gpConfig, jobContext);
        assertEquals("new FilenameBatchGenerator().batchValues.size", 0, gen.getBatchValues().size());
        
        jobInput=new JobInput();
        jobInput.setBaseGpHref(proxyHref);
    }
    
    @Test
    public void testBatch_oneBatchParam() {
        // CLE batch input over one param
        final String pname="input.filename";
        jobInput.addBatchValue(pname, proxyHref+jobUploadPath(pname, "all_aml_test.gct"));
        jobInput.addBatchValue(pname, dataFtpDir+"all_aml_test.gct");
        
        gen.extractBatchValues(jobInput);
        
        //LinkedListMultimap<String,GpFilePath> map=gen.getBatchValues();
        //map.asMap().size();
        assertEquals("batchValues.size, after extract", 1, gen.getBatchValues().asMap().size());
        List<GpFilePath> values=gen.getBatchValues().get(pname);
        assertEquals("batchValues."+pname+".size", 2, values.size());
        assertEquals("values[0].isLocal (proxyHref)", true, values.get(0).isLocal());
        assertEquals("values[1].isLocal (ftp)", false, values.get(1).isLocal());
    }

    @Test
    public void testBatch_twoBatchParams() {
        // batch over multiple parameters
        final String pname_gct="input.gct";
        final String pname_cls="input.cls";
        // non batch param
        final String pname_q="q";
        
        jobInput.addValue(pname_q, "0.1");
        
        jobInput.addBatchValue(pname_gct, proxyHref+uploadPath("all_aml_test.gct"));
        jobInput.addBatchValue(pname_gct, dataFtpDir+"all_aml_train.gct");
        
        jobInput.addBatchValue(pname_cls, proxyHref+uploadPath("all_aml_test.cls"));
        jobInput.addBatchValue(pname_cls, dataFtpDir+"all_aml_train.cls");

        gen.extractBatchValues(jobInput);
        assertEquals("batchValues.size, after extract", 2, gen.getBatchValues().asMap().size());

        List<GpFilePath> values_gct=gen.getBatchValues().get(pname_gct);
        List<GpFilePath> values_cls=gen.getBatchValues().get(pname_cls);
        assertEquals("batchValues."+pname_gct+".size", 2, values_gct.size());
        assertEquals("batchValues."+pname_cls+".size", 2, values_cls.size());
        assertEquals(pname_gct+"[0].isLocal (proxyHref)", true, values_gct.get(0).isLocal());
        assertEquals(pname_gct+"[1].isLocal (ftp)", false, values_gct.get(1).isLocal());
        assertEquals(pname_cls+"[0].isLocal (proxyHref)", true, values_cls.get(0).isLocal());
        assertEquals(pname_cls+"[1].isLocal (ftp)", false, values_cls.get(1).isLocal());
    }

}
