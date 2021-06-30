/**
 *
 *  Copyright (C) 2000-2007  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: TransferCompleteStrings.java,v $
 *        Revision 1.1  2007/01/12 02:04:23  bruceb
 *        string matchers
 *
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  Contains fragments of server replies that indicate no files were
 *  found in a supplied directory.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.1 $
 */
final public class TransferCompleteStrings extends ServerStrings {

    /**
     * Server string transfer complete (proFTPD/TLS)
     */
    final private static String TRANSFER_COMPLETE = "TRANSFER COMPLETE";
    
    /**
     * Constructor. Adds the fragments to match on
     */
    public TransferCompleteStrings() {
        add(TRANSFER_COMPLETE);
    }

}
