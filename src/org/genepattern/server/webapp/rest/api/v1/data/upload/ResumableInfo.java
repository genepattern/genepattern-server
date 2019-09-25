package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.File;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ResumableInfo {
    public int      resumableChunkSize;
    public long     resumableTotalSize;
    public String   resumableIdentifier;
    public String   resumableFilename;
    public String   resumableRelativePath;
    public String   destinationFilePath;
    public String   destinationPath;

    
    public String toString(){
        StringBuffer buff = new StringBuffer("ResumableInfo { ");
        buff.append("   resumableFilename: ");
        buff.append(resumableFilename);
        buff.append("   resumableIdentifier: ");
        buff.append(resumableIdentifier);
        buff.append("   destinationFilePath: ");
        buff.append(destinationFilePath);
        buff.append("   destinationPath: ");
        buff.append(destinationPath);
        
        return buff.toString();
    }
    
    
    public static class ResumableChunkNumber {
        public ResumableChunkNumber(int number) {
            this.number = number;
        }

        public int number;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ResumableChunkNumber
                    ? ((ResumableChunkNumber)obj).number == this.number : false;
        }

        @Override
        public int hashCode() {
            return number;
        }
    }

    //Chunks uploaded
    public HashSet<ResumableChunkNumber> uploadedChunks = new HashSet<ResumableChunkNumber>();

    public String resumableFilePath;

    public boolean vaild(){
        if (resumableChunkSize < 0 || resumableTotalSize < 0
                || ResumableHttpUtils.isEmpty(resumableIdentifier)
                || ResumableHttpUtils.isEmpty(resumableFilename)
                || ResumableHttpUtils.isEmpty(resumableRelativePath)) {
            return false;
        } else {
            return true;
        }
    }
    public boolean checkIfUploadFinished() {
        //check if upload finished
        int count = (int) Math.ceil(((double) resumableTotalSize) / ((double) resumableChunkSize));
        for(int i = 1; i < count; i ++) {
            if (!uploadedChunks.contains(new ResumableChunkNumber(i))) {
                return false;
            }
        }

        //Upload finished, change filename.
        File file = new File(this.resumableFilePath);

        System.out.println("UPLOAD FINISHED - " + file.getAbsolutePath());
        
        file.renameTo(new File(this.destinationFilePath));
        System.out.println("UPLOAD RENAMED - DFP " + this.destinationFilePath + "   DP: "+ this.destinationPath);
        
        return true;
    }
}
