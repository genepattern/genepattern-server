package org.genepattern.server.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.genepattern.util.LSID;

public class PatchInfo {
    private LSID patchLsid;
    private URL patchUrl;
    
    public PatchInfo() {
    }
    
    public PatchInfo(final String patchLsid, final String patchUrl) throws MalformedURLException {
        this.patchLsid=patchLsid==null ? null : new LSID(patchLsid);
        this.patchUrl=patchUrl==null ? null : new URL(patchUrl);
    }
    
    public PatchInfo(final LSID patchLsid, final URL patchUrl) {
        this.patchLsid=patchLsid;
        this.patchUrl=patchUrl;
    }
    
    public LSID getPatchLsid() {
        return patchLsid;
    }
    
    public URL getPatchUrl() {
        return patchUrl;
    }
    
    public boolean equals(Object obj) {
        if (obj == null) { 
            return false;
        }
        if (!(obj instanceof PatchInfo)) {
            return false;
        }
        PatchInfo arg=(PatchInfo)obj;
        return Objects.equals(patchLsid, arg.patchLsid) &&
                Objects.equals(patchUrl, arg.patchUrl);
    }
    
    public int hashCode() {
        return Objects.hash(patchLsid, patchUrl);
    }
}
