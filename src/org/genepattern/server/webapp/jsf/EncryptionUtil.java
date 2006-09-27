/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptionUtil {
    private EncryptionUtil() {
    }

    /**
     * Encrypts the clear text
     * 
     * @param clearText
     * @return The encrypted text
     * @throws NoSuchAlgorithmException
     */
    public static String encrypt(String clearText) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        md.update(clearText.getBytes());
        byte[] b = md.digest();
        // Base64 base64 = new Base64();
        // b = base64.encode(b);
        return new String(b);
    }

}
