package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.job.input.JobInput;
import org.junit.Test;

public class TestGpContext {
    @Test
    public void initBaseGpHref() {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(Demo.cleLsid);
        jobInput.setBaseGpHref(Demo.gpHref);
        GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        assertEquals(Demo.gpHref, gpContext.getBaseGpHref());
    }

    @Test
    public void initBaseGpHref_proxy() {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(Demo.cleLsid);
        jobInput.setBaseGpHref(Demo.proxyHref);
        GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        assertEquals(Demo.proxyHref, gpContext.getBaseGpHref());
    }

    @Test
    public void initBaseGpHref_baseGpHrefNotSet() {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(Demo.cleLsid);
        GpContext gpContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        assertEquals(null, gpContext.getBaseGpHref());
    }

    @Test
    public void initBaseGpHref_jobInputNotSet() {
        GpContext gpContext=new GpContext.Builder().build();
        assertEquals(null, gpContext.getBaseGpHref());
    }
}
