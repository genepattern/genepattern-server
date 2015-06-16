/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.server.auth.AuthenticationException;

import junit.framework.TestCase;

/**
 * Unit tests for validating usernames upon account creation.
 * 
 * @author pcarr
 */
public class UserAccountManagerTest extends TestCase {
    public void testValidateUsernameExceptions() {
        String bogus = "\"test user\"";        
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "test user\"";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "'test user'";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "test user'";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "Tab\tcharacter";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
    }
    
    public void testValidateUsername() {
        try {
            UserAccountManager.validateUsername("test@emailaddress.com");
            UserAccountManager.validateUsername("space character");
            UserAccountManager.validateUsername("");
            //TODO: 
            UserAccountManager.validateUsername("Newline character \n");
        }
        catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

}
