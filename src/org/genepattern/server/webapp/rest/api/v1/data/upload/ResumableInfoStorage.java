package org.genepattern.server.webapp.rest.api.v1.data.upload;
import java.io.*;
import java.util.HashMap;

public class ResumableInfoStorage {
    //Single instance
    private ResumableInfoStorage() {
    }
    private static ResumableInfoStorage sInstance;

    public static synchronized ResumableInfoStorage getInstance() {
        if (sInstance == null) {
            sInstance = new ResumableInfoStorage();
        }
        return sInstance;
    }

    //resumableIdentifier --  ResumableInfo
    private HashMap<String, ResumableInfo> mMap = new HashMap<String, ResumableInfo>();

    /**
     * Get ResumableInfo from mMap or Create a new one.
     * @param resumableChunkSize
     * @param resumableTotalSize
     * @param resumableIdentifier
     * @param resumableFilename
     * @param resumableRelativePath
     * @param resumableFilePath
     * @return
     */
    public synchronized ResumableInfo get(int resumableChunkSize, long resumableTotalSize,
                             String resumableIdentifier, String resumableFilename,
                             String resumableRelativePath, String resumableFilePath, String destinationFilePath, String destinationPath) {

        ResumableInfo info = mMap.get(resumableIdentifier);

        if (info == null) {
            info = new ResumableInfo();

            info.resumableChunkSize     = resumableChunkSize;
            info.resumableTotalSize     = resumableTotalSize;
            info.resumableIdentifier    = resumableIdentifier;
            info.resumableFilename      = resumableFilename;
            info.resumableRelativePath  = resumableRelativePath;
            info.resumableFilePath      = resumableFilePath;
            info.destinationFilePath    = destinationFilePath;
            info.destinationPath        = destinationPath;
            
            
            System.out.println("CREATING RESUMABLE "+ info.toString());
            
            mMap.put(resumableIdentifier, info);
        }
        return info;
    }

    /**
     * É¾³ýResumableInfo
     * @param info
     */
    public void remove(ResumableInfo info) {
       mMap.remove(info.resumableIdentifier);
    }
}
