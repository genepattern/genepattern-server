package org.genepattern.server.eula.remote;

import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaInfo.EulaInitException;
import org.genepattern.server.eula.remote.PostToBroad.MyException;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

public class TestPostToBroad {
    private PostToBroad post;
    private EulaInfo eulaInfo;
    final String userid="junit_user";
    final String email="junit_user@broadinstitute.org";
    final String lsidIn="urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3";
    final String nameIn="testLicenseAgreement";
    

    @Before
    public void setUp() throws EulaInitException {
        eulaInfo=new EulaInfo();
        eulaInfo.setModuleLsid(lsidIn);
        eulaInfo.setModuleName(nameIn);
        post=new PostToBroad();
        post.setGpUserId(userid);
        post.setEulaInfo(eulaInfo);
        post.setEmail(email);
    }

    @Test
    public void integrationTest() {
        try {
            post.postRemoteRecord();
        }
        catch (Throwable t) {
            Assert.fail(""+t.getLocalizedMessage());
        }
    }
    
    //error tests
    @Test
    public void testNullRemoteUrl() {
        post.setRemoteUrl(null);
        try {
            post.postRemoteRecord();
            Assert.fail("user==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Throwable t) {
            Assert.fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testRemoteUrl_NotSet() {
        post.setRemoteUrl("");
        try {
            post.postRemoteRecord();
            Assert.fail("user==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Throwable t) {
            Assert.fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testBogusRemoteUrl() {
        //a bogus url, not a valid URL
        post.setRemoteUrl("<RemoteServer>");
        try {
            post.postRemoteRecord();
            Assert.fail("user==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Throwable t) {
            Assert.fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }
    
    @Test
    public void testRemoteUrl_ConnectionRefused() {
        //should cause a connection error, because this is a valid URL to a host which is not running
        post.setRemoteUrl("http://127.0.0.1:10000");
        try {
            post.postRemoteRecord();
            Assert.fail("user==null, should throw IllegalArgumentException");
        }
        catch (MyException e) {
            //expected
        }
        catch (Throwable t) {
            Assert.fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testNullUserId() {
        post.setGpUserId(null);
        try {
            post.postRemoteRecord();
            Assert.fail("user==null, should throw IllegalArgumentException");
        }
        catch (Throwable t) {
            //expected
        }
    }

    @Test
    public void testNullEula() {
        post.setEulaInfo(null);
        try {
            post.postRemoteRecord();
            Assert.fail("eula==null, should throw IllegalArgumentException");
        }
        catch (Throwable t) {
            //expected
        }
    }
}
