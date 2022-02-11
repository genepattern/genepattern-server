/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

/**
 * Parse a config file and create a new instance of a CommandManager.
 * @author pcarr
 */
public interface CommandManagerParser {
    /**
     * Create a new instance of a CommandManager by [optionally] reading settings from the given configuration file.
     * @param pathToConfigFile
     * @return
     */
    CommandManager parseConfigFile(String pathToConfigFile) throws Exception;

    /**
     * Reload job specific settings (e.g. the mapping from JobInfo to CommandExecutor, and any job specific settings) from the given configuration file.
     * @param commandManager
     * @param pathToConfigFile
     */
    void reloadConfigFile(CommandManager commandManager, String pathToConfigFile) throws Exception;
}
