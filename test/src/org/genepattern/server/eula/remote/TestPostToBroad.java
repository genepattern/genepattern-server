/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula.remote;

import static org.junit.Assert.*;

import java.io.IOException;

import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.InitException;
import org.genepattern.server.eula.RecordEulaDefault;
import org.genepattern.server.eula.remote.PostToBroad.PostException;
import org.junit.Before;
import org.junit.Test;

public class TestPostToBroad {
    private PostToBroad post;
    private EulaInfo eulaInfo;
    final String userid="junit_user";
    final String email="junit_user@broadinstitute.org";
    final String lsidIn="urn:lsid:9090.gpdev.gpint01:genepatternmodules:812:3";
    final String nameIn="testLicenseAgreement";
    final String gpUrl="http://127.0.0.1:8080/gp/";

    final String eulaEndpoint=RecordEulaDefault.REMOTE_URL_DEFAULT;

    @Before
    public void setUp() throws InitException {
        eulaInfo=new EulaInfo();
        eulaInfo.setModuleLsid(lsidIn);
        eulaInfo.setModuleName(nameIn);
        post=new PostToBroad();
        post.setRemoteUrl(eulaEndpoint);
        post.setGpUrl(gpUrl);
        post.setGpUserId(userid);
        post.setEulaInfo(eulaInfo);
        post.setEmail(email);
    }

    @Test
    public void integrationTest() {
        try {
            post.doPost();
        }
        catch (Throwable t) {
            fail("Error posting to "+eulaEndpoint+": "+t.getLocalizedMessage());
        }
    }
    
    //error tests
    @Test
    public void testNullRemoteUrl() {
        post.setRemoteUrl(null);
        try {
            post.doPost();
            fail("user==null, should throw InitException");
        }
        catch (InitException e) {
            //expected
        }
        catch (Throwable t) {
            fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testRemoteUrl_NotSet() {
        post.setRemoteUrl("");
        try {
            post.doPost();
            fail("user==null, should throw InitException");
        }
        catch (InitException e) {
            //expected
        }
        catch (Throwable t) {
            fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testBogusRemoteUrl() {
        //a bogus url, not a valid URL
        post.setRemoteUrl("<RemoteServer>");
        try {
            post.doPost();
            fail("user==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
        catch (Throwable t) {
            fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }
    
    @Test
    public void testRemoteUrl_ConnectionRefused() {
        //should cause a connection error, because this is a valid URL to a host which is not running
        post.setRemoteUrl("http://127.0.0.1:10000");
        try {
            post.doPost();
            fail("user==null, should throw IOException");
        }
        catch (IOException e) {
            //expected
        }
        catch (Throwable t) {
            fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }
    
    @Test
    public void testRemoteException() {
        //should cause a connection error, because the Google server doesn't know 
        //how to handle this POST
        post.setRemoteUrl("http://www.google.com");
        try {
            post.doPost();
            fail("user==null, should throw IOException");
        }
        catch (PostException e) {
            //expected
            assertTrue("Expecting a 4xx status code", 400<=e.getStatusCode() && e.getStatusCode()<500);
            assertEquals("", "Method Not Allowed", e.getReason());
        }
        catch (Throwable t) {
            fail("unexpected exception thrown: "+t.getLocalizedMessage());
        }
    }

    @Test
    public void testNullUserId() {
        try {
            post.setGpUserId(null);
            fail("userId==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testUserId_NotSet() {
        try {
            post.setGpUserId("");
            fail("userId==\"\", should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testNullEula() {
        try {
            post.setEulaInfo(null);
            fail("eula==null, should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //expected
        }
    }
    
}
