package org.genepattern.server.genomespace;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.genepattern.server.dm.GpFilePath;

/**
 * Interface for interacting with the GenomeSpace CDK.  This implementation of this interface is in the separated 
 * GenomeSpace JAR, to prevent GenomeSpace classes from loading unless GenomeSpace is enabled.
 * @author tabor
 *
 */
public interface GenomeSpaceClient {
    public InputStream getInputStream(String gpUserId, URL url) throws GenomeSpaceException;
    public GenomeSpaceLogin submitLogin(String env, String username, String password) throws GenomeSpaceException;
    public boolean isLoggedIn(Object gsSession);
    public void logout(Object gsSession);
    public void registerUser(String env, String username, String password, String regEmail) throws GenomeSpaceException;
    public GenomeSpaceFile buildFileTree(Object gsSession);
    public Date getModifiedFromMetadata(Object metadata);
    public Long getSizeFromMetadata(Object metadata);
    public void deleteFile(Object gsSessionObject, GenomeSpaceFile file) throws GenomeSpaceException;
    public Set<String> getAvailableFormats(Object metadata);
    public void saveFileToGenomeSpace(Object gsSessionObj, GpFilePath savedFile, GenomeSpaceFile directory) throws GenomeSpaceException;
    public Map<String, Set<String>> getKindToTools(Object gsSession);
    public URL getSendToToolUrl(Object gsSession, GenomeSpaceFile file, String toolName) throws GenomeSpaceException;
}
