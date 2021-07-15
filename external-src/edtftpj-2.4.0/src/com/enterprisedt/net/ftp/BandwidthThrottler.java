/**
 *
 *  Copyright (C) 2000-2009  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: BandwidthThrottler.java,v $
 *        Revision 1.2  2011-03-28 03:41:41  hans
 *        Made logger static.
 *
 *        Revision 1.1  2009-03-20 03:54:22  bruceb
 *        bandwidth throttling
 *
 *
 */
package com.enterprisedt.net.ftp;

import com.enterprisedt.util.debug.Logger;

/**
 *  Helps throttle bandwidth for transfers
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class BandwidthThrottler {

    private static Logger log = Logger.getLogger("BandwidthThrottler");
    private long lastTime = 0;
    private long lastBytes = 0;
    private int thresholdBytesPerSec = -1;
    
    public BandwidthThrottler(int thresholdBytesPerSec) {
        this.thresholdBytesPerSec = thresholdBytesPerSec;
    }
    
    public void setThreshold(int thresholdBytesPerSec) {
        this.thresholdBytesPerSec = thresholdBytesPerSec;
    }
    
    public int getThreshold() {
        return thresholdBytesPerSec;
    }
    
    public void throttleTransfer(long bytesSoFar) {
        long time = System.currentTimeMillis();
        long diffBytes = bytesSoFar - lastBytes;
        long diffTime = time - lastTime;
        if (diffTime == 0)
            return;
        
        double rate = ((double)diffBytes/(double)diffTime)*1000.0;
        if (log.isDebugEnabled())
            log.debug("rate= " + rate);
        
        while (rate > thresholdBytesPerSec) {
            try {
                if (log.isDebugEnabled())
                    log.debug("Sleeping to decrease transfer rate (rate = " + rate + " bytes/s");
                Thread.sleep(100);
            }
            catch (InterruptedException ex) {}
            diffTime = System.currentTimeMillis() - lastTime;
            rate = ((double)diffBytes/(double)diffTime)*1000.0;
        }
        lastTime = time;
        lastBytes = bytesSoFar;
    }
    
    
    public void reset() {
        lastTime = System.currentTimeMillis();
        lastBytes = 0;
    }
}
