package org.genepattern.server.webapp.rest.api.v1.data.upload;

import java.io.File;
import java.util.HashSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Files;


public class ResumableInfo {
    public int      resumableChunkSize;
    public long     resumableTotalSize;
    public String   resumableIdentifier;
    public String   resumableFilename;
    public String   resumableRelativePath;
    public String   destinationFilePath;
    public String   destinationTargetPath;

    
    public String toString(){
        StringBuffer buff = new StringBuffer("ResumableInfo { ");
        buff.append("   resumableFilename: ");
        buff.append(resumableFilename);
        buff.append("   resumableIdentifier: ");
        buff.append(resumableIdentifier);
        buff.append("   destinationFilePath: ");
        buff.append(destinationFilePath);
        buff.append("   destinationTargetPath: ");
        buff.append(destinationTargetPath);
        
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
        File dest = new File(this.destinationFilePath+File.separator + this.resumableFilename);
       
        try{
            // System.out.println("Uploaded file complete.  Copying from "+ file.getAbsolutePath() + " to " + dest.getAbsolutePath());
            
            Files.move(file, dest);
            
        } catch (Exception ioe){
            ioe.printStackTrace();
        }
        
        return true;
    }
}
