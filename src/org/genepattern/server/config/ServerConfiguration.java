package org.genepattern.server.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.genepattern.server.repository.RepositoryInfo;

public interface ServerConfiguration {
    public static final String PROP_CONFIG_FILE = "config.file";
    //for compatibility with GP 3.2.3 and GP 3.2.4
    public static final String PROP_LEGACY_CONFIG_FILE = "command.manager.config.file";

    String getConfigFilepath();

    File getConfigFile();

    /**
     * Get the list of errors, if any, which resulted from parsing the configuration file.
     * 
     * @return
     */
    List<Throwable> getInitializationErrors();

    CommandManagerProperties getCommandManagerProperties();

    JobConfigObj getJobConfiguration();

    Set<String> getRepositoryUrls();

    RepositoryInfo getRepositoryInfo(final String url);

    /**
     * Utility method for parsing properties as a boolean.
     * The current implementation uses Boolean.parseBoolean, 
     * which returns true iff the property is set and equalsIgnoreCase 'true'.
     * 
     * @param key
     * @return
     */
    boolean getGPBooleanProperty(final GpContext context, final String key);

    boolean getGPBooleanProperty(final GpContext context, final String key, final boolean defaultValue);

    /**
     * Utility method for parsing a property as an Integer.
     * 
     * When a non integer value is set in the config file, the default value is returned.
     * Errors are logged, but exceptions are not thrown.
     * 
     * @param key
     * @param defaultValue
     * 
     * @return the int value for the property, or the default value, can return null.
     */
    Integer getGPIntegerProperty(final GpContext context, final String key, final Integer defaultValue);

    Long getGPLongProperty(final GpContext context, final String key, final Long defaultValue);

    /**
     * @deprecated, use getValue instead, which supports lists.
     * @param context
     * @param key
     * @return
     */
    String getGPProperty(final GpContext context, final String key);

    String getGPProperty(final GpContext context, final String key, final String defaultValue);

    Value getValue(final GpContext context, final String key);

    /**
     * Get the public facing URL for this GenePattern Server.
     * Note: replaces <pre>System.getProperty("GenePatternURL");</pre>
     * @return
     */
    URL getGenePatternURL();
    
    public String getGenePatternVersion();

    //helper methods for locating server files and folders
    /**
     * Get the 'home directory' for a gp user account. This is the location for user data.
     * Home directories are created in the  in the "../users" directory.
     * The default location can be changed with the 'user.root.dir' configuration property. 
     * The 'gp.user.dir' property can be set on a per user basis to change from the standard location.
     * 
     * Note: The 'gp.user.dir' property is an untested feature. If an admin sets a non-standard user dir,
     *     they need to take measures (undocumented and unsupported, @see gp-help) to deal with 
     *     pre-existing files and file entries in the DB.
     * 
     * @param context
     * @return
     */
    File getUserDir(final GpContext context);

    /**
     * Get the jobs directory for the given user. Each job runs in a new working directory. 
     * 
     * circa, GP 3.2.4 and earlier, the working directory is created in the root job dir for the server,
     *     which defaults to './webapps/gp/jobResults'.
     * Edit the 'jobs' property to customize this location. The server configuration system enables setting
     * this property on a per user, group, executor or module basis.
     * 
     * coming soon, job directories, by default, will be created in ../users/<user.id>/jobs/
     * To test this feature, remove the 'jobs' property from genepattern.properties and the configuration file.
     * 
     * @return the parent directory in which to create the new working directory for a job.
     */
    File getRootJobDir(final GpContext context) throws ServerConfigurationException;

    /**
     * Get the upload directory for the given user, the location for files uploaded directly from the Uploads tab.
     * By default, user uploads are stored in ../users/<user.id>/uploads/
     * 
     * The default location can be overridden with the 'user.upload.dir' property.
     * If there is no 'user.upload.dir' or 'gp.user.dir' set, then 'java.io.tempdir' is returned.
     * 
     * @param context
     * @return
     * @throws IllegalArgumentException if a directory is not found for the userId.
     */
    File getUserUploadDir(final GpContext context) throws IllegalArgumentException;

    File getTempDir();

    boolean getAllowInputFilePaths(final GpContext context);

    File getTemporaryUploadDir(final GpContext context) throws IOException, Exception;

}