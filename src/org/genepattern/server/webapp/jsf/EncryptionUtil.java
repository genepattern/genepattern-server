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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class EncryptionUtil {
    private static Logger log = Logger.getLogger(EncryptionUtil.class);

    private EncryptionUtil() {
    }

    /**
     * Encrypts the clear text. The returned byte array is exactly 255 bytes,
     * the size of the password field.
     * 
     * @param clearText
     *            the unencrypted text
     * @return The encrypted text as a byte array
     * @throws NoSuchAlgorithmException
     */
    public static byte[] encrypt(String clearText)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.reset();
        try {
            md.update(clearText.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        byte[] encryptedString = new byte[255];
        Arrays.fill(encryptedString, (byte) 0);
        byte[] digestedString = md.digest();
        for (int i = 0, length = Math.min(encryptedString.length,
                digestedString.length); i < length; i++) {
            encryptedString[i] = digestedString[i];
        }
        return encryptedString;
    }

    public static void main(String[] args) throws Exception {
        String txt = "a";
        byte[] a1 = EncryptionUtil.encrypt(txt);
        byte[] a2 = EncryptionUtil.encrypt(txt);
        System.out.println(a1.length);
        System.out.println(java.util.Arrays.equals(a1, a2));
    }

}
