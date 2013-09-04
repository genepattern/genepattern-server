/**
 *
 *  Copyright (C) 2012  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: TestProgressMonitor.java,v $
 *        Revision 1.1  2012/11/06 04:12:13  bruceb
 *        stream changes - extract TestProgressMonitor out
 *
 *
 */
package com.enterprisedt.net.ftp.test;

import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.util.debug.Logger;

/**
 *  Test of progress monitor functionality   
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.1 $
 */
public class TestProgressMonitor implements FTPProgressMonitor {

    /**
     *  Log stream
     */
    protected static Logger log = Logger.getLogger("TestProgressMonitor");
    
    /* (non-Javadoc)
     * @see com.enterprisedt.net.ftp.FTPProgressMonitor#bytesTransferred(long)
     */
    public void bytesTransferred(long count) {
        log.info(count + " bytes transferred");         
    }            

}
